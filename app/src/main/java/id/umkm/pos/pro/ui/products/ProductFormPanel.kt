package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.SectionHeading
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun ProductFormPanel(
    form: ProductFormState,
    isSaving: Boolean,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (form.isEditing) "Edit product" else "New product",
                    style = LocalAppTextStyles.current.screenTitle,
                )
                Text(
                    text = "Basics, pricing and units, stock alerts",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = actions.onDismissForm) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close product form")
            }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            BasicsSection(form = form, actions = actions)
            PricingSection(form = form, actions = actions)
            StockSection(form = form, actions = actions)
        }
        HorizontalDivider()
        Button(
            onClick = actions.onSubmitForm,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(PosSpacing.md)
                .heightIn(min = PosSizes.touchTarget),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PosSizes.iconSmall),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (form.isEditing) "Save changes" else "Save product",
                    style = LocalAppTextStyles.current.buttonLabel,
                )
            }
        }
    }
}

@Composable
private fun BasicsSection(
    form: ProductFormState,
    actions: ProductsActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        SectionHeading(text = "Basics")
        FormTextField(
            label = "Name",
            value = form.name,
            onValueChange = { actions.onFormFieldChange(ProductFormField.NAME, it) },
            error = form.errorFor(ProductFormField.NAME),
        )
        FormTextField(
            label = "SKU",
            value = form.sku,
            onValueChange = { actions.onFormFieldChange(ProductFormField.SKU, it) },
            error = form.errorFor(ProductFormField.SKU),
            supportingText = "Generated automatically when empty.",
        )
        FormTextField(
            label = "Barcode",
            value = form.barcode,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BARCODE, it) },
            error = form.errorFor(ProductFormField.BARCODE),
        )
        FormTextField(
            label = "Category",
            value = form.category,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CATEGORY, it) },
            error = form.errorFor(ProductFormField.CATEGORY),
        )
        FormTextField(
            label = "Base unit",
            value = form.baseUnit,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_UNIT, it) },
            error = form.errorFor(ProductFormField.BASE_UNIT),
            supportingText = "For example pcs, kg, or liter.",
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Active", style = LocalAppTextStyles.current.body)
                Text(
                    text = "Inactive products stay out of the cashier list.",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = form.isActive, onCheckedChange = actions.onFormActiveChange)
        }
    }
}

@Composable
private fun PricingSection(
    form: ProductFormState,
    actions: ProductsActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        SectionHeading(text = "Pricing and units")
        FormTextField(
            label = "Selling price",
            value = form.basePrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_PRICE, it) },
            error = form.errorFor(ProductFormField.BASE_PRICE),
            keyboardType = KeyboardType.Number,
        )
        FormTextField(
            label = "Cost of goods",
            value = form.baseHpp,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_HPP, it) },
            error = form.errorFor(ProductFormField.BASE_HPP),
            keyboardType = KeyboardType.Number,
        )
        TextButton(onClick = actions.onFormAdvancedToggle) {
            Icon(
                imageVector = if (form.isAdvancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(PosSpacing.xs))
            Text(text = "More options")
        }
        if (form.isAdvancedExpanded) {
            AdvancedUnitFields(form = form, actions = actions)
        }
    }
}

@Composable
private fun AdvancedUnitFields(
    form: ProductFormState,
    actions: ProductsActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        FormTextField(
            label = "Unit name",
            value = form.variantName,
            onValueChange = { actions.onFormFieldChange(ProductFormField.VARIANT_NAME, it) },
            error = form.errorFor(ProductFormField.VARIANT_NAME),
            supportingText = "For example bundle or carton.",
        )
        FormTextField(
            label = "Conversion factor",
            value = form.conversionFactor,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CONVERSION_FACTOR, it) },
            error = form.errorFor(ProductFormField.CONVERSION_FACTOR),
            keyboardType = KeyboardType.Decimal,
            supportingText = "Base units per unit, for example 10.",
        )
        FormTextField(
            label = "Unit price",
            value = form.variantPrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.VARIANT_PRICE, it) },
            error = form.errorFor(ProductFormField.VARIANT_PRICE),
            keyboardType = KeyboardType.Number,
        )
        FormTextField(
            label = "Wholesale minimum quantity",
            value = form.wholesaleMinQty,
            onValueChange = { actions.onFormFieldChange(ProductFormField.WHOLESALE_MIN_QTY, it) },
            error = form.errorFor(ProductFormField.WHOLESALE_MIN_QTY),
            keyboardType = KeyboardType.Number,
        )
        FormTextField(
            label = "Wholesale price",
            value = form.wholesalePrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.WHOLESALE_PRICE, it) },
            error = form.errorFor(ProductFormField.WHOLESALE_PRICE),
            keyboardType = KeyboardType.Number,
        )
    }
}

@Composable
private fun StockSection(
    form: ProductFormState,
    actions: ProductsActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        SectionHeading(text = "Stock alerts")
        FormTextField(
            label = "Stock quantity",
            value = form.stockQuantity,
            onValueChange = { actions.onFormFieldChange(ProductFormField.STOCK_QUANTITY, it) },
            error = form.errorFor(ProductFormField.STOCK_QUANTITY),
            keyboardType = KeyboardType.Decimal,
        )
        FormTextField(
            label = "Low stock alert at",
            value = form.minStockAlert,
            onValueChange = { actions.onFormFieldChange(ProductFormField.MIN_STOCK_ALERT, it) },
            error = form.errorFor(ProductFormField.MIN_STOCK_ALERT),
            keyboardType = KeyboardType.Decimal,
            supportingText = "Shown as a low stock chip when reached.",
        )
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
) {
    val helperText = error ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = error != null,
        supportingText = helperText?.let { text -> { Text(text) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
