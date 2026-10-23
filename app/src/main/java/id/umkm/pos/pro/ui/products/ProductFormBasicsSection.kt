package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosColors

private const val NAME_COUNTER_VISIBLE_LENGTH = 30

private val COMMON_BASE_UNITS = listOf(
    "pcs",
    "bks",
    "butir",
    "kg",
    "g",
    "liter",
    "ml",
    "cup",
    "dus",
    "pack",
    "botol",
    "sachet",
)

@Composable
internal fun ProductBasicsSection(
    form: ProductFormState,
    categorySuggestions: List<String>,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
    isSaving: Boolean,
) {
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProductTextField(
                label = strings.productsFieldName,
                value = form.name,
                onValueChange = { value ->
                    actions.onFormFieldChange(ProductFormField.NAME, value.take(PRODUCT_NAME_MAX_LENGTH))
                },
                error = form.errorFor(ProductFormField.NAME),
                isRequired = true,
                enabled = !isSaving,
                modifier = fieldFocus.modifierFor(ProductFormField.NAME),
            )
            if (form.name.length >= NAME_COUNTER_VISIBLE_LENGTH) {
                Text(
                    text = "${form.name.length}/$PRODUCT_NAME_MAX_LENGTH",
                    modifier = Modifier.fillMaxWidth(),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = if (form.name.length >= PRODUCT_NAME_MAX_LENGTH) {
                        LocalPosSemanticPalette.current.warningText
                    } else {
                        PosColors.TextSecondary
                    },
                    textAlign = TextAlign.End,
                )
            }
            if (form.duplicateNameWarning) {
                ProductInlineWarning(text = strings.productsDuplicateNameWarning)
            }
        }
        CategoryPicker(
            productId = form.productId,
            value = form.category,
            suggestions = categorySuggestions,
            error = form.errorFor(ProductFormField.CATEGORY),
            onValueChange = { value -> actions.onFormFieldChange(ProductFormField.CATEGORY, value) },
            fieldFocus = fieldFocus,
            enabled = !isSaving,
        )
        BaseUnitPicker(
            value = form.baseUnit,
            error = form.errorFor(ProductFormField.BASE_UNIT),
            onValueChange = { value -> actions.onFormFieldChange(ProductFormField.BASE_UNIT, value) },
            modifier = fieldFocus.modifierFor(ProductFormField.BASE_UNIT),
            enabled = !isSaving,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(
    productId: Long?,
    value: String,
    suggestions: List<String>,
    error: UiText?,
    onValueChange: (String) -> Unit,
    fieldFocus: FormFieldFocus,
    enabled: Boolean,
) {
    val strings = LocalPosStrings.current
    val normalizedValue = value.trim()
    val normalizedSuggestions = remember(suggestions) {
        suggestions
            .map { suggestion -> suggestion.trim() }
            .filter { suggestion -> suggestion.isNotEmpty() }
            .distinctBy { suggestion -> suggestion.lowercase() }
    }
    val isCustomValue = normalizedValue.isNotEmpty() &&
        normalizedSuggestions.none { suggestion -> suggestion.equals(normalizedValue, ignoreCase = true) }
    var isNewCategoryVisible by remember(productId) { mutableStateOf(false) }
    var shouldFocusCategory by remember(productId) { mutableStateOf(false) }
    LaunchedEffect(shouldFocusCategory) {
        if (!shouldFocusCategory) return@LaunchedEffect
        fieldFocus.focusAndReveal(ProductFormField.CATEGORY)
        shouldFocusCategory = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProductFieldLabel(text = strings.productsCategory)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ProductSelectableChip(
                label = strings.commonNoCategory,
                selected = normalizedValue.isEmpty() && !isNewCategoryVisible,
                compact = true,
                enabled = enabled,
                onClick = {
                    isNewCategoryVisible = false
                    onValueChange("")
                },
            )
            normalizedSuggestions.forEach { suggestion ->
                ProductSelectableChip(
                    label = suggestion,
                    selected = !isNewCategoryVisible &&
                        suggestion.equals(normalizedValue, ignoreCase = true),
                    compact = true,
                    enabled = enabled,
                    onClick = {
                        isNewCategoryVisible = false
                        onValueChange(suggestion)
                    },
                )
            }
            if (isCustomValue && !isNewCategoryVisible) {
                ProductSelectableChip(
                    label = normalizedValue,
                    selected = true,
                    compact = true,
                    enabled = enabled,
                    onClick = {},
                )
            }
            ProductSelectableChip(
                label = strings.productsNewCategory,
                selected = false,
                accent = true,
                action = true,
                compact = true,
                dashedBorder = true,
                enabled = enabled,
                onClick = {
                    onValueChange("")
                    isNewCategoryVisible = true
                    shouldFocusCategory = true
                },
            )
        }
        if (isNewCategoryVisible) {
            ProductTextField(
                label = strings.productsCategory,
                value = value,
                onValueChange = onValueChange,
                error = error,
                isRequired = false,
                enabled = enabled,
                placeholder = strings.productsNewCategoryHint,
                modifier = fieldFocus.modifierFor(ProductFormField.CATEGORY),
            )
            TextButton(
                onClick = { isNewCategoryVisible = false },
                enabled = enabled,
                modifier = Modifier.heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    text = strings.productsCategoryPickExisting,
                    style = LocalAppTextStyles.current.buttonLabel,
                )
            }
        }
    }
}

@Composable
private fun BaseUnitPicker(
    value: String,
    error: UiText?,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val strings = LocalPosStrings.current
    val options = remember(value) {
        buildList {
            addAll(COMMON_BASE_UNITS)
            value.trim().takeIf { unit -> unit.isNotEmpty() }?.let { unit -> add(unit) }
        }.distinctBy { unit -> unit.lowercase() }
    }
    ProductDropdownField(
        label = strings.productsFieldBaseUnit,
        value = value.trim(),
        options = options,
        error = error,
        onSelect = onValueChange,
        modifier = modifier,
        enabled = enabled,
        supportingText = strings.productsBaseUnitHint,
    )
}
