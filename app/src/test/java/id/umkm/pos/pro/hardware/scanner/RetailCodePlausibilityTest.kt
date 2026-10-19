package id.umkm.pos.pro.hardware.scanner

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetailCodePlausibilityTest {

    @Test
    fun acceptsRetailNumericLengths() {
        assertThat(isPlausibleRetailCode("12345678")).isTrue()
        assertThat(isPlausibleRetailCode("123456789012")).isTrue()
        assertThat(isPlausibleRetailCode("8990000000001")).isTrue()
        assertThat(isPlausibleRetailCode("12345678901234")).isTrue()
    }

    @Test
    fun rejectsNumericCodesOutsideRetailLengths() {
        assertThat(isPlausibleRetailCode("1234567")).isFalse()
        assertThat(isPlausibleRetailCode("123456789012345")).isFalse()
        assertThat(isPlausibleRetailCode("12")).isFalse()
    }

    @Test
    fun acceptsAlphanumericLabels() {
        assertThat(isPlausibleRetailCode("KOPI-001")).isTrue()
        assertThat(isPlausibleRetailCode("AB12")).isTrue()
    }

    @Test
    fun rejectsCodesThatAreTooShortOrTooLong() {
        assertThat(isPlausibleRetailCode("AB1")).isFalse()
        assertThat(isPlausibleRetailCode("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567")).isFalse()
    }
}
