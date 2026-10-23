package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.scanner.BarcodeScanDialog
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors

@Composable
internal fun ProductStockSection(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
    isSaving: Boolean,
) {
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        QuantityStepper(
            label = strings.productsStockQty,
            value = form.stockQuantity,
            onValueChange = { value ->
                actions.onFormFieldChange(ProductFormField.STOCK_QUANTITY, value)
            },
            error = form.errorFor(ProductFormField.STOCK_QUANTITY),
            decreaseDescription = strings.productsStockStepDecrease,
            increaseDescription = strings.productsStockStepIncrease,
            enabled = !isSaving,
            modifier = fieldFocus.modifierFor(ProductFormField.STOCK_QUANTITY),
        )
        QuantityStepper(
            label = strings.productsMinStockAlert,
            value = form.minStockAlert,
            onValueChange = { value ->
                actions.onFormFieldChange(ProductFormField.MIN_STOCK_ALERT, value)
            },
            error = form.errorFor(ProductFormField.MIN_STOCK_ALERT),
            decreaseDescription = strings.productsThresholdStepDecrease,
            increaseDescription = strings.productsThresholdStepIncrease,
            enabled = !isSaving,
            modifier = fieldFocus.modifierFor(ProductFormField.MIN_STOCK_ALERT),
        )
    }
}

@Composable
internal fun ProductOtherOptionsSection(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
    isSaving: Boolean,
) {
    val strings = LocalPosStrings.current
    var isBarcodeScanOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProductTextField(
            label = strings.productsSku,
            value = form.sku,
            onValueChange = { value -> actions.onFormFieldChange(ProductFormField.SKU, value) },
            error = form.errorFor(ProductFormField.SKU),
            isRequired = false,
            enabled = !isSaving,
            supportingText = if (form.sku.isBlank()) strings.productsSkuAutoHint else null,
            modifier = fieldFocus.modifierFor(ProductFormField.SKU),
        )
        ProductTextField(
            label = strings.productsBarcode,
            value = form.barcode,
            onValueChange = { value -> actions.onFormFieldChange(ProductFormField.BARCODE, value) },
            error = form.errorFor(ProductFormField.BARCODE),
            isRequired = false,
            enabled = !isSaving,
            modifier = fieldFocus.modifierFor(ProductFormField.BARCODE),
            trailingIcon = {
                IconButton(
                    onClick = { isBarcodeScanOpen = true },
                    enabled = !isSaving,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_scan_barcode),
                        contentDescription = strings.scannerTitle,
                    )
                }
            },
        )
        if (isBarcodeScanOpen) {
            BarcodeScanDialog(
                submitLabel = strings.scannerManualUse,
                onSubmit = { code ->
                    isBarcodeScanOpen = false
                    actions.onFormFieldChange(ProductFormField.BARCODE, code)
                },
                onDismiss = { isBarcodeScanOpen = false },
            )
        }
        HorizontalDivider(color = ProductPalette.Line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .toggleable(
                    value = form.isActive,
                    enabled = !isSaving,
                    role = Role.Switch,
                    onValueChange = actions.onFormActiveChange,
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.productsActive,
                    style = LocalAppTextStyles.current.body,
                    color = ProductPalette.Ink,
                )
                Text(
                    text = strings.productsActiveDesc,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = PosColors.TextSecondary,
                )
            }
            Switch(
                checked = form.isActive,
                onCheckedChange = null,
                enabled = !isSaving,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ProductPalette.Brand,
                    checkedBorderColor = ProductPalette.Brand,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = ProductPalette.Line,
                    uncheckedBorderColor = PosColors.Border,
                ),
            )
        }
        HorizontalDivider(color = ProductPalette.Line)
        ProductMediaSection(
            form = form,
            actions = actions,
            isSaving = isSaving,
        )
    }
}
