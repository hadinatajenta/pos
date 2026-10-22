package id.umkm.pos.pro.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ThousandSeparatorVisualTransformation
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PersonAvatar
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

const val SHIFT_SHEET_TEST_TAG = "shift-sheet"
const val SHIFT_CASHIER_NAME_FIELD_TEST_TAG = "shift-cashier-name-field"
const val SHIFT_ACTUAL_CASH_FIELD_TEST_TAG = "shift-actual-cash-field"
const val SHIFT_PRIMARY_ACTION_TEST_TAG = "shift-primary-action"
const val SHIFT_HANDOVER_TOGGLE_TEST_TAG = "shift-handover-toggle"
const val SHIFT_OPERATOR_TAB_TEST_TAG = "shift-operator-tab"
const val SHIFT_CLOSE_TAB_TEST_TAG = "shift-close-tab"
const val SHIFT_STAFF_ROW_TEST_TAG_PREFIX = "shift-staff-row-"

private const val TABLET_SHEET_HEIGHT_FRACTION = 0.86f
private const val TABLET_SHEET_MAX_WIDTH_DP = 640
private val TRAILING_VALUE_MAX_WIDTH = 168.dp
private val CASHIER_AVATAR_SIZE = 40.dp
private val MONEY_FIELD_TEXT_SIZE = 18.sp
private val ON_DUTY_CHIP_ICON_SIZE = 14.dp

private enum class ShiftFormTab {
    OPERATOR,
    CLOSE,
}

@Composable
internal fun SwitchCashierSheet(
    state: ShiftUiState,
    actions: ShiftActions,
    onManageStaff: () -> Unit = {},
) {
    val strings = LocalPosStrings.current
    val isOpening = state.activeShift == null
    var selectedTab by rememberSaveable(state.activeShift?.id) {
        mutableStateOf(ShiftFormTab.OPERATOR)
    }
    var isHandoverEnabled by rememberSaveable(state.activeShift?.id) {
        mutableStateOf(false)
    }
    val otherStaff = state.staffOptions.filter { it.id != state.activeStaff?.id }
    ShiftSheetContainer(
        title = if (isOpening) strings.shiftOpenTitle else strings.moreShiftTitle,
        supportingText = if (isOpening) {
            strings.shiftOpenSupporting
        } else {
            strings.shiftCloseSupporting(activeOperatorName(state))
        },
        onDismiss = actions.onDismissForm,
        footer = {
            ShiftSheetFooter(
                state = state,
                actions = actions,
                isOpening = isOpening,
                selectedTab = selectedTab,
                isHandoverEnabled = isHandoverEnabled,
            )
        },
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
                OpenShiftContent(
                    state = state,
                    actions = actions,
                    onManageStaff = onManageStaff,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
                    ShiftModeTabs(
                        selectedTab = selectedTab,
                        onSelectTab = { tab -> selectedTab = tab },
                    )
                    Text(
                        text = if (selectedTab == ShiftFormTab.OPERATOR) {
                            strings.shiftOperatorDescription
                        } else {
                            strings.shiftCloseDescription
                        },
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when (selectedTab) {
                    ShiftFormTab.OPERATOR -> OperatorContent(
                        state = state,
                        actions = actions,
                        otherStaff = otherStaff,
                        onManageStaff = onManageStaff,
                    )

                    ShiftFormTab.CLOSE -> CloseShiftContent(
                        state = state,
                        actions = actions,
                        otherStaff = otherStaff,
                        isHandoverEnabled = isHandoverEnabled,
                        onHandoverChange = { enabled -> isHandoverEnabled = enabled },
                    )
                }
            }
            ShiftFeedback(state = state)
        }
    }
}

private fun activeOperatorName(state: ShiftUiState): String =
    state.activeStaff?.name ?: state.activeShift?.cashierName.orEmpty()

