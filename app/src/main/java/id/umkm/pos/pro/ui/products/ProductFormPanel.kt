package id.umkm.pos.pro.ui.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.asText
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.scanner.BarcodeScanDialog
import id.umkm.pos.pro.ui.security.rememberSecurityGate
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
private const val RUPIAH_PREFIX = "Rp"

private val FORM_FIELD_ORDER = listOf(
    ProductFormField.NAME,
    ProductFormField.SKU,
    ProductFormField.BARCODE,
    ProductFormField.CATEGORY,
    ProductFormField.BASE_UNIT,
    ProductFormField.BASE_PRICE,
    ProductFormField.BASE_HPP,
    ProductFormField.VARIANT_NAME,
    ProductFormField.CONVERSION_FACTOR,
    ProductFormField.VARIANT_PRICE,
    ProductFormField.WHOLESALE_MIN_QTY,
    ProductFormField.WHOLESALE_PRICE,
    ProductFormField.STOCK_QUANTITY,
    ProductFormField.MIN_STOCK_ALERT,
)

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

private class FormFieldFocus {

    private val focusRequesters: Map<ProductFormField, FocusRequester> =
        ProductFormField.entries.associateWith { FocusRequester() }

    @OptIn(ExperimentalFoundationApi::class)
    private val bringIntoViewRequesters: Map<ProductFormField, BringIntoViewRequester> =
        ProductFormField.entries.associateWith { BringIntoViewRequester() }

    @OptIn(ExperimentalFoundationApi::class)
    fun modifierFor(field: ProductFormField): Modifier = Modifier
        .focusRequester(focusRequesters.getValue(field))
        .bringIntoViewRequester(bringIntoViewRequesters.getValue(field))

    @OptIn(ExperimentalFoundationApi::class)
    suspend fun focusFirstError(errors: Map<ProductFormField, UiText>) {
        val target = FORM_FIELD_ORDER.firstOrNull { field -> field in errors } ?: return
        focusRequesters.getValue(target).requestFocus()
        bringIntoViewRequesters.getValue(target).bringIntoView()
    }
}

@Composable
internal fun ProductFormPanel(
    form: ProductFormState,
    isSaving: Boolean,
    categorySuggestions: List<String>,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val fieldFocus = remember { FormFieldFocus() }
    var isCostRevealed by remember(form.productId) { mutableStateOf(false) }
    val showsCostValue = form.persistedPricing == null ||
        isCostRevealed ||
        gate.isUnlocked(CriticalAction.EDIT_PRODUCT_COST)
    val pricingChange = productPricingChange(form)
    val pricingPinAction = when {
        pricingChange.requiresCostPin && !isCostRevealed -> CriticalAction.EDIT_PRODUCT_COST
        pricingChange.requiresPricePin -> CriticalAction.EDIT_PRODUCT_PRICE
        else -> null
    }
    val onSave: () -> Unit = {
        if (pricingPinAction == null || validateProductForm(form).isNotEmpty()) {
            actions.onSubmitForm()
        } else {
            gate.request(pricingPinAction, pricingPinLabel(pricingPinAction, strings)) {
                actions.onSubmitForm()
            }
        }
    }
    LaunchedEffect(form.validationFailures) {
        if (form.validationFailures > 0L) {
            fieldFocus.focusFirstError(form.errors)
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (form.isEditing) strings.productsEditProduct else strings.productsNewProduct,
                    style = LocalAppTextStyles.current.screenTitle,
                )
                Text(
                    text = strings.productsRequiredFieldNotice,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = actions.onDismissForm) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = strings.productsCloseForm,
                )
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
            BasicsSection(
                form = form,
                categorySuggestions = categorySuggestions,
                fieldFocus = fieldFocus,
                actions = actions,
            )
            PricingSection(
                form = form,
                fieldFocus = fieldFocus,
                actions = actions,
                showsCostValue = showsCostValue,
                onRequestCostUnlock = {
                    gate.request(CriticalAction.EDIT_PRODUCT_COST, strings.productsBaseHpp) {
                        isCostRevealed = true
                    }
                },
            )
            StockSection(form = form, fieldFocus = fieldFocus, actions = actions)
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
                text = if (form.isEditing) strings.productsSaveChanges else strings.productsSaveNew,
                onClick = onSave,
                loading = isSaving,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = actions.onDismissForm,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.actionCancel, style = LocalAppTextStyles.current.buttonLabel)
            }
        }
    }
}

