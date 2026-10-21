package id.umkm.pos.pro.ui.cashier

import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.domain.security.PinPolicy
import id.umkm.pos.pro.ui.common.PosStrings

internal enum class LineEditorPinRequest(val action: CriticalAction) {
    ITEM_DISCOUNT(CriticalAction.ITEM_DISCOUNT_ABOVE_THRESHOLD),
    NEGOTIATED_PRICE(CriticalAction.NEGOTIATED_PRICE_BELOW_COST),
}

internal fun LineEditorPinRequest.label(strings: PosStrings): String = when (this) {
    LineEditorPinRequest.ITEM_DISCOUNT -> strings.cashierItemDiscount
    LineEditorPinRequest.NEGOTIATED_PRICE -> strings.cashierNegotiatedPrice
}

internal fun buildLineEditorPricing(
    quantity: Double,
    selectedUnitId: Long?,
    negotiatedUnitPrice: Long?,
    itemDiscount: Long,
    product: Product,
    units: List<ProductUnit>,
    priceEngine: PriceEngine,
): CartLineEditorPricing {
    val preview = priceEngine.priceLine(
        CartLine(
            lineId = PRICING_PREVIEW_LINE_ID,
            productId = product.id,
            unitId = selectedUnitId,
            variantName = null,
            qty = quantity,
            negotiatedUnitPrice = negotiatedUnitPrice,
            itemDiscount = 0L,
        ),
        product,
        units,
    )
    return CartLineEditorPricing(
        unitPrice = preview.unitPrice,
        lineSubtotal = preview.itemSubtotal,
        itemDiscount = itemDiscount.coerceAtLeast(0L),
        unitHpp = preview.unitHpp,
        negotiatedUnitPrice = negotiatedUnitPrice,
    )
}

internal fun resolveLineEditorPinRequest(
    policy: PinPolicy,
    pricing: CartLineEditorPricing,
): LineEditorPinRequest? = when {
    pricing.isBelowCost(policy) -> LineEditorPinRequest.NEGOTIATED_PRICE
    pricing.exceedsDiscountThreshold(policy) -> LineEditorPinRequest.ITEM_DISCOUNT
    else -> null
}

private const val PRICING_PREVIEW_LINE_ID = "editor-preview"
