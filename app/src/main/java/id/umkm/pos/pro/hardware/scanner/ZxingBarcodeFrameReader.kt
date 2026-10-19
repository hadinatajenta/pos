package id.umkm.pos.pro.hardware.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

class ZxingBarcodeFrameReader : BarcodeFrameReader {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to RETAIL_FORMATS,
                DecodeHintType.TRY_HARDER to false,
            ),
        )
    }

    override fun read(luminance: ByteArray, rowStride: Int, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || rowStride < width) return null
        if (luminance.size < rowStride * height) return null
        val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
        val code = try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))?.text
        } catch (error: ReaderException) {
            null
        } finally {
            reader.reset()
        }
        return code?.takeIf { candidate -> isPlausibleRetailCode(candidate) }
    }

    private companion object {
        val RETAIL_FORMATS = listOf(
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.CODE_128,
            BarcodeFormat.ITF,
        )

    }
}

internal const val MIN_RETAIL_CODE_LENGTH = 8
internal const val MAX_RETAIL_CODE_LENGTH = 14
internal const val MIN_ALPHANUMERIC_CODE_LENGTH = 4

internal fun isPlausibleRetailCode(code: String): Boolean {
    if (code.length < MIN_ALPHANUMERIC_CODE_LENGTH || code.length > MAX_RETAIL_CODE_LENGTH) return false
    val isDigitsOnly = code.all { character -> character.isDigit() }
    return !isDigitsOnly || code.length >= MIN_RETAIL_CODE_LENGTH
}
