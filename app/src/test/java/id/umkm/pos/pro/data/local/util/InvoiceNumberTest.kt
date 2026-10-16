package id.umkm.pos.pro.data.local.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.TimeZone

class InvoiceNumberTest {

    @Test
    fun formatsLocalDayAndFourDigitSequence() {
        assertThat(InvoiceNumber.format(SAMPLE_MILLIS, 7, UTC))
            .isEqualTo("INV-20231114-0007")
    }

    @Test
    fun derivesDayFromProvidedTimeZone() {
        assertThat(InvoiceNumber.format(SAMPLE_MILLIS, 1, JAKARTA))
            .isEqualTo("INV-20231115-0001")
    }

    @Test
    fun keepsSequencesBeyondFourDigitsWithoutTruncation() {
        assertThat(InvoiceNumber.format(SAMPLE_MILLIS, 12_345, UTC))
            .isEqualTo("INV-20231114-12345")
    }

    @Test
    fun dayKeyMatchesInvoicesUsedForDailyCounters() {
        assertThat(InvoiceNumber.dayKey(SAMPLE_MILLIS, UTC)).isEqualTo("20231114")
        assertThat(InvoiceNumber.dayKey(SAMPLE_MILLIS, JAKARTA)).isEqualTo("20231115")
    }

    private companion object {
        const val SAMPLE_MILLIS = 1_700_000_000_000L
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        val JAKARTA: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }
}
