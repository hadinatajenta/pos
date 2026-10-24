package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import id.umkm.pos.pro.ui.common.CenteredScreenHeader
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.PosSearchField
import id.umkm.pos.pro.ui.products.ProductPalette
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosShapes
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
            CenteredScreenHeader(
                title = strings.transactionsTitle,
                supportingText = strings.transactionsCountWithPeriod(
                    uiState.visibleTransactions.size,
                    activePeriodLabel,
                ),
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
                    )
                    .shadow(
                        elevation = 3.dp,
                        shape = PosShapes.large,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.05f),
                    ),
                containerColor = Color.White,
                focusedContainerColor = Color.White,
                showFocusBorder = false,
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

                    isExpanded -> TransactionsCompactList(
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
    horizontalPadding: Dp,
) {
    val strings = LocalPosStrings.current
    val containerShape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 4.dp,
                bottom = 8.dp,
            )
            .shadow(
                elevation = 4.dp,
                shape = containerShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .background(ProductPalette.Surface, containerShape),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "period") {
                TransactionFilterChip(
                    label = periodText,
                    selected = false,
                    onClick = onOpenPeriodFilter,
                    leadingIconRes = R.drawable.ic_calendar,
                )
            }
            item(key = "all") {
                TransactionFilterChip(
                    label = strings.transactionsFilterAll(transactions.size),
                    selected = statusFilter == null,
                    onClick = onSelectAll,
                )
            }
            items(
                items = TransactionStatusFilter.entries,
                key = { option -> option.name },
            ) { option ->
                val count = transactions.count { transaction -> transaction.status in option.statuses }
                TransactionFilterChip(
                    label = strings.transactionsFilterOption(option.label(strings), count),
                    selected = option == statusFilter,
                    onClick = { onToggleStatusFilter(option) },
                )
            }
        }
    }
}

@Composable
private fun TransactionFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIconRes: Int? = null,
) {
    Surface(
        onClick = onClick,
        color = if (selected) ProductPalette.Brand else Color.Transparent,
        contentColor = if (selected) Color.White else ProductPalette.MutedText,
        shape = CircleShape,
        modifier = Modifier.heightIn(min = 36.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIconRes != null) {
                Icon(
                    painter = painterResource(leadingIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
            paymentMethod = PaymentMethod.TRANSFER,
            grandTotal = 120_000L,
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
