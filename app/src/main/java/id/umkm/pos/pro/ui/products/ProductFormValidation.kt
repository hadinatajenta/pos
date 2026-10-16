package id.umkm.pos.pro.ui.products

fun validateProductForm(form: ProductFormState): Map<ProductFormField, String> {
    val errors = mutableMapOf<ProductFormField, String>()
    if (form.name.isBlank()) {
        errors[ProductFormField.NAME] = "Enter a product name."
    }
    if (form.baseUnit.isBlank()) {
        errors[ProductFormField.BASE_UNIT] = "Enter the base unit."
    }
    validateAmount(form.basePrice, ProductFormField.BASE_PRICE, "Enter a valid selling price.", errors)
    validateAmount(form.baseHpp, ProductFormField.BASE_HPP, "Enter a valid cost price.", errors)
    validateQuantity(form.stockQuantity, ProductFormField.STOCK_QUANTITY, "Enter a valid stock quantity.", errors)
    validateQuantity(form.minStockAlert, ProductFormField.MIN_STOCK_ALERT, "Enter a valid alert quantity.", errors)

    if (!form.isAdvancedExpanded) return errors

    val isUnitRequested = form.variantName.isNotBlank() ||
        form.conversionFactor.isNotBlank() ||
        form.variantPrice.isNotBlank()
    if (isUnitRequested) {
        if (form.variantName.isBlank()) {
            errors[ProductFormField.VARIANT_NAME] = "Enter a unit name."
        }
        validatePositiveQuantity(
            form.conversionFactor,
            ProductFormField.CONVERSION_FACTOR,
            "Enter a conversion factor greater than zero.",
            errors,
        )
        validateAmount(form.variantPrice, ProductFormField.VARIANT_PRICE, "Enter a valid unit price.", errors)
    }

    val isWholesaleRequested = form.wholesaleMinQty.isNotBlank() || form.wholesalePrice.isNotBlank()
    if (isWholesaleRequested) {
        validatePositiveCount(
            form.wholesaleMinQty,
            ProductFormField.WHOLESALE_MIN_QTY,
            "Enter a minimum quantity of at least 1.",
            errors,
        )
        validateAmount(form.wholesalePrice, ProductFormField.WHOLESALE_PRICE, "Enter a valid wholesale price.", errors)
    }
    return errors
}

private fun validateAmount(
    value: String,
    field: ProductFormField,
    message: String,
    errors: MutableMap<ProductFormField, String>,
) {
    val parsed = value.trim().toLongOrNull()
    if (parsed == null || parsed < 0L) {
        errors[field] = message
    }
}

private fun validateQuantity(
    value: String,
    field: ProductFormField,
    message: String,
    errors: MutableMap<ProductFormField, String>,
) {
    val parsed = value.trim().toDoubleOrNull()
    if (parsed == null || parsed < 0.0) {
        errors[field] = message
    }
}

private fun validatePositiveQuantity(
    value: String,
    field: ProductFormField,
    message: String,
    errors: MutableMap<ProductFormField, String>,
) {
    val parsed = value.trim().toDoubleOrNull()
    if (parsed == null || parsed <= 0.0) {
        errors[field] = message
    }
}

private fun validatePositiveCount(
    value: String,
    field: ProductFormField,
    message: String,
    errors: MutableMap<ProductFormField, String>,
) {
    val parsed = value.trim().toIntOrNull()
    if (parsed == null || parsed < 1) {
        errors[field] = message
    }
}
