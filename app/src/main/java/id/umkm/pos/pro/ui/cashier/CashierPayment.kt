package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import id.umkm.pos.pro.domain.qris.QrisPayload
import id.umkm.pos.pro.ui.common.renderQrBitmap
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette

private const val CUSTOMER_LIST_MAX_HEIGHT_DP = 320
private const val CUSTOMER_SEARCH_THRESHOLD = 4
private const val MAX_QUICK_CASH_SUGGESTIONS = 3
private val TRAILING_VALUE_MAX_WIDTH = 176.dp
private val TOTAL_VALUE_MAX_WIDTH = 208.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierPaymentSheet(
    state: CashierUiState,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    ModalBottomSheet(
        onDismissRequest = actions.onClosePaymentSheet,
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
            PosSectionHeader(title = strings.cashierPaymentTitle)
            PosCard(modifier = Modifier.fillMaxWidth()) {
                PaymentTotal(grandTotal = state.grandTotal)
                HorizontalDivider()
                PaymentMethodChips(
                    selectedMethod = state.paymentDraft.method,
                    onSelect = actions.onPaymentMethodChange,
                    isQrisAvailable = state.qrisPayload != null,
                )
                PaymentConditionalFields(state = state, actions = actions)
                PaymentSummary(state = state)
            }
            val submitErrorMessage = state.submitErrorMessage?.asTextOrNull()
            if (submitErrorMessage != null) {
                Text(
                    text = submitErrorMessage,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PrimaryActionButton(
                text = strings.cashierConfirmPayment,
                onClick = actions.onConfirmCharge,
                enabled = state.lineCount > 0,
                loading = state.isSubmitting,
            )
        }
    }
}

@Composable
private fun PaymentTotal(grandTotal: Long) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = strings.cashierTotal, style = LocalAppTextStyles.current.sectionHeading)
        AutoSizeNumberText(
            text = Rupiah.format(grandTotal),
            style = LocalAppTextStyles.current.financialValueLarge,
            modifier = Modifier.widthIn(max = TOTAL_VALUE_MAX_WIDTH),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodChips(
    selectedMethod: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
    isQrisAvailable: Boolean,
) {
    val strings = LocalPosStrings.current
    Text(
        text = strings.cashierPaymentMethodLabel,
        style = LocalAppTextStyles.current.supportingLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        PaymentMethod.entries.forEach { method ->
            FilterChip(
                selected = method == selectedMethod,
                onClick = { onSelect(method) },
                enabled = method != PaymentMethod.QRIS || isQrisAvailable,
                label = { Text(paymentMethodLabel(method, strings)) },
                leadingIcon = if (method == selectedMethod) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(PosSizes.iconSmall),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun PaymentConditionalFields(
    state: CashierUiState,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    val draft = state.paymentDraft
    when (draft.method) {
        PaymentMethod.CASH -> {
            QuickCashChipsRow(
                grandTotal = state.grandTotal,
                currentInput = draft.amountReceivedInput,
                onSelectAmount = { actions.onAmountReceivedChange(it.toString()) },
            )
            AmountReceivedField(
                value = draft.amountReceivedInput,
                onValueChange = actions.onAmountReceivedChange,
                label = strings.cashierAmountReceived,
            )
        }

        PaymentMethod.SPLIT -> {
            CustomerField(
                customerName = state.selectedCustomerName,
                onClick = actions.onOpenCustomerPicker,
            )
            QuickPartialChipsRow(
                grandTotal = state.grandTotal,
                currentInput = draft.amountReceivedInput,
                onSelectAmount = { actions.onAmountReceivedChange(it.toString()) },
            )
            AmountReceivedField(
                value = draft.amountReceivedInput,
                onValueChange = actions.onAmountReceivedChange,
                label = strings.cashierAmountPaidNow,
            )
            if (expectedRemainingAmount(state) > 0L) {
                DueDateField(
                    dueDateMillis = draft.dueDateMillis,
                    onClick = actions.onOpenDueDatePicker,
                )
            }
        }

        PaymentMethod.KASBON -> {
            CustomerField(
                customerName = state.selectedCustomerName,
                onClick = actions.onOpenCustomerPicker,
            )
            AmountReceivedField(
                value = draft.amountReceivedInput,
                onValueChange = actions.onAmountReceivedChange,
                label = strings.cashierDepositReceived,
                supportingText = strings.cashierDepositOptionalHint,
            )
            if (expectedRemainingAmount(state) > 0L) {
                DueDateField(
                    dueDateMillis = draft.dueDateMillis,
                    onClick = actions.onOpenDueDatePicker,
                )
            }
        }

        PaymentMethod.QRIS -> QrisPaymentSection(
            payload = state.qrisPayload,
            amountRupiah = state.grandTotal,
        )

        PaymentMethod.TRANSFER -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCashChipsRow(
    grandTotal: Long,
    currentInput: String,
    onSelectAmount: (Long) -> Unit,
) {
    if (grandTotal <= 0L) return
    val strings = LocalPosStrings.current
    val suggestions = remember(grandTotal) { quickCashSuggestions(grandTotal) }
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = strings.cashierQuickNominal,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            FilterChip(
                selected = currentInput == grandTotal.toString(),
                onClick = { onSelectAmount(grandTotal) },
                label = { Text(strings.cashierExactMoney) },
            )
            suggestions.forEach { amount ->
                FilterChip(
                    selected = currentInput == amount.toString(),
                    onClick = { onSelectAmount(amount) },
                    label = { Text(Rupiah.format(amount)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPartialChipsRow(
    grandTotal: Long,
    currentInput: String,
    onSelectAmount: (Long) -> Unit,
) {
    val suggestions = remember(grandTotal) { quickPartialSuggestions(grandTotal) }
    if (suggestions.isEmpty()) return
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = strings.cashierQuickNominalOptions,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            suggestions.forEach { amount ->
                FilterChip(
                    selected = currentInput == amount.toString(),
                    onClick = { onSelectAmount(amount) },
                    label = { Text(Rupiah.format(amount)) },
                )
            }
        }
    }
}

private fun quickCashSuggestions(grandTotal: Long): List<Long> {
    if (grandTotal <= 0L) return emptyList()
    val candidates = buildList {
        addAll(listOf(5_000L, 10_000L, 20_000L, 50_000L, 100_000L))
        listOf(5_000L, 10_000L, 20_000L, 50_000L, 100_000L).forEach { step ->
            val nextMultiple = ((grandTotal / step) + 1L) * step
            add(nextMultiple)
        }
    }
    return candidates
        .filter { it > grandTotal }
        .distinct()
        .sorted()
        .take(MAX_QUICK_CASH_SUGGESTIONS)
}

private fun quickPartialSuggestions(grandTotal: Long): List<Long> {
    if (grandTotal <= 1_000L) return emptyList()
    val halfRounded = ((grandTotal / 2L) / 1_000L) * 1_000L
    val candidates = buildList {
        if (halfRounded > 0L) add(halfRounded)
        addAll(listOf(10_000L, 20_000L, 50_000L, 100_000L))
    }
    return candidates
        .filter { it in 1L until grandTotal }
        .distinct()
        .sorted()
        .take(MAX_QUICK_CASH_SUGGESTIONS)
}

private val QR_PAYMENT_SIZE = 220.dp
private const val QR_RENDER_SIZE_PX = 720

@Composable
private fun QrisPaymentSection(
    payload: String?,
    amountRupiah: Long,
) {
    val palette = LocalPosSemanticPalette.current
    val strings = LocalPosStrings.current
    if (payload == null) {
        Text(
            text = strings.cashierQrisNotConfigured,
            style = LocalAppTextStyles.current.supportingLabel,
            color = palette.danger,
        )
        return
    }
    val qrImage by produceState<ImageBitmap?>(initialValue = null, payload, amountRupiah) {
        value = withContext(Dispatchers.Default) {
            QrisPayload.withAmount(payload, amountRupiah)
                ?.let { dynamicPayload -> renderQrBitmap(dynamicPayload, QR_RENDER_SIZE_PX)?.asImageBitmap() }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        PosCard(modifier = Modifier.size(QR_PAYMENT_SIZE)) {
            val image = qrImage
            if (image == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.cashierPreparingQris,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Image(
                    bitmap = image,
                    contentDescription = strings.cashierQrisDescription,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = Rupiah.format(amountRupiah),
            style = LocalAppTextStyles.current.financialValue,
        )
        Text(
            text = strings.cashierAmountAutoFilled,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AmountReceivedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = supportingText?.let { text -> { Text(text) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
    )
}

@Composable
private fun CustomerField(
    customerName: String?,
    onClick: () -> Unit,
) {
    val strings = LocalPosStrings.current
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = PosSizes.touchTarget),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_users),
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(PosSpacing.sm))
        Text(
            text = customerName ?: strings.cashierSelectCustomer,
            style = LocalAppTextStyles.current.buttonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DueDateField(
    dueDateMillis: Long?,
    onClick: () -> Unit,
) {
    val strings = LocalPosStrings.current
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = PosSizes.touchTarget),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(PosSpacing.sm))
        Text(
            text = dueDateMillis?.let { millis -> strings.cashierDueDateSet(formatDate(millis, strings)) }
                ?: strings.cashierSetDueDate,
            style = LocalAppTextStyles.current.buttonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PaymentSummary(state: CashierUiState) {
    val grandTotal = state.grandTotal
    val strings = LocalPosStrings.current
    val paidAmount = displayPaidAmount(state)
    val changeAmount = if (state.paymentDraft.method == PaymentMethod.CASH && paidAmount != null) {
        (paidAmount - grandTotal).coerceAtLeast(0L)
    } else {
        0L
    }
    val remainingAmount = if (paidAmount != null) (grandTotal - paidAmount).coerceAtLeast(0L) else 0L

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
    ) {
        if (paidAmount != null && paidAmount != grandTotal) {
            SummaryLine(label = strings.cashierPaid, value = Rupiah.format(paidAmount))
        }
        if (changeAmount > 0L) {
            SummaryLine(label = strings.cashierChange, value = Rupiah.format(changeAmount), emphasized = true)
        }
        if (remainingAmount > 0L) {
            SummaryLine(label = strings.cashierRemainingAmount, value = Rupiah.format(remainingAmount))
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
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
            style = if (emphasized) {
                LocalAppTextStyles.current.financialValue
            } else {
                LocalAppTextStyles.current.body
            },
            modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
        )
    }
}

@Composable
internal fun CashierCustomerPicker(
    state: CashierUiState,
    actions: CashierActions,
) {
    if (!state.isCustomerPickerVisible) return
    val strings = LocalPosStrings.current
    var isCreateOpen by rememberSaveable(state.payableCustomers.isEmpty()) {
        mutableStateOf(state.payableCustomers.isEmpty())
    }
    var newName by rememberSaveable { mutableStateOf("") }
    var newPhone by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredCustomers = remember(state.payableCustomers, searchQuery) {
        if (searchQuery.isBlank()) {
            state.payableCustomers
        } else {
            val trimmed = searchQuery.trim()
            state.payableCustomers.filter { customer ->
                customer.name.contains(trimmed, ignoreCase = true) ||
                    customer.whatsappNumber?.contains(trimmed, ignoreCase = true) == true
            }
        }
    }

    AlertDialog(
        onDismissRequest = actions.onCloseCustomerPicker,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = strings.cashierSelectCustomer,
                    modifier = Modifier.weight(1f),
                )
                if (state.payableCustomers.isNotEmpty()) {
                    SecondaryTextAction(
                        text = if (isCreateOpen) strings.actionClose else strings.cashierNewCustomerToggle,
                        onClick = { isCreateOpen = !isCreateOpen },
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                if (isCreateOpen) {
                    PosCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.cashierAddNewCustomer,
                            style = LocalAppTextStyles.current.body,
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.cashierCustomerNameLabel) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.cashierCustomerWhatsappLabel) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                            ),
                        )
                        PrimaryActionButton(
                            text = strings.cashierSaveAndSelectCustomer,
                            onClick = {
                                actions.onQuickCreateCustomer(newName.trim(), newPhone.trim())
                            },
                            enabled = newName.isNotBlank(),
                        )
                    }
                }

                if (state.payableCustomers.size > CUSTOMER_SEARCH_THRESHOLD) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.cashierSearchCustomerPlaceholder) },
                        singleLine = true,
                    )
                }

                if (state.payableCustomers.isEmpty()) {
                    Text(
                        text = strings.cashierNoCustomersHint,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (filteredCustomers.isEmpty()) {
                    Text(
                        text = strings.cashierCustomerNotFound,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = CUSTOMER_LIST_MAX_HEIGHT_DP.dp)) {
                        items(items = filteredCustomers, key = { customer -> customer.id }) { customer ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actions.onCustomerSelected(customer.id) }
                                    .padding(vertical = PosSpacing.sm),
                            ) {
                                Text(text = customer.name, style = LocalAppTextStyles.current.body)
                                val details = buildList {
                                    if (!customer.whatsappNumber.isNullOrBlank()) {
                                        add(customer.whatsappNumber)
                                    }
                                    add(strings.cashierCustomerDebtChip(Rupiah.format(customer.totalDebt)))
                                }.joinToString(" • ")
                                Text(
                                    text = details,
                                    style = LocalAppTextStyles.current.supportingLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onCloseCustomerPicker) {
                Text(text = strings.actionCancel)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierDueDatePicker(
    state: CashierUiState,
    actions: CashierActions,
) {
    if (!state.isDueDatePickerVisible) return
    val strings = LocalPosStrings.current
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.paymentDraft.dueDateMillis,
    )
    DatePickerDialog(
        onDismissRequest = actions.onCloseDueDatePicker,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        actions.onDueDateChange(selectedMillis)
                    } else {
                        actions.onCloseDueDatePicker()
                    }
                },
            ) {
                Text(text = strings.cashierSetDueDate)
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCloseDueDatePicker) {
                Text(text = strings.actionCancel)
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

internal fun displayPaidAmount(state: CashierUiState): Long? {
    val enteredAmount = state.paymentDraft.amountReceivedInput.toLongOrNull()
    return when (state.paymentDraft.method) {
        PaymentMethod.CASH, PaymentMethod.SPLIT -> enteredAmount
        PaymentMethod.KASBON -> enteredAmount ?: 0L
        PaymentMethod.QRIS, PaymentMethod.TRANSFER -> state.grandTotal
    }
}

internal fun expectedRemainingAmount(state: CashierUiState): Long {
    val paidAmount = displayPaidAmount(state) ?: return 0L
    return (state.grandTotal - paidAmount).coerceAtLeast(0L)
}
