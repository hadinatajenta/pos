package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (form.isEditing) "Ubah produk" else "Produk baru",
                    style = LocalAppTextStyles.current.screenTitle,
                )
                Text(
                    text = "Dasar, harga & satuan, stok",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = actions.onDismissForm) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Tutup formulir produk")
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PosSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            PrimaryActionButton(
                text = if (form.isEditing) "Simpan perubahan" else "Simpan produk",
                onClick = actions.onSubmitForm,
                loading = isSaving,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = actions.onDismissForm,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = "Batal", style = LocalAppTextStyles.current.buttonLabel)
            }
        }
    }
}

@Composable
private fun BasicsSection(
    form: ProductFormState,
    actions: ProductsActions,
) {
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = "Dasar")
        FormTextField(
            label = "Nama",
            value = form.name,
            onValueChange = { actions.onFormFieldChange(ProductFormField.NAME, it) },
            error = form.errorFor(ProductFormField.NAME),
        )
        FormTextField(
            label = "SKU",
            value = form.sku,
            onValueChange = { actions.onFormFieldChange(ProductFormField.SKU, it) },
            error = form.errorFor(ProductFormField.SKU),
            supportingText = "Dibuat otomatis bila dikosongkan.",
        )
        FormTextField(
            label = "Barcode",
            value = form.barcode,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BARCODE, it) },
            error = form.errorFor(ProductFormField.BARCODE),
        )
        FormTextField(
            label = "Kategori",
            value = form.category,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CATEGORY, it) },
            error = form.errorFor(ProductFormField.CATEGORY),
        )
        FormTextField(
            label = "Satuan dasar",
            value = form.baseUnit,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_UNIT, it) },
            error = form.errorFor(ProductFormField.BASE_UNIT),
            supportingText = "Contoh: pcs, kg, atau liter.",
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Aktif", style = LocalAppTextStyles.current.body)
                Text(
                    text = "Produk nonaktif tidak muncul di kasir.",
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
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = "Harga & Satuan")
        FormTextField(
            label = "Harga jual",
            value = form.basePrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_PRICE, it) },
            error = form.errorFor(ProductFormField.BASE_PRICE),
            keyboardType = KeyboardType.Number,
        )
        FormTextField(
            label = "Harga pokok",
            value = form.baseHpp,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_HPP, it) },
            error = form.errorFor(ProductFormField.BASE_HPP),
            keyboardType = KeyboardType.Number,
            supportingText = "Harga pokok (HPP) per satuan dasar.",
        )
        TextButton(onClick = actions.onFormAdvancedToggle) {
            Icon(
                imageVector = if (form.isAdvancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(PosSpacing.xs))
            Text(text = "Opsi lainnya")
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
            label = "Nama satuan",
            value = form.variantName,
            onValueChange = { actions.onFormFieldChange(ProductFormField.VARIANT_NAME, it) },
            error = form.errorFor(ProductFormField.VARIANT_NAME),
            supportingText = "Contoh: dus atau karton.",
        )
        FormTextField(
            label = "Faktor konversi",
            value = form.conversionFactor,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CONVERSION_FACTOR, it) },
            error = form.errorFor(ProductFormField.CONVERSION_FACTOR),
            keyboardType = KeyboardType.Decimal,
            supportingText = "Jumlah satuan dasar per satuan, contoh 10.",
        )
        FormTextField(
            label = "Harga satuan",
            value = form.variantPrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.VARIANT_PRICE, it) },
            error = form.errorFor(ProductFormField.VARIANT_PRICE),
            keyboardType = KeyboardType.Number,
        )
        FormTextField(
            label = "Minimal grosir",
            value = form.wholesaleMinQty,
            onValueChange = { actions.onFormFieldChange(ProductFormField.WHOLESALE_MIN_QTY, it) },
            error = form.errorFor(ProductFormField.WHOLESALE_MIN_QTY),
            keyboardType = KeyboardType.Number,
        )
        FormTextField(
            label = "Harga grosir",
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
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = "Stok")
        FormTextField(
            label = "Jumlah stok",
            value = form.stockQuantity,
            onValueChange = { actions.onFormFieldChange(ProductFormField.STOCK_QUANTITY, it) },
            error = form.errorFor(ProductFormField.STOCK_QUANTITY),
            keyboardType = KeyboardType.Decimal,
        )
        FormTextField(
            label = "Peringatan stok menipis",
            value = form.minStockAlert,
            onValueChange = { actions.onFormFieldChange(ProductFormField.MIN_STOCK_ALERT, it) },
            error = form.errorFor(ProductFormField.MIN_STOCK_ALERT),
            keyboardType = KeyboardType.Decimal,
            supportingText = "Ditandai stok menipis saat jumlah ini tercapai.",
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
