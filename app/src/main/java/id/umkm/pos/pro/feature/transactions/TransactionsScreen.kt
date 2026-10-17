package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.feature.reports.ReportsFilterSheet
import id.umkm.pos.pro.feature.reports.ReportsRangePreset
import id.umkm.pos.pro.feature.reports.ReportsUiState
import id.umkm.pos.pro.feature.reports.RangeChip
import id.umkm.pos.pro.feature.reports.TransactionStatusFilter
import id.umkm.pos.pro.feature.reports.periodLabel
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.PrimaryActionButton
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
    onAddTransaction: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZone = remember { TimeZone.getDefault() }
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= PosBreakpoints.twoPane
        Column(modifier = Modifier.fillMaxSize()) {
            PosSectionHeader(
                title = "Transaksi",
                supportingText = "${uiState.visibleTransactions.size} transaksi · " +
                    periodLabel(uiState.preset, uiState.range, timeZone),
                modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            )
            TransactionsFilterRow(
                isExpanded = isExpanded,
                statusFilter = uiState.statusFilter,
                onToggleStatusFilter = onToggleStatusFilter,
                onOpenPeriodFilter = { isFilterSheetVisible = true },
                onAddTransaction = onAddTransaction,
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
                        LoadingState(label = "Memuat transaksi")

                    uiState.transactions.isEmpty() -> EmptyState(
                        iconRes = R.drawable.ic_receipt,
                        title = "Belum ada penjualan",
                        supportingText = "Transaksi yang selesai di Kasir akan tampil di sini pada periode ini.",
                    )

                    uiState.visibleTransactions.isEmpty() -> EmptyState(
                        iconRes = R.drawable.ic_receipt,
                        title = "Tidak ada transaksi",
                        supportingText = "Tidak ada transaksi dengan status " +
                            "${uiState.statusFilter?.label.orEmpty()} pada periode ini.",
                        actionLabel = "Tampilkan Semua",
                        onAction = {
                            uiState.statusFilter?.let { statusFilter -> onToggleStatusFilter(statusFilter) }
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
    isExpanded: Boolean,
    statusFilter: TransactionStatusFilter?,
    onToggleStatusFilter: (TransactionStatusFilter) -> Unit,
    onOpenPeriodFilter: () -> Unit,
    onAddTransaction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PosSpacing.md, end = PosSpacing.md, bottom = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        TransactionStatusFilter.entries.forEach { option ->
            RangeChip(
                label = option.label,
                selected = option == statusFilter,
                onClick = { onToggleStatusFilter(option) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenPeriodFilter) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Filter periode",
            )
        }
        if (isExpanded) {
            PrimaryActionButton(
                text = "Tambah Transaksi",
                onClick = onAddTransaction,
                iconRes = R.drawable.ic_plus,
                shouldFillWidth = false,
            )
        } else {
            FilledIconButton(
                onClick = onAddTransaction,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "Tambah transaksi",
                )
            }
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
