package id.umkm.pos.pro.ui.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R

private val PHOTO_PREVIEW_SIZE = 96.dp
private const val FIELD_MARKER_SEPARATOR = " · "
private const val REQUIRED_FIELD_MARKER = "Wajib"
private const val OPTIONAL_FIELD_MARKER = "Opsional"

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
                    text = "Kolom bertanda $REQUIRED_FIELD_MARKER harus diisi.",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = actions.onDismissForm) {
                Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Tutup formulir produk")
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
            if (form.errors.isNotEmpty()) {
                ValidationSummary(errors = form.errors)
            }
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
        ProductPhotoField(form = form, actions = actions)
        FormTextField(
            label = "Nama",
            value = form.name,
            onValueChange = { actions.onFormFieldChange(ProductFormField.NAME, it) },
            error = form.errorFor(ProductFormField.NAME),
            isRequired = true,
        )
        FormTextField(
            label = "SKU",
            value = form.sku,
            onValueChange = { actions.onFormFieldChange(ProductFormField.SKU, it) },
            error = form.errorFor(ProductFormField.SKU),
            isRequired = false,
            supportingText = "Dibuat otomatis bila dikosongkan.",
        )
        FormTextField(
            label = "Barcode",
            value = form.barcode,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BARCODE, it) },
            error = form.errorFor(ProductFormField.BARCODE),
            isRequired = false,
        )
        FormTextField(
            label = "Kategori",
            value = form.category,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CATEGORY, it) },
            error = form.errorFor(ProductFormField.CATEGORY),
            isRequired = false,
        )
        FormTextField(
            label = "Satuan dasar",
            value = form.baseUnit,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_UNIT, it) },
            error = form.errorFor(ProductFormField.BASE_UNIT),
            isRequired = true,
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
private fun ProductPhotoField(
    form: ProductFormState,
    actions: ProductsActions,
) {
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { pickedUri ->
        pickedUri?.let { uri -> actions.onProductImagePicked(uri.toString()) }
    }
    val isPickingAllowed = !form.isImageImporting
    val launchImagePicker: () -> Unit = {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Text(
            text = "Foto produk$FIELD_MARKER_SEPARATOR$OPTIONAL_FIELD_MARKER",
            style = LocalAppTextStyles.current.body,
        )
        ProductThumbnail(category = form.category, size = PHOTO_PREVIEW_SIZE, imagePath = form.imagePath)
        if (form.isImageRemoved) {
            Text(
                text = "Foto akan dihapus saat produk disimpan.",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (form.isImageImporting) {
                CircularProgressIndicator(modifier = Modifier.size(PosSizes.iconSmall), strokeWidth = 2.dp)
            }
            if (form.imagePath == null) {
                PhotoActionButton(
                    text = "Pilih gambar",
                    enabled = isPickingAllowed,
                    onClick = launchImagePicker,
                )
            } else {
                PhotoActionButton(
                    text = "Ganti",
                    enabled = isPickingAllowed,
                    onClick = launchImagePicker,
                )
                PhotoActionButton(
                    text = "Hapus",
                    enabled = isPickingAllowed,
                    onClick = actions.onProductImageRemove,
                )
            }
        }
    }
}

@Composable
private fun PhotoActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = PosSizes.touchTarget),
    ) {
        Text(text = text, style = LocalAppTextStyles.current.buttonLabel)
    }
}

@Composable
private fun ValidationSummary(errors: Map<ProductFormField, String>) {
    val palette = LocalPosSemanticPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = palette.dangerContainer, shape = MaterialTheme.shapes.small)
            .padding(PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_triangle_alert),
            contentDescription = null,
            tint = palette.danger,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Text(
            text = productFormErrorSummary(errors),
            style = LocalAppTextStyles.current.supportingLabel,
            color = palette.danger,
        )
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
            isRequired = true,
        )
        FormTextField(
            label = "Harga pokok",
            value = form.baseHpp,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_HPP, it) },
            error = form.errorFor(ProductFormField.BASE_HPP),
            keyboardType = KeyboardType.Number,
            isRequired = true,
            supportingText = "Harga pokok (HPP) per satuan dasar.",
        )
        TextButton(onClick = actions.onFormAdvancedToggle) {
            Icon(
                painter = painterResource(if (form.isAdvancedExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down),
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
            isRequired = false,
            supportingText = "Contoh: dus atau karton.",
        )
        FormTextField(
            label = "Faktor konversi",
            value = form.conversionFactor,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CONVERSION_FACTOR, it) },
            error = form.errorFor(ProductFormField.CONVERSION_FACTOR),
            keyboardType = KeyboardType.Decimal,
            isRequired = false,
            supportingText = "Jumlah satuan dasar per satuan, contoh 10.",
        )
        FormTextField(
            label = "Harga satuan",
            value = form.variantPrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.VARIANT_PRICE, it) },
            error = form.errorFor(ProductFormField.VARIANT_PRICE),
            keyboardType = KeyboardType.Number,
            isRequired = false,
        )
        FormTextField(
            label = "Minimal grosir",
            value = form.wholesaleMinQty,
            onValueChange = { actions.onFormFieldChange(ProductFormField.WHOLESALE_MIN_QTY, it) },
            error = form.errorFor(ProductFormField.WHOLESALE_MIN_QTY),
            keyboardType = KeyboardType.Number,
            isRequired = false,
        )
        FormTextField(
            label = "Harga grosir",
            value = form.wholesalePrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.WHOLESALE_PRICE, it) },
            error = form.errorFor(ProductFormField.WHOLESALE_PRICE),
            keyboardType = KeyboardType.Number,
            isRequired = false,
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
            isRequired = true,
        )
        FormTextField(
            label = "Peringatan stok menipis",
            value = form.minStockAlert,
            onValueChange = { actions.onFormFieldChange(ProductFormField.MIN_STOCK_ALERT, it) },
            error = form.errorFor(ProductFormField.MIN_STOCK_ALERT),
            keyboardType = KeyboardType.Decimal,
            isRequired = true,
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
    isRequired: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
) {
    val helperText = error ?: supportingText
    val marker = if (isRequired) REQUIRED_FIELD_MARKER else OPTIONAL_FIELD_MARKER
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("$label$FIELD_MARKER_SEPARATOR$marker") },
        isError = error != null,
        supportingText = helperText?.let { text -> { Text(text) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
