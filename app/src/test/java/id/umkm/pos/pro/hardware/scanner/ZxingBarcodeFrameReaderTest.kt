package id.umkm.pos.pro.hardware.scanner

import com.google.common.truth.Truth.assertThat
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.EAN13Writer
import org.junit.Test

class ZxingBarcodeFrameReaderTest {

    private val reader = ZxingBarcodeFrameReader()

    @Test
    fun readsAnEan13Frame() {
        val frame = renderEan13("590123412345")

        val code = reader.read(frame.luminance, frame.rowStride, frame.width, frame.height)

        assertThat(code).isNotNull()
        assertThat(code!!).startsWith("590123412345")
        assertThat(code.length).isEqualTo(13)
    }

    @Test
    fun returnsNullForABlankFrame() {
        val width = 320
        val height = 240
        val blank = ByteArray(width * height) { 255.toByte() }

        val code = reader.read(blank, width, width, height)

        assertThat(code).isNull()
    }

    @Test
    fun returnsNullForAnInvalidStride() {
        val width = 320
        val height = 240
        val frame = ByteArray(width * height) { 255.toByte() }

        assertThat(reader.read(frame, rowStride = 100, width = width, height = height)).isNull()
    }

    @Test
    fun returnsNullWhenTheBufferIsShorterThanTheFrame() {
        val width = 320
        val height = 240
        val shortBuffer = ByteArray(10)

        assertThat(reader.read(shortBuffer, rowStride = width, width = width, height = height)).isNull()
    }

    private data class LuminanceFrame(
        val luminance: ByteArray,
        val rowStride: Int,
        val width: Int,
        val height: Int,
    )

    private fun renderEan13(contents: String): LuminanceFrame {
        val width = 600
        val height = 300
        val rowStride = width + 16
        val matrix: BitMatrix = EAN13Writer().encode(contents, BarcodeFormat.EAN_13, width, height)
        val luminance = ByteArray(rowStride * height) { 255.toByte() }
        for (y in 0 until height) {
            for (x in 0 until width) {
                luminance[y * rowStride + x] = if (matrix.get(x, y)) 0 else 255.toByte()
            }
        }
        return LuminanceFrame(luminance, rowStride, width, height)
    }
}