@Composable
private fun BasicsSection(
    form: ProductFormState,
    categorySuggestions: List<String>,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = strings.productsSectionBasics)
        ProductPhotoField(form = form, actions = actions)
        FormTextField(
            label = strings.productsFieldName,
            value = form.name,
            onValueChange = { actions.onFormFieldChange(ProductFormField.NAME, it) },
            error = form.errorFor(ProductFormField.NAME),
            isRequired = true,
            modifier = fieldFocus.modifierFor(ProductFormField.NAME),
        )
        FormTextField(
            label = strings.productsSku,
            value = form.sku,
            onValueChange = { actions.onFormFieldChange(ProductFormField.SKU, it) },
            error = form.errorFor(ProductFormField.SKU),
            isRequired = false,
            supportingText = strings.productsSkuAutoHint,
            modifier = fieldFocus.modifierFor(ProductFormField.SKU),
        )
        var isBarcodeScanOpen by remember { mutableStateOf(false) }
        FormTextField(
            label = strings.productsBarcode,
            value = form.barcode,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BARCODE, it) },
            error = form.errorFor(ProductFormField.BARCODE),
            isRequired = false,
            modifier = fieldFocus.modifierFor(ProductFormField.BARCODE),
            trailingIcon = {
                IconButton(onClick = { isBarcodeScanOpen = true }) {
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
        CategoryField(
            value = form.category,
            error = form.errorFor(ProductFormField.CATEGORY),
            suggestions = categorySuggestions,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CATEGORY, it) },
            modifier = fieldFocus.modifierFor(ProductFormField.CATEGORY),
        )
        BaseUnitField(
            value = form.baseUnit,
            error = form.errorFor(ProductFormField.BASE_UNIT),
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_UNIT, it) },
            modifier = fieldFocus.modifierFor(ProductFormField.BASE_UNIT),
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = strings.productsActive, style = LocalAppTextStyles.current.body)
                Text(
                    text = strings.productsActiveDesc,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = form.isActive, onCheckedChange = actions.onFormActiveChange)
        }
    }
}

