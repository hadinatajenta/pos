package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PriceLineTest {

    private val priceEngine = DefaultPriceEngine()

    @Test
    fun negotiatedPriceTakesPrecedenceOverWholesaleAndBase() {
        val product = sampleProduct(basePrice = 5_000L)
        val units = listOf(sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 5, wholesalePrice = 42_000L))
        val line = sampleLine(unitId = 10L, qty = 10.0, negotiatedUnitPrice = 39_000L)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(39_000L)
        assertThat(pricedLine.isNegotiated).isTrue()
        assertThat(pricedLine.isWholesale).isFalse()
        assertThat(pricedLine.itemSubtotal).isEqualTo(390_000L)
    }

    @Test
    fun wholesaleTierAppliesAtExactBoundary() {
        val product = sampleProduct()
        val units = listOf(sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 5, wholesalePrice = 42_000L))
        val line = sampleLine(unitId = 10L, qty = 5.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(42_000L)
        assertThat(pricedLine.isWholesale).isTrue()
        assertThat(pricedLine.isNegotiated).isFalse()
        assertThat(pricedLine.itemSubtotal).isEqualTo(210_000L)
    }

    @Test
    fun wholesaleTierDoesNotApplyBelowBoundary() {
        val product = sampleProduct()
        val units = listOf(sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 5, wholesalePrice = 42_000L))
        val line = sampleLine(unitId = 10L, qty = 4.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(45_000L)
        assertThat(pricedLine.isWholesale).isFalse()
        assertThat(pricedLine.itemSubtotal).isEqualTo(180_000L)
    }

    @Test
    fun selectsWholesaleTierWithHighestEligibleMinimum() {
        val product = sampleProduct()
        val units = listOf(
            sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 5, wholesalePrice = 42_000L),
            sampleUnit(id = 11L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 10, wholesalePrice = 40_000L),
        )
        val line = sampleLine(unitId = 10L, qty = 12.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(40_000L)
        assertThat(pricedLine.isWholesale).isTrue()
    }

    @Test
    fun selectsLowerWholesaleTierWhenHigherTierIsNotReached() {
        val product = sampleProduct()
        val units = listOf(
            sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 5, wholesalePrice = 42_000L),
            sampleUnit(id = 11L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 10, wholesalePrice = 40_000L),
        )
        val line = sampleLine(unitId = 10L, qty = 7.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(42_000L)
        assertThat(pricedLine.isWholesale).isTrue()
    }

    @Test
    fun wholesaleTierOfAnotherUnitMagnitudeDoesNotApply() {
        val product = sampleProduct()
        val units = listOf(sampleUnit(id = 20L, conversionFactor = 40.0, unitPrice = 170_000L, wholesaleMinQty = 2, wholesalePrice = 160_000L))
        val line = sampleLine(unitId = null, qty = 100.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(5_000L)
        assertThat(pricedLine.isWholesale).isFalse()
    }

    @Test
    fun fallsBackToUnitPriceWhenNoTierIsEligible() {
        val product = sampleProduct()
        val units = listOf(sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 5, wholesalePrice = 42_000L))
        val line = sampleLine(unitId = 10L, qty = 1.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(45_000L)
        assertThat(pricedLine.isWholesale).isFalse()
        assertThat(pricedLine.isNegotiated).isFalse()
    }

    @Test
    fun fallsBackToBasePriceForBaseUnit() {
        val product = sampleProduct(baseHpp = 1_000L, basePrice = 6_000L)
        val line = sampleLine(unitId = null, qty = 3.0)

        val pricedLine = priceEngine.priceLine(line, product, emptyList())

        assertThat(pricedLine.unitPrice).isEqualTo(6_000L)
        assertThat(pricedLine.itemSubtotal).isEqualTo(18_000L)
        assertThat(pricedLine.unitHpp).isEqualTo(1_000L)
    }

    @Test
    fun appliesConversionFactorToUnitHpp() {
        val product = sampleProduct(baseHpp = 1_200L)
        val units = listOf(sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L))
        val line = sampleLine(unitId = 10L, qty = 2.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitHpp).isEqualTo(12_000L)
        assertThat(pricedLine.itemSubtotal).isEqualTo(90_000L)
    }

    @Test
    fun negativeNegotiatedPriceIsIgnored() {
        val product = sampleProduct()
        val units = listOf(sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L, wholesaleMinQty = 5, wholesalePrice = 42_000L))
        val line = sampleLine(unitId = 10L, qty = 5.0, negotiatedUnitPrice = -1L)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(42_000L)
        assertThat(pricedLine.isNegotiated).isFalse()
        assertThat(pricedLine.isWholesale).isTrue()
    }

    @Test
    fun negativeQuantityIsTreatedAsZero() {
        val product = sampleProduct()
        val line = sampleLine(qty = -5.0)

        val pricedLine = priceEngine.priceLine(line, product, emptyList())

        assertThat(pricedLine.itemSubtotal).isEqualTo(0L)
    }

    @Test
    fun negativeItemDiscountIsTreatedAsZero() {
        val product = sampleProduct(basePrice = 5_000L)
        val line = sampleLine(qty = 1.0, itemDiscount = -1_000L)

        val pricedLine = priceEngine.priceLine(line, product, emptyList())

        assertThat(pricedLine.itemSubtotal).isEqualTo(5_000L)
    }

    @Test
    fun itemSubtotalNeverGoesNegative() {
        val product = sampleProduct(basePrice = 5_000L)
        val line = sampleLine(qty = 1.0, itemDiscount = 8_000L)

        val pricedLine = priceEngine.priceLine(line, product, emptyList())

        assertThat(pricedLine.itemSubtotal).isEqualTo(0L)
    }

    @Test
    fun unknownUnitFallsBackToBaseUnitPricing() {
        val product = sampleProduct(basePrice = 5_000L)
        val units = listOf(sampleUnit(id = 10L, conversionFactor = 10.0, unitPrice = 45_000L))
        val line = sampleLine(unitId = 999L, qty = 2.0)

        val pricedLine = priceEngine.priceLine(line, product, units)

        assertThat(pricedLine.unitPrice).isEqualTo(5_000L)
        assertThat(pricedLine.unitHpp).isEqualTo(1_000L)
    }
}
