package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.umkm.pos.pro.feature.reports.ReportDates
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.feature.reports.TransactionDetail
import id.umkm.pos.pro.feature.reports.TransactionDetailLine
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing
import java.util.TimeZone

@Composable
internal fun TransactionDetailDialog(
    detail: TransactionDetail?,
    isDetailLoading: Boolean,
    timeZone: TimeZone,
    onDismiss: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DETAIL_DIALOG_MAX_WIDTH)
                .heightIn(max = DETAIL_DIALOG_MAX_HEIGHT),
        ) {
            if (detail == null) {
                if (isDetailLoading) {
                    LoadingState(label = strings.transactionsLoadingDetail)
                }
            } else {
                TransactionDetailContent(detail = detail, timeZone = timeZone, onDismiss = onDismiss)
            }
        }
    }
}

private val DETAIL_DIALOG_MAX_WIDTH = 520.dp
private val DETAIL_DIALOG_MAX_HEIGHT = 640.dp

@Composable
private fun TransactionDetailContent(
    detail: TransactionDetail,
    timeZone: TimeZone,
    onDismiss: () -> Unit,
) {
    val transaction = detail.transaction
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PosSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.transactionsDetailTitle,
                    style = LocalAppTextStyles.current.sectionHeading,
                )
                Text(
                    text = transaction.invoiceNo,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TransactionStatusChip(status = transaction.status)
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            DetailInfoRow(
                label = strings.transactionsDate,
                value = ReportDates.formatLocalDateTimeShort(transaction.createdAt, timeZone, strings),
            )
            detail.cashierName?.takeIf { name -> name.isNotBlank() }?.let { cashierName ->
                DetailInfoRow(label = strings.transactionsFieldCashier, value = cashierName)
            }
            detail.customerName?.takeIf { name -> name.isNotBlank() }?.let { customerName ->
                DetailInfoRow(label = strings.transactionsFieldCustomer, value = customerName)
            }
            DetailInfoRow(
                label = strings.transactionsFieldPaymentMethod,
                value = paymentMethodLabel(transaction.paymentMethod, strings),
            )
            HorizontalDivider()
            Text(
                text = strings.transactionsItemsTitle,
                style = LocalAppTextStyles.current.tableHeader,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            detail.lines.forEach { line -> DetailLineRow(line = line) }
            HorizontalDivider()
            DetailAmountRow(label = strings.transactionsFieldSubtotal, value = transaction.subtotal)
            if (transaction.invoiceDiscount > 0L) {
                DetailAmountRow(label = strings.transactionsFieldDiscount, value = -transaction.invoiceDiscount)
            }
            if (transaction.taxTotal > 0L) {
                DetailAmountRow(label = strings.transactionsFieldTax, value = transaction.taxTotal)
            }
            if (transaction.rounding != 0L) {
                DetailAmountRow(label = strings.transactionsFieldRounding, value = transaction.rounding)
            }
            DetailAmountRow(
                label = strings.transactionsFieldTotal,
                value = transaction.grandTotal,
                isEmphasized = true,
            )
            DetailAmountRow(label = strings.transactionsFieldPaid, value = transaction.paidAmount)
            if (transaction.changeAmount > 0L) {
                DetailAmountRow(label = strings.transactionsFieldChange, value = transaction.changeAmount)
            }
            if (transaction.remainingDebt > 0L) {
                DetailAmountRow(label = strings.transactionsFieldRemainingDebt, value = transaction.remainingDebt)
            }
            transaction.dueDate?.let { dueDate ->
                DetailInfoRow(label = strings.transactionsFieldDueDate, value = formatDate(dueDate, strings))
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onDismiss) {
                Text(text = strings.actionClose, style = LocalAppTextStyles.current.buttonLabel)
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = LocalAppTextStyles.current.body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailLineRow(line: TransactionDetailLine) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
    ) {
        Text(
            text = line.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${line.quantityLabel} × ${Rupiah.format(line.unitPrice)}",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Rupiah.format(line.lineTotal),
                style = LocalAppTextStyles.current.catalogItemPrice,
                maxLines = 1,
            )
        }
        if (line.itemDiscount > 0L) {
            Text(
                text = LocalPosStrings.current.transactionsItemDiscountLine(Rupiah.format(line.itemDiscount)),
                style = LocalAppTextStyles.current.supportingLabel,
                color = LocalPosSemanticPalette.current.danger,
            )
        }
    }
}

@Composable
private fun DetailAmountRow(
    label: String,
    value: Long,
    isEmphasized: Boolean = false,
) {
    val valueColor: Color = if (value < 0L) {
        LocalPosSemanticPalette.current.danger
    } else {
        Color.Unspecified
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (isEmphasized) {
                MaterialTheme.typography.titleSmall
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
        Text(
            text = Rupiah.format(value),
            style = if (isEmphasized) {
                LocalAppTextStyles.current.financialValue
            } else {
                LocalAppTextStyles.current.catalogItemPrice
            },
            color = valueColor,
            maxLines = 1,
        )
    }
}
