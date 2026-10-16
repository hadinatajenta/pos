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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.SectionHeading
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val CUSTOMER_LIST_MAX_HEIGHT_DP = 320

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierPaymentSheet(
    state: CashierUiState,
    actions: CashierActions,
) {
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
            SectionHeading(text = "Payment")
            PaymentTotal(grandTotal = state.grandTotal)
            PaymentMethodChips(
                selectedMethod = state.paymentDraft.method,
                onSelect = actions.onPaymentMethodChange,
            )
            PaymentConditionalFields(state = state, actions = actions)
            PaymentSummary(state = state)
            state.submitErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = actions.onConfirmCharge,
                enabled = !state.isSubmitting && state.lineCount > 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = PosSizes.touchTarget),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(PosSizes.iconSmall),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = "Confirm payment", style = LocalAppTextStyles.current.buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun PaymentTotal(grandTotal: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Total", style = LocalAppTextStyles.current.sectionHeading)
        Text(
            text = Rupiah.format(grandTotal),
            style = LocalAppTextStyles.current.financialValueLarge,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodChips(
    selectedMethod: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        PaymentMethod.entries.forEach { method ->
            FilterChip(
                selected = method == selectedMethod,
                onClick = { onSelect(method) },
                label = { Text(paymentMethodLabel(method)) },
                leadingIcon = if (method == selectedMethod) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
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
    when (state.paymentDraft.method) {
        PaymentMethod.CASH -> AmountReceivedField(
            value = state.paymentDraft.amountReceivedInput,
            onValueChange = actions.onAmountReceivedChange,
            label = "Amount received",
        )

        PaymentMethod.SPLIT -> AmountReceivedField(
            value = state.paymentDraft.amountReceivedInput,
            onValueChange = actions.onAmountReceivedChange,
            label = "Amount received",
        )

        PaymentMethod.KASBON -> {
            CustomerField(state = state, actions = actions)
            AmountReceivedField(
                value = state.paymentDraft.amountReceivedInput,
                onValueChange = actions.onAmountReceivedChange,
                label = "Deposit received",
                supportingText = "Leave empty when nothing is paid now.",
            )
            if (expectedRemainingAmount(state) > 0L) {
                DueDateField(state = state, actions = actions)
            }
        }

        PaymentMethod.QRIS, PaymentMethod.TRANSFER -> Unit
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
    state: CashierUiState,
    actions: CashierActions,
) {
    OutlinedButton(
        onClick = actions.onOpenCustomerPicker,
        modifier = Modifier.fillMaxWidth().heightIn(min = PosSizes.touchTarget),
    ) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(PosSpacing.sm))
        Text(
            text = state.selectedCustomerName ?: "Choose customer",
            style = LocalAppTextStyles.current.buttonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DueDateField(
    state: CashierUiState,
    actions: CashierActions,
) {
    val dueDateMillis = state.paymentDraft.dueDateMillis
    OutlinedButton(
        onClick = actions.onOpenDueDatePicker,
        modifier = Modifier.fillMaxWidth().heightIn(min = PosSizes.touchTarget),
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(PosSpacing.sm))
        Text(
            text = dueDateMillis?.let { millis -> "Due ${formatDate(millis)}" } ?: "Set due date",
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
        SummaryLine(label = "Total", value = Rupiah.format(grandTotal))
        if (paidAmount != null && paidAmount != grandTotal) {
            SummaryLine(label = "Paid", value = Rupiah.format(paidAmount))
        }
        if (changeAmount > 0L) {
            SummaryLine(label = "Change", value = Rupiah.format(changeAmount), emphasized = true)
        }
        if (remainingAmount > 0L) {
            SummaryLine(label = "Remaining balance", value = Rupiah.format(remainingAmount))
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
        Text(
            text = value,
            style = if (emphasized) {
                LocalAppTextStyles.current.financialValue
            } else {
                LocalAppTextStyles.current.body
            },
        )
    }
}

@Composable
internal fun CashierCustomerPicker(
    state: CashierUiState,
    actions: CashierActions,
) {
    if (!state.isCustomerPickerVisible) return
    AlertDialog(
        onDismissRequest = actions.onCloseCustomerPicker,
        title = { Text(text = "Choose customer") },
        text = {
            if (state.payableCustomers.isEmpty()) {
                Text(
                    text = "No customers yet. Add a customer before selling on kasbon.",
                    style = LocalAppTextStyles.current.supportingLabel,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = CUSTOMER_LIST_MAX_HEIGHT_DP.dp)) {
                    items(items = state.payableCustomers, key = { customer -> customer.id }) { customer ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actions.onCustomerSelected(customer.id) }
                                .padding(vertical = PosSpacing.sm),
                        ) {
                            Text(text = customer.name, style = LocalAppTextStyles.current.body)
                            Text(
                                text = "Debt ${Rupiah.format(customer.totalDebt)}",
                                style = LocalAppTextStyles.current.supportingLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onCloseCustomerPicker) {
                Text(text = "Cancel")
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
                Text(text = "Set due date")
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCloseDueDatePicker) {
                Text(text = "Cancel")
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
