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
    fun defaultsTheLowStockThresholdToSix() {
        assertThat(ProductFormState().minStockAlert).isEqualTo("6")
    }

    @Test
    fun acceptsAnEmptyLowStockThreshold() {
        val errors = validateProductForm(validForm().copy(minStockAlert = "  "))

        assertThat(errors).doesNotContainKey(ProductFormField.MIN_STOCK_ALERT)
    }

    @Test
    fun acceptsAZeroLowStockThreshold() {
        val errors = validateProductForm(validForm().copy(minStockAlert = "0"))

        assertThat(errors).doesNotContainKey(ProductFormField.MIN_STOCK_ALERT)
    }

    @Test
    fun rejectsANegativeLowStockThreshold() {
        val errors = validateProductForm(validForm().copy(minStockAlert = "-1"))

        assertThat(errors[ProductFormField.MIN_STOCK_ALERT]?.resolve(IdStrings))
            .isEqualTo("Masukkan jumlah peringatan yang valid.")
    }

    @Test
    fun rejectsAMissingName() {
        val errors = validateProductForm(validForm().copy(name = " "))

        assertThat(errors[ProductFormField.NAME]?.resolve(IdStrings))
            .isEqualTo("Masukkan nama produk.")
    }

    @Test
    fun acceptsANameAtTheMaximumLength() {
        val errors = validateProductForm(validForm().copy(name = "a".repeat(PRODUCT_NAME_MAX_LENGTH)))

        assertThat(errors).doesNotContainKey(ProductFormField.NAME)
    }

    @Test
    fun rejectsANameLongerThanTheMaximumLength() {
        val errors = validateProductForm(validForm().copy(name = "a".repeat(PRODUCT_NAME_MAX_LENGTH + 1)))

        assertThat(errors[ProductFormField.NAME]?.resolve(IdStrings))
            .isEqualTo("Nama produk maksimal 40 karakter.")
    }

    @Test
    fun rejectsAZeroSellingPrice() {
        val errors = validateProductForm(validForm().copy(basePrice = "0"))

        assertThat(errors[ProductFormField.BASE_PRICE]?.resolve(IdStrings))
            .isEqualTo("Harga jual harus lebih dari nol.")
    }

    @Test
    fun rejectsANegativeSellingPrice() {
        val errors = validateProductForm(validForm().copy(basePrice = "-1"))

        assertThat(errors[ProductFormField.BASE_PRICE]?.resolve(IdStrings))
            .isEqualTo("Harga jual harus lebih dari nol.")
    }

    @Test
    fun rejectsAnUnparsableSellingPrice() {
        val errors = validateProductForm(validForm().copy(basePrice = "abc"))

        assertThat(errors[ProductFormField.BASE_PRICE]?.resolve(IdStrings))
            .isEqualTo("Harga jual harus lebih dari nol.")
    }

    @Test
    fun acceptsABlankCost() {
        val errors = validateProductForm(validForm().copy(baseHpp = " "))

        assertThat(errors).doesNotContainKey(ProductFormField.BASE_HPP)
    }

    @Test
    fun acceptsAZeroCost() {
        val errors = validateProductForm(validForm().copy(baseHpp = "0"))

        assertThat(errors).doesNotContainKey(ProductFormField.BASE_HPP)
    }

    @Test
    fun rejectsANegativeCost() {
        val errors = validateProductForm(validForm().copy(baseHpp = "-1"))

        assertThat(errors[ProductFormField.BASE_HPP]?.resolve(IdStrings))
            .isEqualTo("Masukkan harga modal yang valid.")
    }

    @Test
    fun rejectsAnUnparsableCost() {
        val errors = validateProductForm(validForm().copy(baseHpp = "abc"))

        assertThat(errors[ProductFormField.BASE_HPP]?.resolve(IdStrings))
            .isEqualTo("Masukkan harga modal yang valid.")
    }

    @Test
    fun acceptsZeroStock() {
        val errors = validateProductForm(validForm().copy(stockQuantity = "0"))

        assertThat(errors).doesNotContainKey(ProductFormField.STOCK_QUANTITY)
    }

    @Test
    fun rejectsNegativeStock() {
        val errors = validateProductForm(validForm().copy(stockQuantity = "-1"))

        assertThat(errors[ProductFormField.STOCK_QUANTITY]?.resolve(IdStrings))
            .isEqualTo("Masukkan jumlah stok yang valid.")
    }

    @Test
    fun rejectsUnparsableQuantities() {
        val errors = validateProductForm(validForm().copy(stockQuantity = "abc"))

        assertThat(errors[ProductFormField.STOCK_QUANTITY]?.resolve(IdStrings))
            .isEqualTo("Masukkan jumlah stok yang valid.")
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
