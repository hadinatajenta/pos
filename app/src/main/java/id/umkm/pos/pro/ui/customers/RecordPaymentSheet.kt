package id.umkm.pos.pro.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.SectionHeading
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val RECORD_PAYMENT_SHEET_HEIGHT_FRACTION = 0.92f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordPaymentSheet(
    draft: RecordPaymentState,
    isSubmitting: Boolean,
    actions: CustomersActions,
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * RECORD_PAYMENT_SHEET_HEIGHT_FRACTION).dp
    ModalBottomSheet(
        onDismissRequest = actions.onCloseRecordPayment,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Record payment",
                        style = LocalAppTextStyles.current.screenTitle,
                    )
                    Text(
                        text = draft.customerName,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = actions.onCloseRecordPayment) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close payment form")
                }
            }
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = PosSpacing.md, vertical = PosSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
            ) {
                OutstandingDebtRow(outstandingDebt = draft.outstandingDebt)
                if (draft.transactions.isEmpty()) {
                    Text(
                        text = "This customer has no outstanding debt to pay.",
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SectionHeading(text = "Debt to pay")
                    draft.transactions.forEach { item ->
                        TransactionOptionRow(
                            item = item,
                            isSelected = item.transactionId == draft.selectedTransactionId,
                            onSelect = { actions.onSelectTransaction(item.transactionId) },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.amountInput,
                    onValueChange = actions.onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount received") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PaymentShortcut(
                    label = "Pay selected debt in full",
                    remainingDebt = draft.transactions
                        .firstOrNull { it.transactionId == draft.selectedTransactionId }
                        ?.remainingDebt,
                    onApply = actions.onAmountChange,
                )
                PaymentMethodSelector(
                    selectedMethod = draft.method,
                    onSelect = actions.onPaymentMethodChange,
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = actions.onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                )
                draft.validationMessage?.let { message ->
                    Text(
                        text = message,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            HorizontalDivider()
            Button(
                onClick = actions.onConfirmPayment,
                enabled = !isSubmitting && draft.transactions.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PosSpacing.md)
                    .heightIn(min = PosSizes.touchTarget),
            ) {
                if (isSubmitting) {
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
private fun OutstandingDebtRow(outstandingDebt: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Outstanding",
            style = LocalAppTextStyles.current.sectionHeading,
        )
        Text(
            text = Rupiah.format(outstandingDebt),
            style = LocalAppTextStyles.current.financialValueLarge,
        )
    }
}

@Composable
private fun TransactionOptionRow(
    item: OutstandingTransactionItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.invoiceNo, style = LocalAppTextStyles.current.body)
            Text(
                text = dueDateText(item),
                style = LocalAppTextStyles.current.supportingLabel,
                color = if (item.isOverdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = Rupiah.format(item.remainingDebt),
            style = LocalAppTextStyles.current.financialValue,
        )
    }
}

@Composable
private fun PaymentShortcut(
    label: String,
    remainingDebt: Long?,
    onApply: (String) -> Unit,
) {
    val debt = remainingDebt ?: return
    if (debt <= 0L) return
    TextButton(onClick = { onApply(debt.toString()) }) {
        Text(text = label)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        SectionHeading(text = "Payment method")
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
}

private fun dueDateText(item: OutstandingTransactionItem): String {
    val dueDate = item.dueDateMillis
    return when {
        dueDate == null -> "No due date"
        item.isOverdue -> "Overdue since ${formatDate(dueDate)}"
        else -> "Due ${formatDate(dueDate)}"
    }
}
