package id.umkm.pos.pro.ui.customers

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WhatsappNumberTest {

    @Test
    fun replacesALeadingZeroWithTheCountryCode() {
        assertThat(normalizeWhatsappNumber("0812-3456-7890")).isEqualTo("6281234567890")
    }

    @Test
    fun keepsNumbersThatAlreadyCarryTheCountryCode() {
        assertThat(normalizeWhatsappNumber("+62 812 3456 7890")).isEqualTo("6281234567890")
    }

    @Test
    fun addsTheCountryCodeForNumbersStartingWithEight() {
        assertThat(normalizeWhatsappNumber("812 3456 7890")).isEqualTo("6281234567890")
    }

    @Test
    fun rejectsValuesWithoutDigits() {
        assertThat(normalizeWhatsappNumber("")).isNull()
        assertThat(normalizeWhatsappNumber("—")).isNull()
    }
}
