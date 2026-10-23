package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.uiSampleProduct
import org.junit.Test

class ProductStockStateTest {

    private fun product(stock: Double, minStockAlert: Double = 5.0): Product = uiSampleProduct(
        id = 1L,
        stockBaseQty = stock,
        minStockAlert = minStockAlert,
    )

    @Test
    fun zeroStockIsOut() {
        assertThat(resolveProductStockState(product(0.0))).isEqualTo(ProductStockState.OUT)
    }

    @Test
    fun negativeStockIsOut() {
        assertThat(resolveProductStockState(product(-4.0))).isEqualTo(ProductStockState.OUT)
    }

    @Test
    fun stockAtTheThresholdIsLow() {
        assertThat(resolveProductStockState(product(5.0, minStockAlert = 5.0))).isEqualTo(ProductStockState.LOW)
    }

    @Test
    fun stockBelowTheThresholdIsLow() {
        assertThat(resolveProductStockState(product(4.9, minStockAlert = 5.0))).isEqualTo(ProductStockState.LOW)
        assertThat(resolveProductStockState(product(1.0, minStockAlert = 5.0))).isEqualTo(ProductStockState.LOW)
    }

    @Test
    fun stockAboveTheThresholdIsOk() {
        assertThat(resolveProductStockState(product(5.1, minStockAlert = 5.0))).isEqualTo(ProductStockState.OK)
        assertThat(resolveProductStockState(product(120.0, minStockAlert = 5.0))).isEqualTo(ProductStockState.OK)
    }

    @Test
    fun nonFiniteStockIsOk() {
        assertThat(resolveProductStockState(product(Double.NaN))).isEqualTo(ProductStockState.OK)
        assertThat(resolveProductStockState(product(Double.POSITIVE_INFINITY))).isEqualTo(ProductStockState.OK)
    }

    @Test
    fun outOfStockIsNotCountedAsLowStock() {
        val products = listOf(
            product(10.0),
            product(5.0),
            product(0.0),
            product(-1.0),
        )
        val stockStates = products.map { product -> resolveProductStockState(product) }
        val catalog = buildProductsCatalog(
            productItems = products.map { product ->
                ProductListItem(product = product, stockStatus = resolveStockStatus(product))
            },
            categoryFilter = null,
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(stockStates.count { state -> state == ProductStockState.LOW }).isEqualTo(1)
        assertThat(stockStates.count { state -> state == ProductStockState.OUT }).isEqualTo(2)
        assertThat(catalog.summary.lowCount).isEqualTo(1)
        assertThat(catalog.summary.outCount).isEqualTo(2)
    }
}
