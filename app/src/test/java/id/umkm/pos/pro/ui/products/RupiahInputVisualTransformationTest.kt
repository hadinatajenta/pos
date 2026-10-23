package id.umkm.pos.pro.ui.products

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RupiahInputVisualTransformationTest {

    private fun transform(input: String): TransformedText =
        RupiahInputVisualTransformation.filter(AnnotatedString(input))

    @Test
    fun groupsFiveDigitsWithOneSeparator() {
        assertThat(transform("15000").text.text).isEqualTo("15.000")
    }

    @Test
    fun groupsSevenDigitsWithTwoSeparators() {
        assertThat(transform("1500000").text.text).isEqualTo("1.500.000")
    }

    @Test
    fun leavesShortInputUngrouped() {
        assertThat(transform("999").text.text).isEqualTo("999")
        assertThat(transform("1000").text.text).isEqualTo("1.000")
    }

    @Test
    fun emptyInputStaysEmpty() {
        assertThat(transform("").text.text).isEmpty()
    }

    @Test
    fun mapsTheCursorAtTheStartMiddleAndEnd() {
        val mapping = transform("15000").offsetMapping

        assertThat(mapping.originalToTransformed(0)).isEqualTo(0)
        assertThat(mapping.originalToTransformed(2)).isEqualTo(2)
        assertThat(mapping.originalToTransformed(3)).isEqualTo(4)
        assertThat(mapping.originalToTransformed(5)).isEqualTo(6)
    }

    @Test
    fun mapsTheCursorAfterASeparatorBackToTheDigitBoundary() {
        val mapping = transform("15000").offsetMapping

        assertThat(mapping.transformedToOriginal(2)).isEqualTo(2)
        assertThat(mapping.transformedToOriginal(3)).isEqualTo(2)
        assertThat(mapping.transformedToOriginal(4)).isEqualTo(3)
        assertThat(mapping.transformedToOriginal(6)).isEqualTo(5)
    }
}
