package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.SectionHeading
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val SHEET_HEIGHT_FRACTION = 0.9f

@Composable
internal fun CashierCartPane(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SectionHeading(
            text = "Cart",
            modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        )
        CartContents(state = state, actions = actions, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun CashierCartBar(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = state.lineCount > 0, onClick = actions.onOpenCartSheet)
                    .padding(vertical = PosSpacing.xs),
            ) {
                Text(
                    text = if (state.lineCount == 0) "Cart is empty" else "${state.lineCount} items",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Rupiah.format(state.grandTotal),
                    style = LocalAppTextStyles.current.financialValue,
                )
            }
            Button(
                onClick = actions.onOpenPaymentSheet,
                enabled = state.lineCount > 0 && !state.isSubmitting,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.Filled.PointOfSale,
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(text = "Charge", style = LocalAppTextStyles.current.buttonLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierCartSheet(
    state: CashierUiState,
    actions: CashierActions,
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * SHEET_HEIGHT_FRACTION).dp
    ModalBottomSheet(
        onDismissRequest = actions.onCloseCartSheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)) {
            SectionHeading(
                text = "Cart",
                modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            )
            CartContents(state = state, actions = actions, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CartContents(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (state.cartLines.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ShoppingCart,
                title = "Cart is empty",
                supportingText = "Tap a product to add it to the sale.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
            ) {
                items(items = state.cartLines, key = { line -> line.lineId }) { line ->
                    CartLineRow(line = line, actions = actions)
                }
            }
        }
        HorizontalDivider()
        CartTotals(state = state)
        Button(
            onClick = actions.onOpenPaymentSheet,
            enabled = state.cartLines.isNotEmpty() && !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm)
                .heightIn(min = PosSizes.touchTarget),
        ) {
            Text(text = "Charge", style = LocalAppTextStyles.current.buttonLabel)
        }
    }
}

@Composable
private fun CartLineRow(
    line: CashierCartLine,
    actions: CashierActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = line.productName,
                    style = LocalAppTextStyles.current.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${Rupiah.format(line.unitPrice)} / ${line.unitLabel}",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinePriceNote(line = line)
            }
            Text(
                text = Rupiah.format(line.lineSubtotal),
                style = LocalAppTextStyles.current.financialValue,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            IconButton(onClick = { actions.onDecreaseQuantity(line.lineId) }) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease quantity of ${line.productName}",
                )
            }
            Text(
                text = formatQuantity(line.quantity),
                style = LocalAppTextStyles.current.financialValue,
            )
            IconButton(onClick = { actions.onIncreaseQuantity(line.lineId) }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase quantity of ${line.productName}",
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { actions.onOpenLineEditor(line.lineId) }) {
                Text(text = "Edit")
            }
            IconButton(onClick = { actions.onRemoveLine(line.lineId) }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remove ${line.productName}",
                )
            }
        }
    }
}

@Composable
private fun LinePriceNote(line: CashierCartLine) {
    val note = when {
        line.itemDiscount > 0L -> "Item discount ${Rupiah.format(line.itemDiscount)}"
        line.isNegotiated -> "Negotiated price"
        line.isWholesale -> "Wholesale price"
        else -> null
    } ?: return
    Text(
        text = note,
        style = LocalAppTextStyles.current.supportingLabel,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun CartTotals(state: CashierUiState) {
    val pricedCart = state.pricedCart ?: return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
    ) {
        TotalLine(label = "Items", value = state.lineCount.toString())
        TotalLine(label = "Subtotal", value = Rupiah.format(pricedCart.subtotal))
        if (pricedCart.invoiceDiscount > 0L) {
            TotalLine(label = "Discount", value = "-${Rupiah.format(pricedCart.invoiceDiscount)}")
        }
        if (pricedCart.taxTotal > 0L) {
            TotalLine(label = "Tax", value = Rupiah.format(pricedCart.taxTotal))
        }
        if (pricedCart.rounding != 0L) {
            TotalLine(label = "Rounding", value = Rupiah.format(pricedCart.rounding))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = PosSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Total", style = LocalAppTextStyles.current.sectionHeading)
            Text(
                text = Rupiah.format(pricedCart.grandTotal),
                style = LocalAppTextStyles.current.financialValueLarge,
            )
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = LocalAppTextStyles.current.body)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierLineEditorSheet(
    editor: CartLineEditorState,
    actions: CashierActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onCloseLineEditor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = PosSpacing.md)
                .padding(bottom = PosSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            SectionHeading(text = "Line options")
            Text(
                text = editor.productName,
                style = LocalAppTextStyles.current.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (editor.unitOptions.isNotEmpty()) {
                UnitSelector(
                    options = editor.unitOptions,
                    selectedUnitId = editor.selectedUnitId,
                    onSelect = actions.onLineEditorUnitChange,
                )
            }
            OutlinedTextField(
                value = editor.negotiatedUnitPriceInput,
                onValueChange = actions.onLineEditorPriceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Negotiated unit price") },
                supportingText = { Text("Leave empty to use the normal price.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = editor.itemDiscountInput,
                onValueChange = actions.onLineEditorDiscountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Item discount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            editor.validationMessage?.let { message ->
                Text(
                    text = message,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
                OutlinedButton(
                    onClick = actions.onCloseLineEditor,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(text = "Cancel")
                }
                Button(
                    onClick = actions.onApplyLineEditor,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(text = "Apply", style = LocalAppTextStyles.current.buttonLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitSelector(
    options: List<ProductUnit>,
    selectedUnitId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = "Unit",
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            FilterChip(
                selected = selectedUnitId == null,
                onClick = { onSelect(null) },
                label = { Text("Base unit") },
            )
            options.forEach { unit ->
                FilterChip(
                    selected = selectedUnitId == unit.id,
                    onClick = { onSelect(unit.id) },
                    label = { Text(unit.variantName) },
                )
            }
        }
    }
}
