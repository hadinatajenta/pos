package id.umkm.pos.pro.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PosDestinationTest {

    @Test
    fun exposesTheFivePrimaryDestinationsInOrder() {
        val names = PosDestination.entries.map { it.name }

        assertThat(names)
            .containsExactly("CASHIER", "PRODUCTS", "CUSTOMERS", "REPORTS", "MORE")
            .inOrder()
    }

    @Test
    fun cashierIsTheDefaultDestination() {
        assertThat(PosDestination.entries.first()).isEqualTo(PosDestination.CASHIER)
    }

    @Test
    fun everyDestinationHasANonEmptyLabel() {
        PosDestination.entries.forEach { destination ->
            assertThat(destination.label).isNotEmpty()
        }
    }
}
