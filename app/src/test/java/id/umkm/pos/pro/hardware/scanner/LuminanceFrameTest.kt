package id.umkm.pos.pro.hardware.scanner

import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Test

class LuminanceFrameTest {

    @Test
    fun extractsThePaddedRowsWholeWhenStrideExceedsWidth() {
        val width = 4
        val height = 3
        val rowStride = 6
        val buffer = ByteBuffer.allocate(rowStride * height)
        for (value in 0 until rowStride * height) {
            buffer.put((value % 200).toByte())
        }
        buffer.rewind()

        val luminance = LuminanceFrame.extract(buffer, rowStride, width, height)

        assertThat(luminance).isNotNull()
        assertThat(luminance!!.size).isEqualTo(rowStride * height)
        assertThat(luminance[0]).isEqualTo(0.toByte())
        assertThat(luminance[rowStride * height - 1]).isEqualTo(((rowStride * height - 1) % 200).toByte())
    }

    @Test
    fun extractsOnlyTheRequestedBytesFromALargerBuffer() {
        val width = 4
        val height = 2
        val rowStride = 4
        val buffer = ByteBuffer.allocate(rowStride * height + 32)
        buffer.put(ByteArray(rowStride * height) { index -> index.toByte() })
        buffer.put(ByteArray(32) { 99.toByte() })
        buffer.rewind()

        val luminance = LuminanceFrame.extract(buffer, rowStride, width, height)

        assertThat(luminance).isNotNull()
        assertThat(luminance!!.size).isEqualTo(rowStride * height)
        assertThat(luminance.last()).isEqualTo((rowStride * height - 1).toByte())
    }

    @Test
    fun refusesABufferShorterThanTheFrame() {
        val buffer = ByteBuffer.allocate(8)

        val luminance = LuminanceFrame.extract(buffer, rowStride = 4, width = 4, height = 4)

        assertThat(luminance).isNull()
    }

    @Test
    fun refusesAStrideNarrowerThanTheWidth() {
        val buffer = ByteBuffer.allocate(64)

        val luminance = LuminanceFrame.extract(buffer, rowStride = 3, width = 4, height = 4)

        assertThat(luminance).isNull()
    }

    @Test
    fun refusesAnEmptyFrame() {
        val buffer = ByteBuffer.allocate(16)

        assertThat(LuminanceFrame.extract(buffer, rowStride = 4, width = 0, height = 4)).isNull()
        assertThat(LuminanceFrame.extract(buffer, rowStride = 4, width = 4, height = 0)).isNull()
    }

    @Test
    fun aQuarterTurnClockwiseRestoresAFrameRotatedCounterClockwise() {
        val width = 4
        val height = 2
        val original = ByteArray(width * height) { index -> index.toByte() }

        val turned = LuminanceFrame.rotateQuarterTurn(original, width, width, height, clockwise = false)
        assertThat(turned).isNotNull()
        assertThat(turned!!.width).isEqualTo(height)
        assertThat(turned.height).isEqualTo(width)

        val restored = LuminanceFrame.rotateQuarterTurn(
            turned.luminance,
            turned.rowStride,
            turned.width,
            turned.height,
            clockwise = true,
        )

        assertThat(restored).isNotNull()
        assertThat(restored!!.luminance).isEqualTo(original)
    }

    @Test
    fun aQuarterTurnMovesPixelsToTheExpectedCorner() {
        val width = 2
        val height = 3
        val original = byteArrayOf(1, 2, 3, 4, 5, 6)

        val clockwise = LuminanceFrame.rotateQuarterTurn(original, width, width, height, clockwise = true)

        assertThat(clockwise).isNotNull()
        assertThat(clockwise!!.width).isEqualTo(3)
        assertThat(clockwise.height).isEqualTo(2)
        assertThat(clockwise.luminance).isEqualTo(byteArrayOf(5, 3, 1, 6, 4, 2))
    }

    @Test
    fun aQuarterTurnRefusesAnInvalidInput() {
        val frame = ByteArray(16)

        assertThat(LuminanceFrame.rotateQuarterTurn(frame, rowStride = 2, width = 4, height = 4, clockwise = true))
            .isNull()
        assertThat(LuminanceFrame.rotateQuarterTurn(ByteArray(4), rowStride = 4, width = 4, height = 4, clockwise = true))
            .isNull()
    }
}
