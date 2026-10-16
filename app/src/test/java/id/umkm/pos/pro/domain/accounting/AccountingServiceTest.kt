package id.umkm.pos.pro.domain.accounting

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.PricingContext
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.pos.sampleLine
import id.umkm.pos.pro.domain.pos.sampleProduct
import org.junit.Test

class AccountingServiceTest {

    private val accountingService = DefaultAccountingService()

    @Test
    fun netProfitIsInvoiceNetMinusTotalHpp() {
        val pricedCart = PricedCart(
            lines = emptyList(),
            subtotal = 17_500L,
            invoiceDiscount = 500L,
            taxTotal = 1_700L,
            rounding = 0L,
            grandTotal = 18_700L,
            totalHpp = 4_000L,
            netProfit = 0L,
        )

        assertThat(accountingService.netProfit(pricedCart)).isEqualTo(13_000L)
    }

    @Test
    fun netProfitExcludesTaxAndRounding() {
        val pricedCart = PricedCart(
            lines = emptyList(),
            subtotal = 10_000L,
            invoiceDiscount = 0L,
            taxTotal = 1_000L,
            rounding = 250L,
            grandTotal = 11_250L,
            totalHpp = 3_000L,
            netProfit = 0L,
        )

        assertThat(accountingService.netProfit(pricedCart)).isEqualTo(7_000L)
    }

    @Test
    fun netProfitCanBeNegativeWhenHppExceedsRevenue() {
        val pricedCart = PricedCart(
            lines = emptyList(),
            subtotal = 5_000L,
            invoiceDiscount = 0L,
            taxTotal = 0L,
            rounding = 0L,
            grandTotal = 5_000L,
            totalHpp = 6_000L,
            netProfit = 0L,
        )

        assertThat(accountingService.netProfit(pricedCart)).isEqualTo(-1_000L)
    }

    @Test
    fun invoiceDiscountBeyondSubtotalClampsInvoiceNetToZero() {
        val pricedCart = PricedCart(
            lines = emptyList(),
            subtotal = 5_000L,
            invoiceDiscount = 8_000L,
            taxTotal = 0L,
            rounding = 0L,
            grandTotal = 0L,
            totalHpp = 2_000L,
            netProfit = 0L,
        )

        assertThat(accountingService.netProfit(pricedCart)).isEqualTo(-2_000L)
    }

    @Test
    fun netProfitMatchesPriceEngineNetProfit() {
        val products = mapOf(1L to sampleProduct(id = 1L, baseHpp = 1_000L, basePrice = 5_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 2.0))

        val pricedCart = DefaultPriceEngine().priceCart(
            lines,
            products,
            emptyMap(),
            PricingContext(invoiceDiscount = 500L, taxRatePercent = 10),
        )

        assertThat(accountingService.netProfit(pricedCart)).isEqualTo(pricedCart.netProfit)
    }
}
