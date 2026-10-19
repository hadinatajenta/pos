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
}
