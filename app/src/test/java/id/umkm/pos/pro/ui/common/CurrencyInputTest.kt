package id.umkm.pos.pro.ui.common

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CurrencyInputTest {

    @Test
    fun groupedLeavesEmptyInputEmpty() {
        assertThat(CurrencyDigits.grouped("")).isEmpty()
    }

    @Test
    fun groupedKeepsASingleZero() {
        assertThat(CurrencyDigits.grouped("0")).isEqualTo("0")
    }

    @Test
    fun groupedSeparatesSixDigits() {
        assertThat(CurrencyDigits.grouped("518000")).isEqualTo("518.000")
    }

    @Test
    fun groupedSeparatesSevenDigits() {
        assertThat(CurrencyDigits.grouped("1234567")).isEqualTo("1.234.567")
    }

    @Test
    fun transformationDisplaysGroupedDigits() {
        val transformed = ThousandSeparatorVisualTransformation.filter(AnnotatedString("518000"))

        assertThat(transformed.text.text).isEqualTo("518.000")
    }

    @Test
    fun originalToTransformedMapsDigitBoundaries() {
        val mapping = ThousandSeparatorVisualTransformation
            .filter(AnnotatedString("518000"))
            .offsetMapping

        assertThat(mapping.originalToTransformed(0)).isEqualTo(0)
        assertThat(mapping.originalToTransformed(3)).isEqualTo(3)
        assertThat(mapping.originalToTransformed(4)).isEqualTo(5)
        assertThat(mapping.originalToTransformed(6)).isEqualTo(7)
    }

    @Test
    fun transformedToOriginalMapsAcrossTheSeparator() {
        val mapping = ThousandSeparatorVisualTransformation
            .filter(AnnotatedString("518000"))
            .offsetMapping

        assertThat(mapping.transformedToOriginal(0)).isEqualTo(0)
        assertThat(mapping.transformedToOriginal(3)).isEqualTo(3)
        assertThat(mapping.transformedToOriginal(4)).isEqualTo(3)
        assertThat(mapping.transformedToOriginal(5)).isEqualTo(4)
        assertThat(mapping.transformedToOriginal(7)).isEqualTo(6)
    }

    @Test
    fun transformationDropsNonDigitCharactersWithoutCrashing() {
        val transformed = ThousandSeparatorVisualTransformation.filter(AnnotatedString("51a8"))

        assertThat(transformed.text.text).isEqualTo("518")
    }
}
