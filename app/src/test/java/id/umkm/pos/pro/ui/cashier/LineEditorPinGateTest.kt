package id.umkm.pos.pro.ui.cashier

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.security.PinPolicy
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.security.OpenSecurityGate
import id.umkm.pos.pro.ui.security.SecurityGate
import id.umkm.pos.pro.ui.uiSampleProduct
import org.junit.Test

class LineEditorPinGateTest {

    private val policy = PinPolicy()
    private val priceEngine = DefaultPriceEngine()
    private val product = uiSampleProduct(id = 1L, basePrice = 10_000L, baseHpp = 6_000L)

    private val carton = ProductUnit(
        id = 11L,
        productId = 1L,
        variantName = "Dus",
        conversionFactor = 10.0,
        customBarcode = null,
        unitPrice = 95_000L,
        wholesaleMinQty = null,
        wholesalePrice = null,
    )

    private fun pricing(
        quantity: Double = 2.0,
        selectedUnitId: Long? = null,
        negotiatedUnitPrice: Long? = null,
        itemDiscount: Long = 0L,
    ): CartLineEditorPricing = buildLineEditorPricing(
        quantity = quantity,
        selectedUnitId = selectedUnitId,
        negotiatedUnitPrice = negotiatedUnitPrice,
        itemDiscount = itemDiscount,
        product = product,
        units = listOf(carton),
        priceEngine = priceEngine,
    )

    @Test
    fun plainLineWithoutDiscountOrNegotiationNeedsNoPin() {
        assertThat(resolveLineEditorPinRequest(policy, pricing())).isNull()
    }

    @Test
    fun subtotalComesFromQuantityAndSelectedPriceBeforeDiscount() {
        val withoutDiscount = pricing(itemDiscount = 0L)

        assertThat(withoutDiscount.unitPrice).isEqualTo(10_000L)
        assertThat(withoutDiscount.lineSubtotal).isEqualTo(20_000L)
        assertThat(withoutDiscount.itemDiscount).isEqualTo(0L)
    }

    @Test
    fun discountAboveThePercentageThresholdAsksForTheItemDiscountPin() {
        val assessed = pricing(itemDiscount = 3_000L)

        assertThat(resolveLineEditorPinRequest(policy, assessed))
            .isEqualTo(LineEditorPinRequest.ITEM_DISCOUNT)
        assertThat(LineEditorPinRequest.ITEM_DISCOUNT.action)
            .isEqualTo(CriticalAction.ITEM_DISCOUNT_ABOVE_THRESHOLD)
    }

    @Test
    fun discountAboveTheNominalThresholdAsksForTheItemDiscountPin() {
        val bigLine = pricing(quantity = 20.0, itemDiscount = 25_000L)

        assertThat(resolveLineEditorPinRequest(policy, bigLine))
            .isEqualTo(LineEditorPinRequest.ITEM_DISCOUNT)
    }

    @Test
    fun discountAtTheThresholdNeedsNoPin() {
        val assessed = pricing(itemDiscount = 2_000L)

        assertThat(resolveLineEditorPinRequest(policy, assessed)).isNull()
    }

    @Test
    fun negotiatedPriceBelowCostAsksForTheOwnerLevelPin() {
        val assessed = pricing(negotiatedUnitPrice = 5_000L)

        assertThat(resolveLineEditorPinRequest(policy, assessed))
            .isEqualTo(LineEditorPinRequest.NEGOTIATED_PRICE)
        assertThat(LineEditorPinRequest.NEGOTIATED_PRICE.action)
            .isEqualTo(CriticalAction.NEGOTIATED_PRICE_BELOW_COST)
        assertThat(policy.alwaysPrompts(LineEditorPinRequest.NEGOTIATED_PRICE.action)).isTrue()
    }

    @Test
    fun negotiatedPriceAtOrAboveCostNeedsNoPin() {
        assertThat(resolveLineEditorPinRequest(policy, pricing(negotiatedUnitPrice = 6_000L))).isNull()
        assertThat(resolveLineEditorPinRequest(policy, pricing(negotiatedUnitPrice = 9_000L))).isNull()
    }

    @Test
    fun discountAndBelowCostTogetherAskOnlyOnceWithTheOwnerLevelAction() {
        val assessed = pricing(negotiatedUnitPrice = 5_000L, itemDiscount = 4_000L)

        val request = resolveLineEditorPinRequest(policy, assessed)

        assertThat(request).isEqualTo(LineEditorPinRequest.NEGOTIATED_PRICE)
        assertThat(request?.action).isEqualTo(CriticalAction.NEGOTIATED_PRICE_BELOW_COST)
    }

    @Test
    fun variantUnitComparesTheNegotiatedPriceWithTheUnitCost() {
        val assessed = pricing(selectedUnitId = carton.id, negotiatedUnitPrice = 55_000L)

        assertThat(assessed.unitPrice).isEqualTo(55_000L)
        assertThat(assessed.lineSubtotal).isEqualTo(110_000L)
        assertThat(assessed.unitHpp).isEqualTo(60_000L)
        assertThat(resolveLineEditorPinRequest(policy, assessed))
            .isEqualTo(LineEditorPinRequest.NEGOTIATED_PRICE)
    }

    @Test
    fun labelUsesTheExistingCashierStrings() {
        assertThat(LineEditorPinRequest.ITEM_DISCOUNT.label(IdStrings))
            .isEqualTo(IdStrings.cashierItemDiscount)
        assertThat(LineEditorPinRequest.NEGOTIATED_PRICE.label(IdStrings))
            .isEqualTo(IdStrings.cashierNegotiatedPrice)
        assertThat(LineEditorPinRequest.ITEM_DISCOUNT.label(EnStrings))
            .isEqualTo(EnStrings.cashierItemDiscount)
    }

    @Test
    fun openGateAppliesTheLineWithoutAskingForAPin() {
        val gate: SecurityGate = OpenSecurityGate
        val assessed = pricing(negotiatedUnitPrice = 5_000L, itemDiscount = 4_000L)
        val request = requireNotNull(resolveLineEditorPinRequest(gate.policy(), assessed))
        var applied = false

        gate.request(request.action, request.label(IdStrings)) { applied = true }

        assertThat(gate.isUnlocked(request.action)).isTrue()
        assertThat(applied).isTrue()
    }
}
