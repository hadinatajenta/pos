package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.PricingContext
import org.junit.Assert.assertThrows
import org.junit.Test

class PriceCartTest {

    private val priceEngine = DefaultPriceEngine()

    @Test
    fun sumsItemSubtotalsIntoSubtotal() {
        val products = mapOf(
            1L to sampleProduct(id = 1L, basePrice = 5_000L),
            2L to sampleProduct(id = 2L, basePrice = 7_500L),
        )
        val lines = listOf(
            sampleLine(lineId = "l1", productId = 1L, qty = 2.0),
            sampleLine(lineId = "l2", productId = 2L, qty = 1.0),
        )

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext())

        assertThat(pricedCart.lines).hasSize(2)
        assertThat(pricedCart.subtotal).isEqualTo(17_500L)
        assertThat(pricedCart.grandTotal).isEqualTo(17_500L)
    }

    @Test
    fun itemDiscountReducesItemSubtotalBeforeInvoiceDiscount() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 5_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 2.0, itemDiscount = 1_000L))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext())

        assertThat(pricedCart.lines.single().itemSubtotal).isEqualTo(9_000L)
        assertThat(pricedCart.subtotal).isEqualTo(9_000L)
    }

    @Test
    fun invoiceDiscountReducesInvoiceNet() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 5_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 4.0))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext(invoiceDiscount = 2_000L))

        assertThat(pricedCart.subtotal).isEqualTo(20_000L)
        assertThat(pricedCart.invoiceDiscount).isEqualTo(2_000L)
        assertThat(pricedCart.grandTotal).isEqualTo(18_000L)
    }

    @Test
    fun taxIsFlooredOnInvoiceNet() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 1_999L))
        val lines = listOf(sampleLine(productId = 1L, qty = 1.0))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext(taxRatePercent = 10))

        assertThat(pricedCart.taxTotal).isEqualTo(199L)
        assertThat(pricedCart.grandTotal).isEqualTo(2_198L)
    }

    @Test
    fun taxAppliesToInvoiceNetAfterDiscounts() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 5_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 4.0, itemDiscount = 2_000L))

        val pricedCart = priceEngine.priceCart(
            lines,
            products,
            emptyMap(),
            PricingContext(invoiceDiscount = 3_000L, taxRatePercent = 10),
        )

        assertThat(pricedCart.subtotal).isEqualTo(18_000L)
        assertThat(pricedCart.taxTotal).isEqualTo(1_500L)
        assertThat(pricedCart.grandTotal).isEqualTo(16_500L)
    }

    @Test
    fun negativeTaxRateIsTreatedAsZero() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 5_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 2.0))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext(taxRatePercent = -5))

        assertThat(pricedCart.taxTotal).isEqualTo(0L)
    }

    @Test
    fun roundingIncrementRoundsHalfUpToNearestIncrement() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 17_500L))
        val lines = listOf(sampleLine(productId = 1L, qty = 1.0))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext(roundingIncrement = 1_000L))

        assertThat(pricedCart.rounding).isEqualTo(500L)
        assertThat(pricedCart.grandTotal).isEqualTo(18_000L)
    }

    @Test
    fun roundingAdjustmentCanBeNegative() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 17_400L))
        val lines = listOf(sampleLine(productId = 1L, qty = 1.0))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext(roundingIncrement = 1_000L))

        assertThat(pricedCart.rounding).isEqualTo(-400L)
        assertThat(pricedCart.grandTotal).isEqualTo(17_000L)
    }

    @Test
    fun zeroRoundingIncrementLeavesTotalUnchanged() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 17_500L))
        val lines = listOf(sampleLine(productId = 1L, qty = 1.0))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext(roundingIncrement = 0L))

        assertThat(pricedCart.rounding).isEqualTo(0L)
        assertThat(pricedCart.grandTotal).isEqualTo(17_500L)
    }

    @Test
    fun roundingAppliesToInvoiceNetPlusTax() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 10_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 1.0))

        val pricedCart = priceEngine.priceCart(
            lines,
            products,
            emptyMap(),
            PricingContext(taxRatePercent = 10, roundingIncrement = 1_000L),
        )

        assertThat(pricedCart.taxTotal).isEqualTo(1_000L)
        assertThat(pricedCart.rounding).isEqualTo(0L)
        assertThat(pricedCart.grandTotal).isEqualTo(11_000L)
    }

    @Test
    fun totalHppIsFlooredOnceAcrossLines() {
        val products = mapOf(1L to sampleProduct(id = 1L, baseHpp = 3L, basePrice = 100L))
        val lines = listOf(
            sampleLine(lineId = "l1", productId = 1L, qty = 0.5),
            sampleLine(lineId = "l2", productId = 1L, qty = 0.5),
        )

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext())

        assertThat(pricedCart.totalHpp).isEqualTo(3L)
        assertThat(pricedCart.netProfit).isEqualTo(97L)
    }

    @Test
    fun totalHppUsesUnitConversionFactor() {
        val products = mapOf(1L to sampleProduct(id = 1L, baseHpp = 1_000L, basePrice = 45_000L))
        val unitsByProduct = mapOf(1L to listOf(sampleUnit(id = 10L, productId = 1L, conversionFactor = 12.0, unitPrice = 45_000L)))
        val lines = listOf(sampleLine(productId = 1L, unitId = 10L, qty = 2.0))

        val pricedCart = priceEngine.priceCart(lines, products, unitsByProduct, PricingContext())

        assertThat(pricedCart.lines.single().unitHpp).isEqualTo(12_000L)
        assertThat(pricedCart.totalHpp).isEqualTo(24_000L)
        assertThat(pricedCart.netProfit).isEqualTo(66_000L)
    }

    @Test
    fun netProfitEqualsInvoiceNetMinusTotalHpp() {
        val products = mapOf(1L to sampleProduct(id = 1L, baseHpp = 1_000L, basePrice = 5_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 2.0))

        val pricedCart = priceEngine.priceCart(
            lines,
            products,
            emptyMap(),
            PricingContext(invoiceDiscount = 500L),
        )

        val invoiceNet = pricedCart.subtotal - pricedCart.invoiceDiscount
        assertThat(pricedCart.netProfit).isEqualTo(invoiceNet - pricedCart.totalHpp)
        assertThat(pricedCart.netProfit).isEqualTo(7_500L)
    }

    @Test
    fun invoiceDiscountCannotDriveInvoiceNetNegative() {
        val products = mapOf(1L to sampleProduct(id = 1L, basePrice = 5_000L))
        val lines = listOf(sampleLine(productId = 1L, qty = 1.0))

        val pricedCart = priceEngine.priceCart(lines, products, emptyMap(), PricingContext(invoiceDiscount = 8_000L))

        assertThat(pricedCart.grandTotal).isEqualTo(0L)
        assertThat(pricedCart.taxTotal).isEqualTo(0L)
    }

    @Test
    fun emptyCartProducesZeroTotals() {
        val pricedCart = priceEngine.priceCart(emptyList(), emptyMap(), emptyMap(), PricingContext())

        assertThat(pricedCart.lines).isEmpty()
        assertThat(pricedCart.subtotal).isEqualTo(0L)
        assertThat(pricedCart.taxTotal).isEqualTo(0L)
        assertThat(pricedCart.rounding).isEqualTo(0L)
        assertThat(pricedCart.grandTotal).isEqualTo(0L)
        assertThat(pricedCart.totalHpp).isEqualTo(0L)
        assertThat(pricedCart.netProfit).isEqualTo(0L)
    }

    @Test
    fun unknownProductIsRejected() {
        val lines = listOf(sampleLine(productId = 42L, qty = 1.0))

        assertThrows(IllegalArgumentException::class.java) {
            priceEngine.priceCart(lines, emptyMap(), emptyMap(), PricingContext())
        }
    }
}
