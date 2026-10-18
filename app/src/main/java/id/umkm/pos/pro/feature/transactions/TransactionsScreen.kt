package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.feature.reports.ReportsFilterSheet
import id.umkm.pos.pro.feature.reports.ReportsRangePreset
import id.umkm.pos.pro.feature.reports.ReportsUiState
import id.umkm.pos.pro.feature.reports.TransactionStatusFilter
import id.umkm.pos.pro.feature.reports.label
import id.umkm.pos.pro.feature.reports.periodLabel
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.HeaderActionButton
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.components.PosSearchField
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import java.util.TimeZone

@Composable
internal fun TransactionsScreen(
    uiState: ReportsUiState,
    actions: TransactionActions,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onToggleStatusFilter: (TransactionStatusFilter) -> Unit,
    onSelectStatusFilter: (TransactionStatusFilter?) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onAddTransaction: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZone = remember { TimeZone.getDefault() }
    val strings = LocalPosStrings.current
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    val activePeriodLabel = periodLabel(uiState.preset, uiState.range, timeZone, strings)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= PosBreakpoints.twoPane
        val horizontalPadding = if (isExpanded) PosSpacing.lg else PosSpacing.md
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = strings.transactionsTitle,
                supportingText = strings.transactionsCountWithPeriod(
                    uiState.visibleTransactions.size,
                    activePeriodLabel,
                ),
                actions = {
                    IconButton(onClick = { isFilterSheetVisible = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = strings.transactionsFilterPeriod,
                        )
                    }
                    HeaderActionButton(
                        text = strings.actionAdd,
                        iconRes = R.drawable.ic_plus,
                        contentDescription = strings.transactionsAddAction,
                        onClick = onAddTransaction,
                    )
                },
            )
            PosSearchField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = strings.transactionsSearchPlaceholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = PosSpacing.xs,
                    ),
            )
            TransactionsFilterRow(
                transactions = uiState.transactions,
                statusFilter = uiState.statusFilter,
                periodText = activePeriodLabel,
                onOpenPeriodFilter = { isFilterSheetVisible = true },
                onSelectAll = { onSelectStatusFilter(null) },
                onToggleStatusFilter = onToggleStatusFilter,
                horizontalPadding = horizontalPadding,
            )
            if (uiState.isLoading && uiState.summary != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val error = uiState.error
                when {
                    error != null -> ErrorState(error = error, onRetry = onRefresh)
                    uiState.summary == null && uiState.isLoading ->
                        LoadingState(label = strings.transactionsLoading)

                    uiState.transactions.isEmpty() -> EmptyState(
                        iconRes = R.drawable.ic_receipt,
                        title = strings.transactionsEmptyTitle,
                        supportingText = strings.transactionsEmptyHint,
                    )

                    uiState.visibleTransactions.isEmpty() -> EmptyState(
                        iconRes = R.drawable.ic_receipt,
                        title = strings.transactionsNoMatchTitle,
                        supportingText = strings.transactionsNoMatchHint,
                        actionLabel = strings.transactionsShowAll,
                        onAction = {
                            onSelectStatusFilter(null)
                            onSearchQueryChange("")
                        },
                    )

                    isExpanded -> TransactionsTable(
                        transactions = uiState.visibleTransactions,
                        timeZone = timeZone,
                        actions = actions,
                    )

                    else -> TransactionsCompactList(
                        transactions = uiState.visibleTransactions,
                        timeZone = timeZone,
                        actions = actions,
                    )
                }
            }
        }
    }
    if (isFilterSheetVisible) {
        ReportsFilterSheet(
            preset = uiState.preset,
            range = uiState.range,
            timeZone = timeZone,
            onDismiss = { isFilterSheetVisible = false },
            onPresetSelected = onPresetSelected,
            onCustomRangeSelected = onCustomRangeSelected,
        )
    }
}

