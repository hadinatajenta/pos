package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RupiahTest {

    @Test
    fun formatGroupsThousandsForSmallAmounts() {
        assertThat(Rupiah.format(0L)).isEqualTo("Rp 0")
        assertThat(Rupiah.format(4_000L)).isEqualTo("Rp 4.000")
        assertThat(Rupiah.format(18_000L)).isEqualTo("Rp 18.000")
        assertThat(Rupiah.format(1_250_000L)).isEqualTo("Rp 1.250.000")
    }

    @Test
    fun formatKeepsHundredsOfMillionsAndBillionsComplete() {
        assertThat(Rupiah.format(250_000_000L)).isEqualTo("Rp 250.000.000")
        assertThat(Rupiah.format(1_250_000_000L)).isEqualTo("Rp 1.250.000.000")
        assertThat(Rupiah.format(999_999_999_999L)).isEqualTo("Rp 999.999.999.999")
    }

    @Test
    fun formatNeverAbbreviatesLargeValues() {
        val formatted = Rupiah.format(1_250_000_000L)

        assertThat(formatted).doesNotContain("jt")
        assertThat(formatted).doesNotContain("rb")
        assertThat(formatted).doesNotContain("...")
        assertThat(formatted.filter { character -> character.isDigit() }).isEqualTo("1250000000")
    }

    @Test
    fun formatMarksNegativeAmountsWithSignBeforePrefix() {
        assertThat(Rupiah.format(-250_000L)).isEqualTo("-Rp 250.000")
        assertThat(Rupiah.format(-1_250_000_000L)).isEqualTo("-Rp 1.250.000.000")
    }
}
