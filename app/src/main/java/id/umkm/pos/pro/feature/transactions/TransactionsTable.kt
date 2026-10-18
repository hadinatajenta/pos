package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.feature.reports.ReportDates
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.PosTextStyles
import java.util.TimeZone

private val TABLE_NUMBER_WIDTH = 40.dp
private val TABLE_ACTION_WIDTH = 48.dp
private val COMPACT_VALUE_MAX_WIDTH = 168.dp
private val TABLE_MONEY_TEXT_STYLE = PosTextStyles.financialValue.copy(fontSize = 15.sp, lineHeight = 20.sp)
private const val TABLE_ID_WEIGHT = 1.6f
private const val TABLE_DATE_WEIGHT = 1.4f
private const val TABLE_METHOD_WEIGHT = 1.0f
private const val TABLE_STATUS_WEIGHT = 1.1f
private const val TABLE_TOTAL_WEIGHT = 1.2f
private const val ROW_DIVIDER_ALPHA = 0.5f

@Composable
internal fun TransactionsTable(
    transactions: List<Transaction>,
    timeZone: TimeZone,
    actions: TransactionActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.lg, vertical = PosSpacing.xs),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TransactionTableHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = ROW_DIVIDER_ALPHA))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(
                    items = transactions,
                    key = { _, transaction -> transaction.id },
                ) { index, transaction ->
                    TransactionTableRow(
                        position = index + 1,
                        transaction = transaction,
                        timeZone = timeZone,
                        actions = actions,
                    )
                    if (index < transactions.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = ROW_DIVIDER_ALPHA),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionTableHeader() {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(start = PosSpacing.md, end = PosSpacing.xs, top = PosSpacing.sm, bottom = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        TableHeaderCell(text = strings.transactionsTableNumber, modifier = Modifier.width(TABLE_NUMBER_WIDTH))
        TableHeaderCell(text = strings.transactionsTableId, modifier = Modifier.weight(TABLE_ID_WEIGHT))
        TableHeaderCell(text = strings.transactionsTableDateTime, modifier = Modifier.weight(TABLE_DATE_WEIGHT))
        TableHeaderCell(text = strings.transactionsTableMethod, modifier = Modifier.weight(TABLE_METHOD_WEIGHT))
        TableHeaderCell(text = strings.transactionsTableStatus, modifier = Modifier.weight(TABLE_STATUS_WEIGHT))
        TableHeaderCell(
            text = strings.transactionsTableTotal,
            modifier = Modifier.weight(TABLE_TOTAL_WEIGHT),
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.width(TABLE_ACTION_WIDTH))
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        style = LocalAppTextStyles.current.tableHeader,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
private fun TransactionTableRow(
    position: Int,
    transaction: Transaction,
    timeZone: TimeZone,
    actions: TransactionActions,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onOpenDetail(transaction.id) }
            .padding(start = PosSpacing.md, end = PosSpacing.xs, top = PosSpacing.sm, bottom = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Text(
            text = position.toString(),
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(TABLE_NUMBER_WIDTH),
        )
        Text(
            text = transaction.invoiceNo,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(TABLE_ID_WEIGHT),
        )
        Text(
            text = ReportDates.formatLocalDateTimeShort(transaction.createdAt, timeZone, strings),
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(TABLE_DATE_WEIGHT),
        )
        Text(
            text = paymentMethodLabel(transaction.paymentMethod, strings),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(TABLE_METHOD_WEIGHT),
        )
        Box(
            modifier = Modifier.weight(TABLE_STATUS_WEIGHT),
            contentAlignment = Alignment.CenterStart,
        ) {
            TransactionStatusChip(status = transaction.status)
        }
        Box(
            modifier = Modifier.weight(TABLE_TOTAL_WEIGHT),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AutoSizeNumberText(
                text = Rupiah.format(transaction.grandTotal),
                style = TABLE_MONEY_TEXT_STYLE,
                maxLines = 1,
            )
        }
        TransactionActionsButton(
            transaction = transaction,
            actions = actions,
            modifier = Modifier.width(TABLE_ACTION_WIDTH),
        )
    }
}

@Composable
internal fun TransactionsCompactList(
    transactions: List<Transaction>,
    timeZone: TimeZone,
    actions: TransactionActions,
    modifier: Modifier = Modifier,
) {
    var sheetTransaction by remember { mutableStateOf<Transaction?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        items(items = transactions, key = { transaction -> transaction.id }) { transaction ->
            TransactionCompactRow(
                transaction = transaction,
                timeZone = timeZone,
                actions = actions,
                onOpenMenu = { sheetTransaction = transaction },
            )
        }
    }
    sheetTransaction?.let { transaction ->
        TransactionActionsSheet(
            transaction = transaction,
            onDismiss = { sheetTransaction = null },
            actions = actions,
        )
    }
}

@Composable
private fun TransactionCompactRow(
    transaction: Transaction,
    timeZone: TimeZone,
    actions: TransactionActions,
    onOpenMenu: () -> Unit,
) {
    val strings = LocalPosStrings.current
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onOpenDetail(transaction.id) },
        contentPadding = PosSpacing.sm,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = transaction.invoiceNo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${ReportDates.formatLocalDateTimeShort(transaction.createdAt, timeZone, strings)} · " +
                        paymentMethodLabel(transaction.paymentMethod, strings),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(PosSpacing.sm))
            AutoSizeNumberText(
                text = Rupiah.format(transaction.grandTotal),
                style = TABLE_MONEY_TEXT_STYLE,
                modifier = Modifier.widthIn(max = COMPACT_VALUE_MAX_WIDTH),
            )
            TransactionActionsButton(
                transaction = transaction,
                actions = actions,
                isSheetStyle = true,
                onOpenSheet = onOpenMenu,
            )
        }
    }
}

