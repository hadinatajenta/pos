package id.umkm.pos.pro.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object CurrencyDigits {
    fun grouped(digits: String): String {
        if (digits.isEmpty()) return ""
        return digits.reversed().chunked(3).joinToString(".").reversed()
    }
}

object ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter(Char::isDigit)
        val grouped = CurrencyDigits.grouped(digits)
        return TransformedText(
            text = AnnotatedString(grouped),
            offsetMapping = ThousandSeparatorOffsetMapping(
                originalLength = digits.length,
                transformed = grouped,
            ),
        )
    }
}

internal class ThousandSeparatorOffsetMapping(
    private val originalLength: Int,
    private val transformed: String,
) : OffsetMapping {

    private val originalToTransformedOffsets = IntArray(originalLength + 1).also { offsets ->
        var digitCount = 0
        offsets[0] = 0
        transformed.forEachIndexed { index, char ->
            if (char.isDigit()) {
                digitCount++
                offsets[digitCount] = index + 1
            }
        }
    }

    override fun originalToTransformed(offset: Int): Int =
        originalToTransformedOffsets[offset.coerceIn(0, originalLength)]

    override fun transformedToOriginal(offset: Int): Int =
        transformed.take(offset.coerceIn(0, transformed.length)).count(Char::isDigit)
}
