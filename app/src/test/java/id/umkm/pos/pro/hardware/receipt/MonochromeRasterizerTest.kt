package id.umkm.pos.pro.hardware.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class MonochromeRasterizerTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val transparent = 0x00000000

    @Test
    fun whitePixelsProduceZeroBits() {
        val pixels = IntArray(8) { white }

        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 8, height = 1))
            .isEqualTo(byteArrayOf(0x00))
    }

    @Test
    fun blackPixelsProduceSetBits() {
        val pixels = IntArray(8) { black }

        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 8, height = 1))
            .isEqualTo(byteArrayOf(0xFF.toByte()))
    }

    @Test
    fun leftmostPixelSetsTheMostSignificantBit() {
        val pixels = intArrayOf(black, white, white, white, white, white, white, white)

        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 8, height = 1))
            .isEqualTo(byteArrayOf(0x80.toByte()))
    }

    @Test
    fun widthNotDivisibleByEightPadsTrailingBits() {
        val pixels = IntArray(12) { white }
        pixels[8] = black

        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 12, height = 1))
            .isEqualTo(byteArrayOf(0x00, 0x80.toByte()))
    }

    @Test
    fun multipleRowsKeepTheirOwnByteOffsets() {
        val pixels = IntArray(16) { white }
        pixels[1] = black
        pixels[14] = black

        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 8, height = 2))
            .isEqualTo(byteArrayOf(0x40, 0x02))
    }

    @Test
    fun transparentPixelsAreTreatedAsWhite() {
        val pixels = IntArray(8) { transparent }

        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 8, height = 1))
            .isEqualTo(byteArrayOf(0x00))
    }

    @Test
    fun thresholdControlsDarknessCutoff() {
        val midGray = 0xFF808080.toInt()
        val pixels = IntArray(8) { midGray }

        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 8, height = 1, threshold = 200))
            .isEqualTo(byteArrayOf(0xFF.toByte()))
        assertThat(MonochromeRasterizer.toRasterBytes(pixels, width = 8, height = 1, threshold = 100))
            .isEqualTo(byteArrayOf(0x00))
    }

    @Test
    fun rasterizerRejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            MonochromeRasterizer.toRasterBytes(IntArray(4), width = 0, height = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MonochromeRasterizer.toRasterBytes(IntArray(4), width = 2, height = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MonochromeRasterizer.toRasterBytes(IntArray(3), width = 2, height = 2)
        }
    }
}
