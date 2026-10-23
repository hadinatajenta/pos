package id.umkm.pos.pro.ui.products

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal object RupiahInputVisualTransformation : VisualTransformation {

    private const val GROUP_SIZE = 3
    private const val THOUSAND_SEPARATOR = '.'

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { character -> character.isDigit() }
        val formatted = StringBuilder(digits.length + digits.length / GROUP_SIZE)
        val originalToTransformed = IntArray(digits.length + 1)
        digits.forEachIndexed { index, digit ->
            if (index > 0 && (digits.length - index) % GROUP_SIZE == 0) {
                formatted.append(THOUSAND_SEPARATOR)
            }
            formatted.append(digit)
            originalToTransformed[index + 1] = formatted.length
        }
        val transformedToOriginal = IntArray(formatted.length + 1)
        var digitsBeforePosition = 0
        for (position in 0..formatted.length) {
            transformedToOriginal[position] = digitsBeforePosition
            if (position < formatted.length && formatted[position].isDigit()) {
                digitsBeforePosition++
            }
        }
        return TransformedText(
            text = AnnotatedString(formatted.toString()),
            offsetMapping = RupiahOffsetMapping(
                originalToTransformed = originalToTransformed,
                transformedToOriginal = transformedToOriginal,
            ),
        )
    }

    private class RupiahOffsetMapping(
        private val originalToTransformed: IntArray,
        private val transformedToOriginal: IntArray,
    ) : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int =
            originalToTransformed[offset.coerceIn(0, originalToTransformed.lastIndex)]

        override fun transformedToOriginal(offset: Int): Int =
            transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
    }
}
