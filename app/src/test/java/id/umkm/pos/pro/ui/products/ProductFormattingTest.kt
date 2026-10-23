package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.uiSampleProduct
import org.junit.Test

class ProductFormattingTest {

    private fun product(stock: Double, basePrice: Long): Product = uiSampleProduct(
        id = 1L,
        stockBaseQty = stock,
        basePrice = basePrice,
    )

    @Test
    fun amountsBelowOneMillionUseFullRupiahGrouping() {
        assertThat(formatCompactRupiah(0L)).isEqualTo("Rp 0")
        assertThat(formatCompactRupiah(850_000L)).isEqualTo("Rp 850.000")
        assertThat(formatCompactRupiah(999_999L)).isEqualTo("Rp 999.999")
    }

    @Test
    fun millionsAreCompactedToTwoDecimals() {
        assertThat(formatCompactRupiah(1_000_000L)).isEqualTo("Rp 1,00 jt")
        assertThat(formatCompactRupiah(2_080_000L)).isEqualTo("Rp 2,08 jt")
    }

    @Test
    fun billionsAreCompactedWithTheMiliarSuffix() {
        assertThat(formatCompactRupiah(1_250_000_000L)).isEqualTo("Rp 1,25 M")
    }

    @Test
    fun stockValueUsesSellingPriceAndClampsNegativeStock() {
        val stockValue = stockValueOf(
            listOf(
                product(stock = -5.0, basePrice = 1_000L),
                product(stock = 2.0, basePrice = 500L),
                product(stock = 0.0, basePrice = 9_000L),
            ),
        )

        assertThat(stockValue).isEqualTo(1_000L)
    }

    @Test
    fun stockValueIgnoresNonFiniteStock() {
        val stockValue = stockValueOf(
            listOf(
                product(stock = Double.NaN, basePrice = 1_000L),
                product(stock = Double.POSITIVE_INFINITY, basePrice = 1_000L),
                product(stock = 2.0, basePrice = 500L),
            ),
        )

        assertThat(stockValue).isEqualTo(1_000L)
    }
}
