package id.umkm.pos.pro.ui.products

fun validateProductForm(form: ProductFormState): Map<ProductFormField, String> {
    val errors = mutableMapOf<ProductFormField, String>()
    if (form.name.isBlank()) {
        errors[ProductFormField.NAME] = "Masukkan nama produk."
    }
    if (form.baseUnit.isBlank()) {
        errors[ProductFormField.BASE_UNIT] = "Masukkan satuan dasar."
    }
    validateAmount(form.basePrice, ProductFormField.BASE_PRICE, "Masukkan harga jual yang valid.", errors)
    validateAmount(form.baseHpp, ProductFormField.BASE_HPP, "Masukkan harga pokok yang valid.", errors)
    validateQuantity(form.stockQuantity, ProductFormField.STOCK_QUANTITY, "Masukkan jumlah stok yang valid.", errors)
    validateQuantity(
        form.minStockAlert,
        ProductFormField.MIN_STOCK_ALERT,
        "Masukkan jumlah peringatan yang valid.",
        errors,
    )

    if (!form.isAdvancedExpanded) return errors

    val isUnitRequested = form.variantName.isNotBlank() ||
        form.conversionFactor.isNotBlank() ||
        form.variantPrice.isNotBlank()
    if (isUnitRequested) {
        if (form.variantName.isBlank()) {
            errors[ProductFormField.VARIANT_NAME] = "Masukkan nama satuan."
        }
        validatePositiveQuantity(
            form.conversionFactor,
            ProductFormField.CONVERSION_FACTOR,
            "Masukkan faktor konversi lebih dari nol.",
            errors,
        )
        validateAmount(form.variantPrice, ProductFormField.VARIANT_PRICE, "Masukkan harga satuan yang valid.", errors)
    }

    val isWholesaleRequested = form.wholesaleMinQty.isNotBlank() || form.wholesalePrice.isNotBlank()
    if (isWholesaleRequested) {
        validatePositiveCount(
            form.wholesaleMinQty,
            ProductFormField.WHOLESALE_MIN_QTY,
            "Masukkan minimal grosir paling sedikit 1.",
            errors,
        )
        validateAmount(
            form.wholesalePrice,
            ProductFormField.WHOLESALE_PRICE,
            "Masukkan harga grosir yang valid.",
            errors,
        )
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
