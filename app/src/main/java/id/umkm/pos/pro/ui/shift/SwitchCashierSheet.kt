package id.umkm.pos.pro.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

const val SHIFT_SHEET_TEST_TAG = "shift-sheet"
const val SHIFT_CASHIER_NAME_FIELD_TEST_TAG = "shift-cashier-name-field"
const val SHIFT_ACTUAL_CASH_FIELD_TEST_TAG = "shift-actual-cash-field"

private const val PHONE_SHEET_HEIGHT_FRACTION = 0.92f
private const val TABLET_SHEET_HEIGHT_FRACTION = 0.86f
private const val TABLET_SHEET_MAX_WIDTH_DP = 640
private val TRAILING_VALUE_MAX_WIDTH = 168.dp

@Composable
internal fun SwitchCashierSheet(
    state: ShiftUiState,
    actions: ShiftActions,
) {
    val strings = LocalPosStrings.current
    val isOpening = state.activeShift == null
    ShiftSheetContainer(
        title = if (isOpening) strings.shiftOpenTitle else strings.shiftCloseTitle,
        supportingText = if (isOpening) {
            strings.shiftOpenSupporting
        } else {
            strings.shiftCloseSupporting(state.activeShift?.cashierName.orEmpty())
        },
        onDismiss = actions.onDismissForm,
        footer = { ShiftFormFooter(state = state, actions = actions) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PosSpacing.md)
                .padding(top = PosSpacing.xs, bottom = PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            if (isOpening) {
                OpenShiftForm(state = state, actions = actions)
            } else {
                CloseShiftForm(state = state, actions = actions)
            }
            ShiftFeedback(state = state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ShiftSheetContainer(
    title: String,
    supportingText: String,
    onDismiss: () -> Unit,
    footer: @Composable (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val strings = LocalPosStrings.current
    BoxWithConstraints {
        val isExpandedWindow = maxWidth >= PosBreakpoints.expandedNavigation
        val phoneMaxHeight = maxHeight * PHONE_SHEET_HEIGHT_FRACTION
        val tabletMaxHeight = maxHeight * TABLET_SHEET_HEIGHT_FRACTION
        if (isExpandedWindow) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .widthIn(max = TABLET_SHEET_MAX_WIDTH_DP.dp)
                        .fillMaxWidth(0.94f)
                        .heightIn(max = tabletMaxHeight)
                        .testTag(SHIFT_SHEET_TEST_TAG),
                ) {
                    ShiftSheetLayout(
                        title = title,
                        supportingText = supportingText,
                        onDismiss = onDismiss,
                        closeDescription = strings.actionClose,
                        footer = footer,
                        content = content,
                    )
                }
            }
        } else {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ShiftSheetLayout(
                    title = title,
                    supportingText = supportingText,
                    onDismiss = onDismiss,
                    closeDescription = strings.actionClose,
                    footer = footer,
                    content = content,
                    modifier = Modifier
                        .heightIn(max = phoneMaxHeight)
                        .testTag(SHIFT_SHEET_TEST_TAG),
                )
            }
        }
    }
}

@Composable
private fun ShiftSheetLayout(
    title: String,
    supportingText: String,
    onDismiss: () -> Unit,
    closeDescription: String,
    footer: @Composable (() -> Unit)?,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PosSpacing.md, end = PosSpacing.xs, top = PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PosSectionHeader(
                title = title,
                supportingText = supportingText,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = closeDescription,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
        ) {
            content()
        }
        if (footer != null) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                footer()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenShiftForm(
    state: ShiftUiState,
    actions: ShiftActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.cashierNameInput,
            onValueChange = actions.onCashierNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SHIFT_CASHIER_NAME_FIELD_TEST_TAG),
            label = { Text(strings.shiftCashierNameLabel) },
            placeholder = { Text(strings.shiftCashierNamePlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        if (state.recentCashierNames.isNotEmpty()) {
            RecentCashierChips(
                names = state.recentCashierNames,
                selectedName = state.cashierNameInput,
                onSelect = actions.onCashierNameChange,
                label = strings.shiftRecentCashiersLabel,
            )
        }
        MoneyField(
            value = state.startingCashInput,
            onValueChange = actions.onStartingCashChange,
            label = strings.shiftStartingCashLabel,
            supportingText = strings.shiftStartingCashHint,
            imeAction = ImeAction.Done,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CloseShiftForm(
    state: ShiftUiState,
    actions: ShiftActions,
) {
    val strings = LocalPosStrings.current
    ShiftSummaryCard(
        summary = state.summary,
        isLoading = state.isSummaryLoading,
        errorMessage = state.summaryErrorMessage?.asTextOrNull(),
    )
    PosCard(modifier = Modifier.fillMaxWidth()) {
        MoneyField(
            value = state.actualEndingCashInput,
            onValueChange = actions.onActualEndingCashChange,
            label = strings.shiftActualEndingCashLabel,
            supportingText = strings.shiftActualEndingCashHint,
            imeAction = ImeAction.Done,
            modifier = Modifier.testTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG),
        )
        val variance = state.variance
        if (variance != null) {
            ShiftVarianceRow(
                variance = variance,
                label = strings.shiftVarianceLabel,
                modifier = Modifier.fillMaxWidth(),
            )
            if (variance != 0L) {
                VarianceConfirmation(
                    isAcknowledged = state.isVarianceAcknowledged,
                    onAcknowledgedChange = actions.onVarianceAcknowledgedChange,
                )
            }
        }
    }
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(
            title = strings.shiftNextCashierTitle,
            supportingText = strings.shiftNextStartingCashHint,
        )
        OutlinedTextField(
            value = state.nextCashierNameInput,
            onValueChange = actions.onNextCashierNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.shiftNextCashierNameLabel) },
            placeholder = { Text(strings.shiftNextCashierNamePlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        if (state.recentCashierNames.isNotEmpty()) {
            RecentCashierChips(
                names = state.recentCashierNames,
                selectedName = state.nextCashierNameInput,
                onSelect = actions.onNextCashierNameChange,
                label = strings.shiftRecentCashiersLabel,
            )
        }
        MoneyField(
            value = state.nextStartingCashInput,
            onValueChange = actions.onNextStartingCashChange,
            label = strings.shiftNextStartingCashLabel,
            supportingText = strings.shiftNextStartingCashHint,
            imeAction = ImeAction.Done,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentCashierChips(
    names: List<String>,
    selectedName: String,
    onSelect: (String) -> Unit,
    label: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            names.forEach { name ->
                FilterChip(
                    selected = selectedName.trim() == name,
                    onClick = { onSelect(name) },
                    label = { Text(name, maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text(supportingText) },
        prefix = { Text("Rp") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
    )
}

@Composable
private fun ShiftSummaryCard(
    summary: ShiftCashSummary?,
    isLoading: Boolean,
    errorMessage: String?,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PosSectionHeader(
                title = strings.shiftSummaryTitle,
                supportingText = summary?.let { strings.shiftTransactionCountLabel(it.transactionCount) },
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PosSizes.iconMedium),
                    strokeWidth = 2.dp,
                )
            }
        }
        when {
            summary != null -> {
                ShiftMoneyRow(
                    label = strings.shiftStartingCashLabel,
                    value = Rupiah.format(summary.startingCash),
                )
                ShiftMoneyRow(
                    label = strings.shiftCashSalesLabel,
                    value = Rupiah.format(summary.cashSales),
                )
                ShiftMoneyRow(
                    label = strings.shiftCashDebtPaymentsLabel,
                    value = Rupiah.format(summary.cashDebtPayments),
                )
                ShiftMoneyRow(
                    label = strings.shiftCashRefundsLabel,
                    value = if (summary.cashRefunds > 0L) {
                        "-${Rupiah.format(summary.cashRefunds)}"
                    } else {
                        Rupiah.format(0L)
                    },
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                ) {
                    Text(
                        text = strings.shiftExpectedEndingCashLabel,
                        style = LocalAppTextStyles.current.body,
                        modifier = Modifier.weight(1f),
                    )
                    AutoSizeNumberText(
                        text = Rupiah.format(summary.expectedEndingCash),
                        style = LocalAppTextStyles.current.financialValue,
                        modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
                    )
                }
            }

            errorMessage != null -> Text(
                text = errorMessage,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.error,
            )

            else -> Text(
                text = strings.shiftSummaryLoading,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ShiftMoneyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isEmphasized: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Text(
            text = label,
            style = if (isEmphasized) {
                LocalAppTextStyles.current.body
            } else {
                LocalAppTextStyles.current.supportingLabel
            },
            color = if (isEmphasized) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        AutoSizeNumberText(
            text = value,
            style = if (isEmphasized) {
                LocalAppTextStyles.current.financialValue
            } else {
                LocalAppTextStyles.current.body
            },
            modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
        )
    }
}

@Composable
internal fun ShiftVarianceRow(
    variance: Long,
    label: String,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val isBalanced = variance == 0L
    val stateLabel = when {
        variance == 0L -> strings.shiftVarianceBalanced
        variance > 0L -> strings.shiftVarianceOver
        else -> strings.shiftVarianceShort
    }
    val valueLabel = when {
        variance > 0L -> "+${Rupiah.format(variance)}"
        variance < 0L -> Rupiah.format(-variance)
        else -> Rupiah.format(0L)
    }
    val contentColor = when {
        variance == 0L -> palette.successText
        variance > 0L -> palette.warningText
        else -> palette.dangerText
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Icon(
            painter = painterResource(
                if (isBalanced) R.drawable.ic_circle_check else R.drawable.ic_triangle_alert,
            ),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stateLabel,
                style = LocalAppTextStyles.current.body,
                color = contentColor,
            )
        }
        AutoSizeNumberText(
            text = valueLabel,
            style = LocalAppTextStyles.current.financialValue,
            color = contentColor,
            modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
        )
    }
}

@Composable
private fun VarianceConfirmation(
    isAcknowledged: Boolean,
    onAcknowledgedChange: (Boolean) -> Unit,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PosSizes.touchTarget)
            .toggleable(
                value = isAcknowledged,
                role = Role.Checkbox,
                onValueChange = onAcknowledgedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isAcknowledged,
            onCheckedChange = null,
        )
        Text(
            text = strings.shiftVarianceAcknowledge,
            style = LocalAppTextStyles.current.supportingLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ShiftFeedback(state: ShiftUiState) {
    val validationMessage = state.validationMessage?.asTextOrNull()
    val submitErrorMessage = state.submitErrorMessage?.asTextOrNull()
    val message = validationMessage ?: submitErrorMessage ?: return
    Text(
        text = message,
        style = LocalAppTextStyles.current.supportingLabel,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun ShiftFormFooter(
    state: ShiftUiState,
    actions: ShiftActions,
) {
    val strings = LocalPosStrings.current
    if (state.activeShift == null) {
        PrimaryActionButton(
            text = strings.shiftOpenSubmit,
            onClick = actions.onOpenShift,
            enabled = state.isOpenInputReady,
            loading = state.isSubmitting,
        )
        SecondaryTextAction(
            text = strings.actionCancel,
            onClick = actions.onDismissForm,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    val gate = rememberSecurityGate()
    val pinLabel = strings.securityAuditActionLabel(CriticalAction.SWITCH_CASHIER)
    val closeOnly = {
        gate.request(CriticalAction.SWITCH_CASHIER, pinLabel, actions.onCloseShift)
    }
    val closeAndSwitch = {
        gate.request(CriticalAction.SWITCH_CASHIER, pinLabel, actions.onSwitchShift)
    }
    PrimaryActionButton(
        text = strings.shiftCloseAndSwitch,
        onClick = closeAndSwitch,
        enabled = state.isSwitchInputReady,
        loading = state.isSubmitting,
    )
    OutlinedButton(
        onClick = closeOnly,
        enabled = state.isCloseInputReady,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PosSizes.touchTarget),
    ) {
        Text(text = strings.shiftCloseOnly, style = LocalAppTextStyles.current.buttonLabel)
    }
    SecondaryTextAction(
        text = strings.actionCancel,
        onClick = actions.onDismissForm,
        modifier = Modifier.fillMaxWidth(),
    )
}
