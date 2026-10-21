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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R

private const val SHEET_HEIGHT_FRACTION = 0.9f
private const val FLOATING_CART_BAR_ELEVATION = 6
private val TRAILING_VALUE_MAX_WIDTH = 176.dp
private val TOTAL_VALUE_MAX_WIDTH = 208.dp

@Composable
internal fun CashierCartPane(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CartHeader(
            lineCount = state.lineCount,
            totalQuantity = state.totalQuantity,
            onClearCart = actions.onClearCart,
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
    if (state.lineCount == 0) return
    val strings = LocalPosStrings.current
    Surface(
        color = PosColors.ForestGreen,
        contentColor = Color.White,
        shape = PosShapes.large,
        shadowElevation = FLOATING_CART_BAR_ELEVATION.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm)
            .clickable(onClickLabel = strings.cashierOpenCart, onClick = actions.onOpenCartSheet),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PosSizes.touchTarget)
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = strings.cashierItemCountLine(
                        formatQuantity(state.totalQuantity),
                        state.lineCount,
                    ),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AutoSizeNumberText(
                    text = Rupiah.format(state.grandTotal),
                    style = LocalAppTextStyles.current.buttonLabel.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val hasActiveShift = state.activeShift != null
            FilledTonalButton(
                onClick = {
                    if (hasActiveShift) {
                        actions.onOpenPaymentSheet()
                    } else {
                        actions.onOpenShift(state.lineCount, actions.onClearCart)
                    }
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White,
                    contentColor = PosColors.ForestGreen,
                ),
                contentPadding = PaddingValues(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
            ) {
                Text(
                    text = if (hasActiveShift) strings.cashierPay else strings.cashierShiftOpenAction,
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
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
            CartHeader(
                lineCount = state.lineCount,
                totalQuantity = state.totalQuantity,
                onClearCart = actions.onClearCart,
                modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            )
            CartContents(state = state, actions = actions, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CartHeader(
    lineCount: Int,
    totalQuantity: Double,
    onClearCart: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PosSectionHeader(
            title = strings.cashierCartTitle,
            supportingText = cartItemSummary(lineCount = lineCount, totalQuantity = totalQuantity, strings = strings),
            modifier = Modifier.weight(1f),
        )
        if (lineCount > 0 && onClearCart != null) {
            SecondaryTextAction(
                text = strings.cashierClearCart,
                onClick = onClearCart,
            )
        }
    }
}

@Composable
private fun CartContents(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (state.cartLines.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_shopping_cart,
                title = strings.cashierEmptyCartTitle,
                supportingText = strings.cashierEmptyCartMsg,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                items(items = state.cartLines, key = { line -> line.lineId }) { line ->
                    CartLineCard(line = line, actions = actions)
                }
            }
        }
        HorizontalDivider()
        CartTotals(state = state)
        val hasActiveShift = state.activeShift != null
        if (!hasActiveShift) {
            Text(
                text = strings.cashierShiftRequired,
                style = LocalAppTextStyles.current.supportingLabel,
                color = LocalPosSemanticPalette.current.warningText,
                modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
            )
        }
        PrimaryActionButton(
            text = if (hasActiveShift) strings.cashierPay else strings.cashierShiftOpenAction,
            onClick = {
                if (hasActiveShift) {
                    actions.onOpenPaymentSheet()
                } else {
                    actions.onOpenShift(state.lineCount, actions.onClearCart)
                }
            },
            enabled = state.cartLines.isNotEmpty(),
            loading = state.isSubmitting,
            modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        )
    }
}

@Composable
private fun CartLineCard(
    line: CashierCartLine,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = strings.cashierEditLineOptions(line.productName),
                    onClick = { actions.onOpenLineEditor(line.lineId) },
                ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
            AutoSizeNumberText(
                text = Rupiah.format(line.lineSubtotal),
                style = LocalAppTextStyles.current.financialValue,
                modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            IconButton(onClick = { actions.onDecreaseQuantity(line.lineId) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_minus),
                    contentDescription = strings.cashierDecreaseLineQuantity(line.productName),
                )
            }
            Text(
                text = formatQuantity(line.quantity),
                style = LocalAppTextStyles.current.financialValue,
            )
            IconButton(onClick = { actions.onIncreaseQuantity(line.lineId) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = strings.cashierIncreaseLineQuantity(line.productName),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            SecondaryTextAction(
                text = strings.cashierUnitAndNegotiation,
                onClick = { actions.onOpenLineEditor(line.lineId) },
            )
            IconButton(onClick = { actions.onRemoveLine(line.lineId) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = strings.cashierRemoveLine(line.productName),
                )
            }
        }
    }
}

@Composable
private fun LinePriceNote(line: CashierCartLine) {
    val strings = LocalPosStrings.current
    val note = when {
        line.itemDiscount > 0L -> strings.cashierLineDiscount(Rupiah.format(line.itemDiscount))
        line.isNegotiated -> strings.cashierLineNegotiatedBadge
        line.isWholesale -> strings.cashierLineWholesaleBadge
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
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
    ) {
        TotalLine(
            label = strings.cashierItemLabel,
            value = strings.cashierItemQuantityLine(formatQuantity(state.totalQuantity), state.lineCount),
        )
        TotalLine(label = strings.cashierSubtotal, value = Rupiah.format(pricedCart.subtotal))
        if (pricedCart.invoiceDiscount > 0L) {
            TotalLine(label = strings.cashierDiscount, value = "-${Rupiah.format(pricedCart.invoiceDiscount)}")
        }
        if (pricedCart.taxTotal > 0L) {
            TotalLine(label = strings.cashierTax, value = Rupiah.format(pricedCart.taxTotal))
        }
        if (pricedCart.rounding != 0L) {
            TotalLine(label = strings.cashierRounding, value = Rupiah.format(pricedCart.rounding))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = PosSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = strings.cashierTotal, style = LocalAppTextStyles.current.sectionHeading)
            AutoSizeNumberText(
                text = Rupiah.format(pricedCart.grandTotal),
                style = LocalAppTextStyles.current.financialValueLarge,
                modifier = Modifier.widthIn(max = TOTAL_VALUE_MAX_WIDTH),
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
        AutoSizeNumberText(
            text = value,
            style = LocalAppTextStyles.current.body,
            modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierLineEditorSheet(
    editor: CartLineEditorState,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val applyLineEditor: () -> Unit = {
        val pinRequest = resolveLineEditorPinRequest(gate.policy(), editor.pricing)
        if (pinRequest == null) {
            actions.onApplyLineEditor()
        } else {
            gate.request(pinRequest.action, pinRequest.label(strings)) {
                actions.onApplyLineEditor()
            }
        }
    }
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
            PosSectionHeader(title = strings.cashierLineItemOptions, supportingText = editor.productName)
            PosCard(modifier = Modifier.fillMaxWidth()) {
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
                    label = { Text(strings.cashierNegotiatedPrice) },
                    supportingText = { Text(strings.cashierNegotiatedPriceHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = editor.itemDiscountInput,
                    onValueChange = actions.onLineEditorDiscountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.cashierItemDiscount) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            val validationMessage = editor.validationMessage?.asTextOrNull()
            if (validationMessage != null) {
                Text(
                    text = validationMessage,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
                OutlinedButton(
                    onClick = actions.onCloseLineEditor,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(text = strings.actionCancel)
                }
                Button(
                    onClick = applyLineEditor,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(text = strings.cashierApply, style = LocalAppTextStyles.current.buttonLabel)
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
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = strings.cashierUnit,
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
                label = { Text(strings.cashierBaseUnit) },
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

private fun cartItemSummary(lineCount: Int, totalQuantity: Double, strings: PosStrings): String =
    if (lineCount == 0) {
        strings.cashierCartEmptyLine
    } else {
        strings.cashierItemQuantityLine(formatQuantity(totalQuantity), lineCount)
    }