@OptIn(ExperimentalMaterial3Api::class)
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
        if (isExpandedWindow) {
            val tabletMaxHeight = maxHeight * TABLET_SHEET_HEIGHT_FRACTION
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentAlignment = Alignment.Center,
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
            }
        } else {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                BoxWithConstraints {
                    val statusBarTop = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding()
                    val maxSheetHeight = maxHeight - statusBarTop - PosSpacing.md
                    ShiftSheetLayout(
                        title = title,
                        supportingText = supportingText,
                        onDismiss = onDismiss,
                        closeDescription = strings.actionClose,
                        footer = footer,
                        content = content,
                        modifier = Modifier
                            .heightIn(max = maxSheetHeight)
                            .testTag(SHIFT_SHEET_TEST_TAG),
                    )
                }
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
                .padding(start = PosSpacing.md, end = PosSpacing.xs, top = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LocalAppTextStyles.current.screenTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (supportingText.isNotBlank()) {
                    Text(
                        text = supportingText,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            ) {
                footer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftModeTabs(
    selectedTab: ShiftFormTab,
    onSelectTab: (ShiftFormTab) -> Unit,
) {
    val strings = LocalPosStrings.current
    val tabs = ShiftFormTab.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, tab ->
            val label = when (tab) {
                ShiftFormTab.OPERATOR -> strings.shiftOperatorTab
                ShiftFormTab.CLOSE -> strings.shiftCloseTab
            }
            val testTag = when (tab) {
                ShiftFormTab.OPERATOR -> SHIFT_OPERATOR_TAB_TEST_TAG
                ShiftFormTab.CLOSE -> SHIFT_CLOSE_TAB_TEST_TAG
            }
            SegmentedButton(
                selected = tab == selectedTab,
                onClick = { onSelectTab(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .heightIn(min = PosSizes.touchTarget)
                    .testTag(testTag),
                icon = {},
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun OpenShiftContent(
    state: ShiftUiState,
    actions: ShiftActions,
    onManageStaff: () -> Unit,
) {
    val strings = LocalPosStrings.current
    if (state.staffOptions.isEmpty()) {
        StaffEmptyState(
            title = strings.shiftNoStaffTitle,
            hint = strings.shiftNoStaffHint,
            onManageStaff = onManageStaff,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            PosSectionHeader(title = strings.shiftCashierNameLabel)
            CashierPicker(
                staff = state.staffOptions,
                selectedStaffId = state.selectedStaffId,
                onSelect = actions.onSelectStaff,
                modifier = Modifier.testTag(SHIFT_CASHIER_NAME_FIELD_TEST_TAG),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
        PosSectionHeader(title = strings.shiftStartingCashLabel)
        MoneyField(
            value = state.startingCashInput,
            onValueChange = actions.onStartingCashChange,
            label = strings.shiftStartingCashLabel,
            supportingText = strings.shiftStartingCashHint,
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
private fun OperatorContent(
    state: ShiftUiState,
    actions: ShiftActions,
    otherStaff: List<Staff>,
    onManageStaff: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
        PosSectionHeader(title = strings.shiftCurrentCashierTitle)
        CurrentCashierRow(name = activeOperatorName(state))
    }
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
        PosSectionHeader(title = strings.shiftSelectReplacementTitle)
        if (otherStaff.isEmpty()) {
            StaffEmptyState(
                title = strings.shiftNoOtherStaffTitle,
                hint = strings.shiftNoOtherStaffHint,
                onManageStaff = onManageStaff,
            )
        } else {
            CashierPicker(
                staff = otherStaff,
                selectedStaffId = state.selectedStaffId,
                onSelect = actions.onSelectStaff,
            )
        }
    }
}

@Composable
private fun CloseShiftContent(
    state: ShiftUiState,
    actions: ShiftActions,
    otherStaff: List<Staff>,
    isHandoverEnabled: Boolean,
    onHandoverChange: (Boolean) -> Unit,
) {
    val strings = LocalPosStrings.current
    ShiftSummarySection(
        summary = state.summary,
        isLoading = state.isSummaryLoading,
        errorMessage = state.summaryErrorMessage?.asTextOrNull(),
    )
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
    HandoverSection(
        state = state,
        actions = actions,
        otherStaff = otherStaff,
        isHandoverEnabled = isHandoverEnabled,
        onHandoverChange = onHandoverChange,
    )
}

@Composable
private fun ShiftSummarySection(
    summary: ShiftCashSummary?,
    isLoading: Boolean,
    errorMessage: String?,
) {
    val strings = LocalPosStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PosSectionHeader(
                    title = strings.shiftSummaryTitle,
                    supportingText = summary?.let {
                        strings.shiftTransactionCountLabel(it.transactionCount)
                    },
                    modifier = Modifier.weight(1f),
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(PosSizes.iconSmall),
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
                    if (summary.cashDebtPayments > 0L) {
                        ShiftMoneyRow(
                            label = strings.shiftCashDebtPaymentsLabel,
                            value = Rupiah.format(summary.cashDebtPayments),
                        )
                    }
                    if (summary.cashRefunds > 0L) {
                        ShiftMoneyRow(
                            label = strings.shiftCashRefundsLabel,
                            value = "-${Rupiah.format(summary.cashRefunds)}",
                        )
                    }
                    HorizontalDivider()
                    ShiftMoneyRow(
                        label = strings.shiftExpectedEndingCashLabel,
                        value = Rupiah.format(summary.expectedEndingCash),
                        isEmphasized = true,
                    )
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
}

@Composable
private fun HandoverSection(
    state: ShiftUiState,
    actions: ShiftActions,
    otherStaff: List<Staff>,
    isHandoverEnabled: Boolean,
    onHandoverChange: (Boolean) -> Unit,
) {
    val strings = LocalPosStrings.current
    val hasOtherStaff = otherStaff.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
        PosSectionHeader(title = strings.shiftNextCashierTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PosSizes.touchTarget)
                .toggleable(
                    value = isHandoverEnabled,
                    enabled = hasOtherStaff,
                    role = Role.Switch,
                    onValueChange = onHandoverChange,
                )
                .testTag(SHIFT_HANDOVER_TOGGLE_TEST_TAG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.shiftHandoverToggleLabel,
                    style = LocalAppTextStyles.current.body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = strings.shiftHandoverToggleHint,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isHandoverEnabled,
                onCheckedChange = null,
                enabled = hasOtherStaff,
            )
        }
        if (!hasOtherStaff) {
            Text(
                text = strings.shiftNoOtherStaffHint,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isHandoverEnabled && hasOtherStaff) {
            CashierPicker(
                staff = otherStaff,
                selectedStaffId = state.selectedNextStaffId,
                onSelect = actions.onSelectNextStaff,
            )
            MoneyField(
                value = state.nextStartingCashInput,
                onValueChange = actions.onNextStartingCashChange,
                label = strings.shiftNextStartingCashLabel,
                supportingText = strings.shiftNextStartingCashHint,
                imeAction = ImeAction.Done,
            )
        }
    }
}

@Composable
private fun CurrentCashierRow(name: String) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PosSizes.touchTarget)
            .padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        PersonAvatar(name = name, size = CASHIER_AVATAR_SIZE)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
        ) {
            Text(
                text = name,
                style = LocalAppTextStyles.current.body,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OnDutyChip(label = strings.staffOnDutyLabel)
        }
    }
}

@Composable
private fun OnDutyChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_circle_check),
                contentDescription = null,
                modifier = Modifier.size(ON_DUTY_CHIP_ICON_SIZE),
            )
            Text(
                text = label,
                style = LocalAppTextStyles.current.supportingLabel,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CashierPicker(
    staff: List<Staff>,
    selectedStaffId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        staff.forEachIndexed { index, member ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            val isSelected = member.id == selectedStaffId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            Color.Transparent
                        },
                        shape = shape,
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(member.id) },
                    )
                    .heightIn(min = PosSizes.touchTarget)
                    .testTag(SHIFT_STAFF_ROW_TEST_TAG_PREFIX + member.id)
                    .padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                PersonAvatar(name = member.name, size = CASHIER_AVATAR_SIZE)
                Text(
                    text = member.name,
                    style = LocalAppTextStyles.current.body,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RadioButton(selected = isSelected, onClick = null)
            }
        }
    }
}

@Composable
private fun StaffEmptyState(
    title: String,
    hint: String,
    onManageStaff: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = title,
            style = LocalAppTextStyles.current.body,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = hint,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onManageStaff,
            modifier = Modifier.heightIn(min = PosSizes.touchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_users),
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconSmall),
            )
            Text(
                text = strings.shiftManageStaffAction,
                style = LocalAppTextStyles.current.buttonLabel,
                modifier = Modifier.padding(start = PosSpacing.xs),
            )
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
        textStyle = LocalAppTextStyles.current.financialValue.copy(fontSize = MONEY_FIELD_TEXT_SIZE),
        visualTransformation = ThousandSeparatorVisualTransformation,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
    )
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
internal fun VarianceConfirmation(
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
private fun ShiftSheetFooter(
    state: ShiftUiState,
    actions: ShiftActions,
    isOpening: Boolean,
    selectedTab: ShiftFormTab,
    isHandoverEnabled: Boolean,
) {
    when {
        isOpening -> OpenShiftFooter(state = state, actions = actions)

        selectedTab == ShiftFormTab.OPERATOR -> OperatorFooter(state = state, actions = actions)

        else -> CloseShiftFooter(
            state = state,
            actions = actions,
            isHandoverEnabled = isHandoverEnabled,
        )
    }
}

@Composable
private fun OpenShiftFooter(
    state: ShiftUiState,
    actions: ShiftActions,
) {
    val strings = LocalPosStrings.current
    PrimaryActionButton(
        text = strings.shiftOpenSubmit,
        onClick = actions.onOpenShift,
        enabled = state.isOpenInputReady,
        loading = state.isSubmitting,
        modifier = Modifier.testTag(SHIFT_PRIMARY_ACTION_TEST_TAG),
    )
}

@Composable
private fun OperatorFooter(
    state: ShiftUiState,
    actions: ShiftActions,
) {
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val pinLabel = strings.securityAuditActionLabel(CriticalAction.SWITCH_CASHIER)
    PrimaryActionButton(
        text = strings.shiftSwitchOperatorAction,
        onClick = {
            val staffId = state.selectedStaffId ?: return@PrimaryActionButton
            gate.request(CriticalAction.SWITCH_CASHIER, pinLabel) {
                actions.onSwitchOperator(staffId)
            }
        },
        enabled = state.isOperatorSwitchReady && state.selectedStaffId != null,
        loading = state.isSubmitting,
        modifier = Modifier.testTag(SHIFT_PRIMARY_ACTION_TEST_TAG),
    )
}

@Composable
private fun CloseShiftFooter(
    state: ShiftUiState,
    actions: ShiftActions,
    isHandoverEnabled: Boolean,
) {
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val pinLabel = strings.securityAuditActionLabel(CriticalAction.SWITCH_CASHIER)
    PrimaryActionButton(
        text = if (isHandoverEnabled) strings.shiftCloseAndSwitch else strings.shiftCloseOnly,
        onClick = {
            gate.request(CriticalAction.SWITCH_CASHIER, pinLabel) {
                if (isHandoverEnabled) {
                    actions.onSwitchShift()
                } else {
                    actions.onCloseShift()
                }
            }
        },
        enabled = if (isHandoverEnabled) state.isSwitchInputReady else state.isCloseInputReady,
        loading = state.isSubmitting,
        modifier = Modifier.testTag(SHIFT_PRIMARY_ACTION_TEST_TAG),
    )
}
