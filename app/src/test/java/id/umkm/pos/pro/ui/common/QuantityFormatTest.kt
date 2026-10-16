package id.umkm.pos.pro.ui.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuantityFormatTest {

    @Test
    fun wholeQuantitiesPrintWithoutDecimalSeparators() {
        assertThat(formatQuantity(3.0)).isEqualTo("3")
        assertThat(formatQuantity(0.0)).isEqualTo("0")
    }

    @Test
    fun fractionalQuantitiesKeepSignificantDecimals() {
        assertThat(formatQuantity(1.5)).isEqualTo("1.5")
        assertThat(formatQuantity(0.125)).isEqualTo("0.125")
    }

    @Test
    fun trailingZerosAreTrimmed() {
        assertThat(formatQuantity(2.5)).isEqualTo("2.5")
        assertThat(formatQuantity(10.25)).isEqualTo("10.25")
    }

    @Test
    fun invalidNumbersBecomeZero() {
        assertThat(formatQuantity(Double.NaN)).isEqualTo("0")
        assertThat(formatQuantity(Double.POSITIVE_INFINITY)).isEqualTo("0")
    }

    @Test
    fun quantityWithUnitAppendsTheUnit() {
        assertThat(formatQuantityWithUnit(2.5, "kg")).isEqualTo("2.5 kg")
    }
}