@Composable
private fun CategoryField(
    value: String,
    error: UiText?,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    var isSuggestionsOpen by remember { mutableStateOf(false) }
    FormTextField(
        label = strings.productsCategory,
        value = value,
        onValueChange = onValueChange,
        error = error,
        isRequired = false,
        modifier = modifier,
        trailingIcon = if (suggestions.isEmpty()) {
            null
        } else {
            {
                Box {
                    IconButton(onClick = { isSuggestionsOpen = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = strings.productsCategoryPickExisting,
                        )
                    }
                    DropdownMenu(
                        expanded = isSuggestionsOpen,
                        onDismissRequest = { isSuggestionsOpen = false },
                    ) {
                        suggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    isSuggestionsOpen = false
                                    onValueChange(suggestion)
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BaseUnitField(
    value: String,
    error: UiText?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        FormTextField(
            label = strings.productsFieldBaseUnit,
            value = value,
            onValueChange = onValueChange,
            error = error,
            isRequired = true,
            supportingText = strings.productsBaseUnitHint,
            modifier = modifier,
        )
        Text(
            text = strings.productsBaseUnitQuickPick,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            COMMON_BASE_UNITS.forEach { unit ->
                val isSelected = value.trim().equals(unit, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onValueChange(unit) },
                    label = { Text(unit) },
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProductPhotoField(
    form: ProductFormState,
    actions: ProductsActions,
) {
    val strings = LocalPosStrings.current
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
            text = "${strings.productsPhoto}$FIELD_MARKER_SEPARATOR${strings.commonOptional}",
            style = LocalAppTextStyles.current.body,
        )
        ProductThumbnail(category = form.category, size = PHOTO_PREVIEW_SIZE, imagePath = form.imagePath)
        if (form.isImageRemoved) {
            Text(
                text = strings.productsPhotoWillBeRemoved,
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
                    text = strings.productsPhotoPick,
                    enabled = isPickingAllowed,
                    onClick = launchImagePicker,
                )
            } else {
                PhotoActionButton(
                    text = strings.productsPhotoChange,
                    enabled = isPickingAllowed,
                    onClick = launchImagePicker,
                )
                PhotoActionButton(
                    text = strings.productsPhotoRemove,
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
private fun ValidationSummary(errors: Map<ProductFormField, UiText>) {
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
            text = productFormErrorSummary(errors).asText(),
            style = LocalAppTextStyles.current.supportingLabel,
            color = palette.danger,
        )
    }
}

@Composable
private fun PricingSection(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
    showsCostValue: Boolean,
    onRequestCostUnlock: () -> Unit,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = strings.productsPricingAndUnits)
        FormTextField(
            label = strings.productsBasePrice,
            value = form.basePrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_PRICE, it) },
            error = form.errorFor(ProductFormField.BASE_PRICE),
            keyboardType = KeyboardType.Number,
            isRequired = true,
            prefix = RUPIAH_PREFIX,
            modifier = fieldFocus.modifierFor(ProductFormField.BASE_PRICE),
        )
        if (showsCostValue) {
            FormTextField(
                label = strings.productsBaseHpp,
                value = form.baseHpp,
                onValueChange = { actions.onFormFieldChange(ProductFormField.BASE_HPP, it) },
                error = form.errorFor(ProductFormField.BASE_HPP),
                keyboardType = KeyboardType.Number,
                isRequired = true,
                supportingText = strings.productsHppHint,
                prefix = RUPIAH_PREFIX,
                modifier = fieldFocus.modifierFor(ProductFormField.BASE_HPP),
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                FormTextField(
                    label = strings.productsBaseHpp,
                    value = strings.securityMaskedValue,
                    onValueChange = {},
                    error = form.errorFor(ProductFormField.BASE_HPP),
                    keyboardType = KeyboardType.Number,
                    isRequired = true,
                    supportingText = strings.productsHppHint,
                    prefix = RUPIAH_PREFIX,
                    readOnly = true,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onRequestCostUnlock),
                )
            }
        }
        TextButton(onClick = actions.onFormAdvancedToggle) {
            Icon(
                painter = painterResource(if (form.isAdvancedExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down),
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(PosSpacing.xs))
            Text(text = strings.productsOtherOptions)
        }
        if (form.isAdvancedExpanded) {
            AdvancedUnitFields(form = form, fieldFocus = fieldFocus, actions = actions)
        }
    }
}

@Composable
private fun AdvancedUnitFields(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        FormTextField(
            label = strings.productsUnitName,
            value = form.variantName,
            onValueChange = { actions.onFormFieldChange(ProductFormField.VARIANT_NAME, it) },
            error = form.errorFor(ProductFormField.VARIANT_NAME),
            isRequired = false,
            supportingText = strings.productsUnitNameHint,
            modifier = fieldFocus.modifierFor(ProductFormField.VARIANT_NAME),
        )
        FormTextField(
            label = strings.productsConversionFactor,
            value = form.conversionFactor,
            onValueChange = { actions.onFormFieldChange(ProductFormField.CONVERSION_FACTOR, it) },
            error = form.errorFor(ProductFormField.CONVERSION_FACTOR),
            keyboardType = KeyboardType.Decimal,
            isRequired = false,
            supportingText = strings.productsConversionFactorHint,
            modifier = fieldFocus.modifierFor(ProductFormField.CONVERSION_FACTOR),
        )
        FormTextField(
            label = strings.productsUnitPrice,
            value = form.variantPrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.VARIANT_PRICE, it) },
            error = form.errorFor(ProductFormField.VARIANT_PRICE),
            keyboardType = KeyboardType.Number,
            isRequired = false,
            prefix = RUPIAH_PREFIX,
            modifier = fieldFocus.modifierFor(ProductFormField.VARIANT_PRICE),
        )
        FormTextField(
            label = strings.productsWholesaleMinQty,
            value = form.wholesaleMinQty,
            onValueChange = { actions.onFormFieldChange(ProductFormField.WHOLESALE_MIN_QTY, it) },
            error = form.errorFor(ProductFormField.WHOLESALE_MIN_QTY),
            keyboardType = KeyboardType.Number,
            isRequired = false,
            modifier = fieldFocus.modifierFor(ProductFormField.WHOLESALE_MIN_QTY),
        )
        FormTextField(
            label = strings.productsWholesalePrice,
            value = form.wholesalePrice,
            onValueChange = { actions.onFormFieldChange(ProductFormField.WHOLESALE_PRICE, it) },
            error = form.errorFor(ProductFormField.WHOLESALE_PRICE),
            keyboardType = KeyboardType.Number,
            isRequired = false,
            prefix = RUPIAH_PREFIX,
            modifier = fieldFocus.modifierFor(ProductFormField.WHOLESALE_PRICE),
        )
    }
}

@Composable
private fun StockSection(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = strings.productsSectionStock)
        FormTextField(
            label = strings.productsStockQty,
            value = form.stockQuantity,
            onValueChange = { actions.onFormFieldChange(ProductFormField.STOCK_QUANTITY, it) },
            error = form.errorFor(ProductFormField.STOCK_QUANTITY),
            keyboardType = KeyboardType.Decimal,
            isRequired = true,
            modifier = fieldFocus.modifierFor(ProductFormField.STOCK_QUANTITY),
        )
        FormTextField(
            label = strings.productsMinStockAlert,
            value = form.minStockAlert,
            onValueChange = { actions.onFormFieldChange(ProductFormField.MIN_STOCK_ALERT, it) },
            error = form.errorFor(ProductFormField.MIN_STOCK_ALERT),
            keyboardType = KeyboardType.Decimal,
            isRequired = false,
            supportingText = strings.productsLowStockHint,
            modifier = fieldFocus.modifierFor(ProductFormField.MIN_STOCK_ALERT),
        )
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: UiText?,
    isRequired: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
    prefix: String? = null,
    readOnly: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val strings = LocalPosStrings.current
    val helperText = error?.asTextOrNull() ?: supportingText
    val marker = if (isRequired) strings.commonRequired else strings.commonOptional
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("$label$FIELD_MARKER_SEPARATOR$marker") },
        isError = error != null,
        supportingText = helperText?.let { text -> { Text(text) } },
        prefix = prefix?.let { text -> { Text(text) } },
        trailingIcon = trailingIcon,
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
