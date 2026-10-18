package id.umkm.pos.pro.ui.components

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import org.junit.Test

class ChartsTest {

    @Test
    fun valuesBelowOneThousandAreRenderedWithoutSuffix() {
        assertThat(compactRupiah(0.0, IdStrings)).isEqualTo("0")
        assertThat(compactRupiah(999.0, IdStrings)).isEqualTo("999")
    }

    @Test
    fun thousandsRoundToWholeNumbers() {
        assertThat(compactRupiah(1_000.0, IdStrings)).isEqualTo("1 rb")
        assertThat(compactRupiah(12_500.0, IdStrings)).isEqualTo("13 rb")
        assertThat(compactRupiah(999_499.0, IdStrings)).isEqualTo("999 rb")
    }

    @Test
    fun millionsKeepOneDecimalPlace() {
        assertThat(compactRupiah(1_000_000.0, IdStrings)).isEqualTo("1.0 jt")
        assertThat(compactRupiah(2_450_000.0, IdStrings)).isEqualTo("2.5 jt")
    }

    @Test
    fun suffixesFollowTheActiveCatalogue() {
        assertThat(compactRupiah(12_500.0, EnStrings)).isEqualTo("13 k")
        assertThat(compactRupiah(2_450_000.0, EnStrings)).isEqualTo("2.5 M")
        assertThat(compactRupiah(12_500.0, ZhStrings)).isEqualTo("13 千")
        assertThat(compactRupiah(2_450_000.0, ZhStrings)).isEqualTo("2.5 百万")
    }
}
