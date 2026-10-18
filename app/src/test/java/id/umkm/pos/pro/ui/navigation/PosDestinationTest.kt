package id.umkm.pos.pro.ui.navigation

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import id.umkm.pos.pro.ui.localizedLabel
import org.junit.Test

class PosDestinationTest {

    @Test
    fun exposesTheFivePrimaryDestinationsInOrder() {
        val names = PosDestination.entries.map { it.name }

        assertThat(names)
            .containsExactly("KASIR", "PRODUK", "TRANSAKSI", "LAPORAN", "LAINNYA")
            .inOrder()
    }

    @Test
    fun kasirIsTheDefaultDestination() {
        assertThat(PosDestination.entries.first()).isEqualTo(PosDestination.KASIR)
    }

    @Test
    fun resolvesIndonesianLabels() {
        val labels = PosDestination.entries.map { it.localizedLabel(IdStrings) }

        assertThat(labels)
            .containsExactly("Kasir", "Produk", "Transaksi", "Laporan", "Lainnya")
            .inOrder()
    }

    @Test
    fun resolvesEnglishLabels() {
        val labels = PosDestination.entries.map { it.localizedLabel(EnStrings) }

        assertThat(labels)
            .containsExactly("Cashier", "Products", "Transactions", "Reports", "More")
            .inOrder()
    }

    @Test
    fun resolvesChineseLabels() {
        val labels = PosDestination.entries.map { it.localizedLabel(ZhStrings) }

        assertThat(labels)
            .containsExactly("收银", "商品", "交易", "报表", "更多")
            .inOrder()
    }

    @Test
    fun everyDestinationHasANonEmptyLabelInEveryLanguage() {
        val catalogues: List<PosStrings> = listOf(IdStrings, EnStrings, ZhStrings)

        catalogues.forEach { strings ->
            PosDestination.entries.forEach { destination ->
                assertThat(destination.localizedLabel(strings).isBlank()).isFalse()
            }
        }
    }
}
