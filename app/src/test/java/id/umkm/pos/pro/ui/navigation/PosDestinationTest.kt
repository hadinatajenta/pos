package id.umkm.pos.pro.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PosDestinationTest {

    @Test
    fun exposesTheSixPrimaryDestinationsInOrder() {
        val names = PosDestination.entries.map { it.name }

        assertThat(names)
            .containsExactly("BERANDA", "PRODUK", "KASIR", "TRANSAKSI", "LAPORAN", "LAINNYA")
            .inOrder()
    }

    @Test
    fun berandaIsTheDefaultDestination() {
        assertThat(PosDestination.entries.first()).isEqualTo(PosDestination.BERANDA)
    }

    @Test
    fun destinationsUseIndonesianLabels() {
        val labels = PosDestination.entries.map { it.label }

        assertThat(labels)
            .containsExactly("Beranda", "Produk", "Kasir", "Transaksi", "Laporan", "Lainnya")
            .inOrder()
    }

    @Test
    fun everyDestinationHasANonEmptyLabel() {
        PosDestination.entries.forEach { destination ->
            assertThat(destination.label).isNotEmpty()
        }
    }
}
