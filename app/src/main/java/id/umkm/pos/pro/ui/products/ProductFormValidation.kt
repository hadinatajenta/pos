package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.ui.common.UiText

fun productFormErrorSummary(errors: Map<ProductFormField, UiText>): UiText =
    UiText.from { strings -> strings.productsValidationSummary(errors.size) }

fun validateProductForm(form: ProductFormState): Map<ProductFormField, UiText> {
    val errors = mutableMapOf<ProductFormField, UiText>()
    val normalizedName = form.name.trim()
    when {
        normalizedName.isEmpty() ->
            errors[ProductFormField.NAME] = UiText.from { strings -> strings.productsValidationName }

        normalizedName.length > PRODUCT_NAME_MAX_LENGTH ->
            errors[ProductFormField.NAME] = UiText.from { strings -> strings.productsValidationNameTooLong }
    }
    if (form.baseUnit.isBlank()) {
        errors[ProductFormField.BASE_UNIT] = UiText.from { strings -> strings.productsValidationBaseUnit }
    }
    validatePositiveAmount(
        form.basePrice,
        ProductFormField.BASE_PRICE,
        UiText.from { strings -> strings.productsValidationBasePrice },
        errors,
    )
    validateOptionalAmount(
        form.baseHpp,
        ProductFormField.BASE_HPP,
        UiText.from { strings -> strings.productsValidationBaseHpp },
        errors,
    )
    validateQuantity(
        form.stockQuantity,
        ProductFormField.STOCK_QUANTITY,
        UiText.from { strings -> strings.productsValidationStock },
        errors,
    )
    validateOptionalQuantity(
        form.minStockAlert,
        ProductFormField.MIN_STOCK_ALERT,
        UiText.from { strings -> strings.productsValidationMinStockAlert },
        errors,
    )

    if (!form.isAdvancedExpanded) return errors

    val isUnitRequested = form.variantName.isNotBlank() ||
        form.conversionFactor.isNotBlank() ||
        form.variantPrice.isNotBlank()
    if (isUnitRequested) {
        if (form.variantName.isBlank()) {
            errors[ProductFormField.VARIANT_NAME] = UiText.from { strings -> strings.productsValidationUnitName }
        }
        validatePositiveQuantity(
            form.conversionFactor,
            ProductFormField.CONVERSION_FACTOR,
            UiText.from { strings -> strings.productsValidationConversionFactor },
            errors,
        )
        validatePositiveAmount(
            form.variantPrice,
            ProductFormField.VARIANT_PRICE,
            UiText.from { strings -> strings.productsValidationUnitPrice },
            errors,
        )
    }

    val isWholesaleRequested = form.wholesaleMinQty.isNotBlank() || form.wholesalePrice.isNotBlank()
    if (isWholesaleRequested) {
        validatePositiveCount(
            form.wholesaleMinQty,
            ProductFormField.WHOLESALE_MIN_QTY,
            UiText.from { strings -> strings.productsValidationWholesaleMinQty },
            errors,
        )
        validatePositiveAmount(
            form.wholesalePrice,
            ProductFormField.WHOLESALE_PRICE,
            UiText.from { strings -> strings.productsValidationWholesalePrice },
            errors,
        )
    }
    return errors
}

private fun validatePositiveAmount(
    value: String,
    field: ProductFormField,
    message: UiText,
    errors: MutableMap<ProductFormField, UiText>,
) {
    val parsed = value.trim().toLongOrNull()
    if (parsed == null || parsed <= 0L) {
        errors[field] = message
    }
}

private fun validateOptionalAmount(
    value: String,
    field: ProductFormField,
    message: UiText,
    errors: MutableMap<ProductFormField, UiText>,
) {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return
    val parsed = trimmed.toLongOrNull()
    if (parsed == null || parsed < 0L) {
        errors[field] = message
    }
}

private fun validateQuantity(
    value: String,
    field: ProductFormField,
    message: UiText,
    errors: MutableMap<ProductFormField, UiText>,
) {
    val parsed = value.trim().toDoubleOrNull()
    if (parsed == null || parsed < 0.0) {
        errors[field] = message
    }
}

private fun validateOptionalQuantity(
    value: String,
    field: ProductFormField,
    message: UiText,
    errors: MutableMap<ProductFormField, UiText>,
) {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return
    val parsed = trimmed.toDoubleOrNull()
    if (parsed == null || parsed < 0.0) {
        errors[field] = message
    }
}

private fun validatePositiveQuantity(
    value: String,
    field: ProductFormField,
    message: UiText,
    errors: MutableMap<ProductFormField, UiText>,
) {
    val parsed = value.trim().toDoubleOrNull()
    if (parsed == null || parsed <= 0.0) {
        errors[field] = message
    }
}

private fun validatePositiveCount(
    value: String,
    field: ProductFormField,
    message: UiText,
    errors: MutableMap<ProductFormField, UiText>,
) {
    val parsed = value.trim().toIntOrNull()
    if (parsed == null || parsed < 1) {
        errors[field] = message
    }
}
