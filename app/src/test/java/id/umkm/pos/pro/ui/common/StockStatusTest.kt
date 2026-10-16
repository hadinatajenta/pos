package id.umkm.pos.pro.ui.common

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.Product
import org.junit.Test

class StockStatusTest {

    private fun product(stock: Double, minAlert: Double): Product = Product(
        id = 1L,
        sku = "SKU-1",
        barcode = null,
        name = "Produk",
        category = null,
        baseUnit = "pcs",
        baseHpp = 1_000L,
        basePrice = 2_000L,
        stockBaseQty = stock,
        minStockAlert = minAlert,
        isActive = true,
    )

    @Test
    fun emptyStockIsOutOfStock() {
        assertThat(resolveStockStatus(product(stock = 0.0, minAlert = 5.0))).isEqualTo(StockStatus.OUT_OF_STOCK)
    }

    @Test
    fun negativeStockIsOutOfStock() {
        assertThat(resolveStockStatus(product(stock = -1.0, minAlert = 0.0))).isEqualTo(StockStatus.OUT_OF_STOCK)
    }

    @Test
    fun stockAtTheAlertThresholdIsLow() {
        assertThat(resolveStockStatus(product(stock = 5.0, minAlert = 5.0))).isEqualTo(StockStatus.LOW_STOCK)
    }

    @Test
    fun stockAboveTheAlertThresholdIsInStock() {
        assertThat(resolveStockStatus(product(stock = 5.01, minAlert = 5.0))).isEqualTo(StockStatus.IN_STOCK)
    }

    @Test
    fun everyStatusHasADistinctLabel() {
        val labels = StockStatus.entries.map { stockStatusLabel(it) }

        assertThat(labels.toSet()).hasSize(StockStatus.entries.size)
        assertThat(labels).contains("Out of stock")
        assertThat(labels).contains("Low stock")
        assertThat(labels).contains("In stock")
    }
}
