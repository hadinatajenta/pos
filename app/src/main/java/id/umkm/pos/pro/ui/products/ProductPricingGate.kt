package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.ui.common.PosStrings

data class ProductPricingSnapshot(
    val baseHpp: Long,
    val basePrice: Long,
    val unitPrice: Long? = null,
    val wholesalePrice: Long? = null,
)

enum class ProductPricingChange {
    NONE,
    COST,
    PRICE,
    COST_AND_PRICE,
    ;

    val requiresCostPin: Boolean get() = this == COST || this == COST_AND_PRICE

    val requiresPricePin: Boolean get() = this == PRICE || this == COST_AND_PRICE

    fun pinAction(): CriticalAction? = when (this) {
        NONE -> null
        PRICE -> CriticalAction.EDIT_PRODUCT_PRICE
        COST, COST_AND_PRICE -> CriticalAction.EDIT_PRODUCT_COST
    }
}

fun productPricingChange(form: ProductFormState): ProductPricingChange {
    val persisted = form.persistedPricing ?: return ProductPricingChange.NONE
    val costChanged = form.baseHpp.trim().toLongOrNull() != persisted.baseHpp
    val priceChanged = form.hasBasePriceChange(persisted) || form.hasUnitPriceChange(persisted)
    return when {
        costChanged && priceChanged -> ProductPricingChange.COST_AND_PRICE
        costChanged -> ProductPricingChange.COST
        priceChanged -> ProductPricingChange.PRICE
        else -> ProductPricingChange.NONE
    }
}

fun pricingPinLabel(action: CriticalAction, strings: PosStrings): String =
    if (action == CriticalAction.EDIT_PRODUCT_PRICE) strings.productsBasePrice else strings.productsBaseHpp

private fun ProductFormState.hasBasePriceChange(persisted: ProductPricingSnapshot): Boolean =
    basePrice.trim().toLongOrNull() != persisted.basePrice

private fun ProductFormState.hasUnitPriceChange(persisted: ProductPricingSnapshot): Boolean {
    if (!isAdvancedExpanded || variantName.isBlank()) return false
    return variantPrice.trim().toLongOrNull() != persisted.unitPrice ||
        wholesalePrice.trim().toLongOrNull() != persisted.wholesalePrice
}