@Composable
private fun TransactionsFilterRow(
    transactions: List<Transaction>,
    statusFilter: TransactionStatusFilter?,
    periodText: String,
    onOpenPeriodFilter: () -> Unit,
    onSelectAll: () -> Unit,
    onToggleStatusFilter: (TransactionStatusFilter) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
) {
    val strings = LocalPosStrings.current
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = PosSpacing.xs),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        item(key = "period") {
            FilterChip(
                selected = false,
                onClick = onOpenPeriodFilter,
                label = { Text(text = periodText) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                shape = CircleShape,
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
        item(key = "all") {
            FilterChip(
                selected = statusFilter == null,
                onClick = onSelectAll,
                label = { Text(text = strings.transactionsFilterAll(transactions.size)) },
                shape = CircleShape,
                border = null,
                colors = chipColors,
            )
        }
        items(
            items = TransactionStatusFilter.entries,
            key = { option -> option.name },
        ) { option ->
            val count = transactions.count { transaction -> transaction.status in option.statuses }
            FilterChip(
                selected = option == statusFilter,
                onClick = { onToggleStatusFilter(option) },
                label = { Text(text = strings.transactionsFilterOption(option.label(strings), count)) },
                shape = CircleShape,
                border = null,
                colors = chipColors,
            )
        }
    }
}

private fun previewTransactionsState(): ReportsUiState {
    val transactions = listOf(
        sampleTransaction(
            id = 1L,
            invoiceNo = "INV-20261017-0003",
            paymentMethod = PaymentMethod.QRIS,
            grandTotal = 96_000L,
        ),
        sampleTransaction(
            id = 2L,
            invoiceNo = "INV-20261017-0002",
            paymentMethod = PaymentMethod.CASH,
            grandTotal = 54_500L,
        ),
        sampleTransaction(
            id = 3L,
            invoiceNo = "INV-20261017-0001",
            paymentMethod = PaymentMethod.KASBON,
            grandTotal = 120_000L,
            status = TransactionStatus.PARTIAL_DEBT,
        ),
    )
    return ReportsUiState(
        range = ReportRange(startMillis = 1_704_758_400_000L, endMillis = 1_705_363_199_999L),
        preset = ReportsRangePreset.TODAY,
        summary = SalesSummary(
            totalSales = 270_500L,
            totalHpp = 190_000L,
            netProfit = 80_500L,
            transactionCount = transactions.size,
        ),
        transactions = transactions,
        visibleTransactions = transactions,
        isLoading = false,
    )
}

private fun sampleTransaction(
    id: Long,
    invoiceNo: String,
    paymentMethod: PaymentMethod,
    grandTotal: Long,
    status: TransactionStatus = TransactionStatus.COMPLETED,
): Transaction = Transaction(
    id = id,
    invoiceNo = invoiceNo,
    createdAt = 1_705_320_180_000L,
    cashierId = null,
    customerId = null,
    subtotal = grandTotal,
    discountTotal = 0L,
    invoiceDiscount = 0L,
    taxTotal = 0L,
    rounding = 0L,
    grandTotal = grandTotal,
    totalHpp = 0L,
    netProfit = 0L,
    paymentMethod = paymentMethod,
    paidAmount = grandTotal,
    changeAmount = 0L,
    remainingDebt = 0L,
    dueDate = null,
    status = status,
    notes = null,
)

private val PREVIEW_ACTIONS = TransactionActions(
    onOpenDetail = {},
    onReprintReceipt = {},
    onShareReceipt = {},
    onRefund = {},
)

@Preview(name = "Transaksi compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun TransactionsCompactPreview() {
    UmkmPosTheme {
        TransactionsScreen(
            uiState = previewTransactionsState(),
            actions = PREVIEW_ACTIONS,
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onToggleStatusFilter = {},
            onAddTransaction = {},
            onRefresh = {},
        )
    }
}

@Preview(name = "Transaksi expanded", widthDp = 960, heightDp = 720, showBackground = true)
@Composable
private fun TransactionsExpandedPreview() {
    UmkmPosTheme {
        TransactionsScreen(
            uiState = previewTransactionsState(),
            actions = PREVIEW_ACTIONS,
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onToggleStatusFilter = {},
            onAddTransaction = {},
            onRefresh = {},
        )
    }
}
