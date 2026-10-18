package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.IdStrings
import org.junit.Test

class ProductFormValidationTest {

    private fun validForm(): ProductFormState = ProductFormState(
        name = "Kopi Susu",
        sku = "SKU-1",
        baseUnit = "pcs",
        basePrice = "18000",
        baseHpp = "9000",
        stockQuantity = "12",
        minStockAlert = "3",
    )

    @Test
    fun acceptsACompleteForm() {
        assertThat(validateProductForm(validForm())).isEmpty()
    }

    @Test
    fun rejectsAMissingName() {
        val errors = validateProductForm(validForm().copy(name = " "))

        assertThat(errors).containsKey(ProductFormField.NAME)
        assertThat(errors[ProductFormField.NAME]?.resolve(IdStrings)).isEqualTo("Masukkan nama produk.")
    }

    @Test
    fun rejectsNegativeAmounts() {
        val errors = validateProductForm(validForm().copy(basePrice = "-1"))

        assertThat(errors).containsKey(ProductFormField.BASE_PRICE)
        assertThat(errors[ProductFormField.BASE_PRICE]?.resolve(IdStrings))
            .isEqualTo("Masukkan harga jual yang valid.")
    }

    @Test
    fun rejectsUnparsableQuantities() {
        val errors = validateProductForm(validForm().copy(stockQuantity = "abc"))

        assertThat(errors).containsKey(ProductFormField.STOCK_QUANTITY)
    }

    @Test
    fun ignoresAdvancedFieldsWhileCollapsed() {
        val collapsed = validForm().copy(variantName = "Dus", conversionFactor = "", variantPrice = "")

        assertThat(validateProductForm(collapsed)).isEmpty()
    }

    @Test
    fun requiresAConversionFactorWhenAVariantIsNamed() {
        val expanded = validForm().copy(
            isAdvancedExpanded = true,
            variantName = "Dus",
            conversionFactor = "0",
            variantPrice = "78000",
        )

        val errors = validateProductForm(expanded)

        assertThat(errors).containsKey(ProductFormField.CONVERSION_FACTOR)
        assertThat(errors).doesNotContainKey(ProductFormField.VARIANT_PRICE)
    }

    @Test
    fun rejectsAWholesaleMinimumBelowOne() {
        val expanded = validForm().copy(
            isAdvancedExpanded = true,
            wholesaleMinQty = "0",
            wholesalePrice = "75000",
        )

        val errors = validateProductForm(expanded)

        assertThat(errors).containsKey(ProductFormField.WHOLESALE_MIN_QTY)
    }

    @Test
    fun rejectsAWholesalePriceWithoutAMinimumQuantity() {
        val expanded = validForm().copy(
            isAdvancedExpanded = true,
            wholesaleMinQty = "",
            wholesalePrice = "abc",
        )

        val errors = validateProductForm(expanded)

        assertThat(errors).containsKey(ProductFormField.WHOLESALE_PRICE)
    }

    @Test
    fun summarisesHowManyFieldsBlockSaving() {
        val errors = validateProductForm(validForm().copy(name = " ", basePrice = ""))

        assertThat(errors.keys).containsExactly(ProductFormField.NAME, ProductFormField.BASE_PRICE)
        assertThat(productFormErrorSummary(errors).resolve(IdStrings))
            .isEqualTo("Lengkapi 2 kolom wajib sebelum menyimpan.")
    }

    @Test
    fun summarisesASingleBlockingField() {
        val errors = validateProductForm(validForm().copy(name = ""))

        assertThat(productFormErrorSummary(errors).resolve(IdStrings))
            .isEqualTo("Lengkapi 1 kolom wajib sebelum menyimpan.")
    }
}
