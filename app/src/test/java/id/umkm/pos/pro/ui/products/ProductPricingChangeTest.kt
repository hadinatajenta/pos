package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.ui.common.IdStrings
import org.junit.Test

class ProductPricingChangeTest {

    private fun editingForm(
        baseHpp: String = "9000",
        basePrice: String = "18000",
        variantPrice: String = "5000",
        wholesalePrice: String = "4500",
        isAdvancedExpanded: Boolean = true,
        variantName: String = "dus",
    ): ProductFormState = ProductFormState(
        productId = 7L,
        name = "Kopi Susu",
        baseUnit = "pcs",
        basePrice = basePrice,
        baseHpp = baseHpp,
        stockQuantity = "10",
        isAdvancedExpanded = isAdvancedExpanded,
        variantName = variantName,
        variantPrice = variantPrice,
        wholesalePrice = wholesalePrice,
        persistedPricing = ProductPricingSnapshot(
            baseHpp = 9000L,
            basePrice = 18000L,
            unitPrice = 5000L,
            wholesalePrice = 4500L,
        ),
    )

    @Test
    fun aNewProductHasNoStoredPricingToProtect() {
        val form = ProductFormState(basePrice = "18000", baseHpp = "9000")

        assertThat(productPricingChange(form)).isEqualTo(ProductPricingChange.NONE)
        assertThat(productPricingChange(form).pinAction()).isNull()
    }

    @Test
    fun anUnchangedFormNeedsNoPin() {
        val change = productPricingChange(editingForm())

        assertThat(change).isEqualTo(ProductPricingChange.NONE)
        assertThat(change.requiresCostPin).isFalse()
        assertThat(change.requiresPricePin).isFalse()
    }

    @Test
    fun aChangedHppAsksForTheOwnerCostPin() {
        val change = productPricingChange(editingForm(baseHpp = "9500"))

        assertThat(change).isEqualTo(ProductPricingChange.COST)
        assertThat(change.requiresCostPin).isTrue()
        assertThat(change.requiresPricePin).isFalse()
        assertThat(change.pinAction()).isEqualTo(CriticalAction.EDIT_PRODUCT_COST)
    }

    @Test
    fun aChangedBasePriceAsksForThePricePin() {
        val change = productPricingChange(editingForm(basePrice = "19000"))

        assertThat(change).isEqualTo(ProductPricingChange.PRICE)
        assertThat(change.pinAction()).isEqualTo(CriticalAction.EDIT_PRODUCT_PRICE)
    }

    @Test
    fun aChangedUnitPriceAsksForThePricePin() {
        val change = productPricingChange(editingForm(variantPrice = "52000"))

        assertThat(change).isEqualTo(ProductPricingChange.PRICE)
        assertThat(change.pinAction()).isEqualTo(CriticalAction.EDIT_PRODUCT_PRICE)
    }

    @Test
    fun aChangedWholesalePriceAsksForThePricePin() {
        val change = productPricingChange(editingForm(wholesalePrice = "44000"))

        assertThat(change).isEqualTo(ProductPricingChange.PRICE)
        assertThat(change.pinAction()).isEqualTo(CriticalAction.EDIT_PRODUCT_PRICE)
    }

    @Test
    fun unitPricesAreIgnoredWhileTheUnitSectionIsClosed() {
        val change = productPricingChange(
            editingForm(variantPrice = "52000", wholesalePrice = "44000", isAdvancedExpanded = false),
        )

        assertThat(change).isEqualTo(ProductPricingChange.NONE)
    }

    @Test
    fun unitPricesAreIgnoredWhenTheUnitNameIsBlank() {
        val change = productPricingChange(
            editingForm(variantPrice = "52000", wholesalePrice = "44000", variantName = "   "),
        )

        assertThat(change).isEqualTo(ProductPricingChange.NONE)
    }

    @Test
    fun changingCostAndPriceTogetherAsksOnceForTheOwnerCostPin() {
        val change = productPricingChange(editingForm(baseHpp = "9500", basePrice = "19000"))

        assertThat(change).isEqualTo(ProductPricingChange.COST_AND_PRICE)
        assertThat(change.requiresCostPin).isTrue()
        assertThat(change.requiresPricePin).isTrue()
        assertThat(change.pinAction()).isEqualTo(CriticalAction.EDIT_PRODUCT_COST)
    }

    @Test
    fun clearingTheStoredHppCountsAsACostChange() {
        val change = productPricingChange(editingForm(baseHpp = ""))

        assertThat(change).isEqualTo(ProductPricingChange.COST)
        assertThat(change.pinAction()).isEqualTo(CriticalAction.EDIT_PRODUCT_COST)
    }

    @Test
    fun aUnitWithoutAStoredPriceAsksForThePricePinWhenOneIsAdded() {
        val form = editingForm().copy(
            variantPrice = "6000",
            persistedPricing = ProductPricingSnapshot(
                baseHpp = 9000L,
                basePrice = 18000L,
                unitPrice = null,
                wholesalePrice = null,
            ),
        )

        assertThat(productPricingChange(form)).isEqualTo(ProductPricingChange.PRICE)
    }

    @Test
    fun theCostPinUsesTheHppLabel() {
        assertThat(pricingPinLabel(CriticalAction.EDIT_PRODUCT_COST, IdStrings))
            .isEqualTo(IdStrings.productsBaseHpp)
        assertThat(pricingPinLabel(CriticalAction.EDIT_PRODUCT_PRICE, IdStrings))
            .isEqualTo(IdStrings.productsBasePrice)
    }
}
