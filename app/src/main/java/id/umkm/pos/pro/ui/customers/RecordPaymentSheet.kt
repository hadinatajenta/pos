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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.asText
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R

private const val RECORD_PAYMENT_SHEET_HEIGHT_FRACTION = 0.92f
private val TRAILING_VALUE_MAX_WIDTH = 176.dp
private val TOTAL_VALUE_MAX_WIDTH = 208.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordPaymentSheet(
    draft: RecordPaymentState,
    isSubmitting: Boolean,
    actions: CustomersActions,
) {
    val configuration = LocalConfiguration.current
    val strings = LocalPosStrings.current
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
                        text = strings.customersRecordPayment,
                        style = LocalAppTextStyles.current.screenTitle,
                    )
                    Text(
                        text = draft.customerName,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = actions.onCloseRecordPayment) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = strings.customersClosePaymentForm,
                    )
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
                OutstandingDebtCard(
                    outstandingDebt = draft.outstandingDebt,
                    strings = strings,
                )
                if (draft.transactions.isEmpty()) {
                    Text(
                        text = strings.customersNoDebtToPay,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    PosCard(modifier = Modifier.fillMaxWidth()) {
                        PosSectionHeader(title = strings.customersSelectDebt)
                        draft.transactions.forEach { item ->
                            TransactionOptionRow(
                                item = item,
                                isSelected = item.transactionId == draft.selectedTransactionId,
                                onSelect = { actions.onSelectTransaction(item.transactionId) },
                                strings = strings,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = draft.amountInput,
                    onValueChange = actions.onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.cashierAmountReceived) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PaymentShortcut(
                    label = strings.customersPayFullShortcut,
                    remainingDebt = draft.transactions
                        .firstOrNull { it.transactionId == draft.selectedTransactionId }
                        ?.remainingDebt,
                    onApply = actions.onAmountChange,
                )
                PaymentMethodSelector(
                    selectedMethod = draft.method,
                    onSelect = actions.onPaymentMethodChange,
                    strings = strings,
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = actions.onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.customersNote) },
                )
                draft.validationMessage?.let { message ->
                    Text(
                        text = message.asText(),
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            HorizontalDivider()
            PrimaryActionButton(
                text = strings.customersConfirmPayment,
                onClick = actions.onConfirmPayment,
                enabled = draft.transactions.isNotEmpty(),
                loading = isSubmitting,
                modifier = Modifier.padding(PosSpacing.md),
            )
        }
    }
}

@Composable
private fun OutstandingDebtCard(
    outstandingDebt: Long,
    strings: PosStrings,
) {
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = strings.customersOutstandingDebt,
                style = LocalAppTextStyles.current.sectionHeading,
            )
            AutoSizeNumberText(
                text = Rupiah.format(outstandingDebt),
                style = LocalAppTextStyles.current.financialValueLarge,
                modifier = Modifier.widthIn(max = TOTAL_VALUE_MAX_WIDTH),
            )
        }
    }
}

@Composable
private fun TransactionOptionRow(
    item: OutstandingTransactionItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    strings: PosStrings,
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
                text = dueDateText(item, strings),
                style = LocalAppTextStyles.current.supportingLabel,
                color = if (item.isOverdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        AutoSizeNumberText(
            text = Rupiah.format(item.remainingDebt),
            style = LocalAppTextStyles.current.financialValue,
            modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
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
    strings: PosStrings,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        PosSectionHeader(title = strings.cashierPaymentMethodLabel)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            PaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = method == selectedMethod,
                    onClick = { onSelect(method) },
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
}

private fun dueDateText(item: OutstandingTransactionItem, strings: PosStrings): String {
    val dueDate = item.dueDateMillis
    return when {
        dueDate == null -> strings.customersNoDueDate
        item.isOverdue -> strings.customersOverdueSince(formatDate(dueDate, strings))
        else -> strings.customersDueDateOn(formatDate(dueDate, strings))
    }
}
