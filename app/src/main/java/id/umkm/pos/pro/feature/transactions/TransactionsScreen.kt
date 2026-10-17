package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.feature.reports.ReportDates
import id.umkm.pos.pro.feature.reports.ReportsDrillDown
import id.umkm.pos.pro.feature.reports.ReportsFilterSheet
import id.umkm.pos.pro.feature.reports.ReportsRangePreset
import id.umkm.pos.pro.feature.reports.ReportsUiState
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.feature.reports.periodLabel
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import java.util.TimeZone

private const val DAILY_BREAKDOWN_COLUMNS = 2
private val TRAILING_VALUE_MAX_WIDTH = 176.dp

@Composable
internal fun TransactionsScreen(
    uiState: ReportsUiState,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onRefresh: () -> Unit,
    onSummaryDrillDown: () -> Unit,
    onDayDrillDown: (Long) -> Unit,
    onCloseDrillDown: () -> Unit,
    onTransactionReturn: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZone = remember { TimeZone.getDefault() }
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= PosBreakpoints.twoPane
        Column(modifier = Modifier.fillMaxSize()) {
            val drillDown = uiState.drillDown
            if (drillDown == null) {
                PosSectionHeader(
                    title = "Transaksi",
                    supportingText = "${uiState.summary?.transactionCount ?: 0} transaksi · " +
                        periodLabel(uiState.preset, uiState.range, timeZone),
                    action = {
                        IconButton(onClick = { isFilterSheetVisible = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "Filter periode",
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
                )
            }
            if (uiState.isLoading && uiState.summary != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val error = uiState.error
                when {
                    error != null -> ErrorState(error = error, onRetry = onRefresh)
                    drillDown != null -> DrillDownSection(
                        drillDown = drillDown,
                        timeZone = timeZone,
                        onCloseDrillDown = onCloseDrillDown,
                        onTransactionReturn = onTransactionReturn,
                    )

                    uiState.summary == null -> LoadingState(label = "Memuat transaksi")
                    else -> DailySalesList(
                        dailySales = uiState.dailySales,
                        isExpanded = isExpanded,
                        onDayDrillDown = onDayDrillDown,
                        onSummaryDrillDown = onSummaryDrillDown,
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
private fun DailySalesList(
    dailySales: List<DailySales>,
    isExpanded: Boolean,
    onDayDrillDown: (Long) -> Unit,
    onSummaryDrillDown: () -> Unit,
) {
    if (dailySales.isEmpty()) {
        EmptyState(
            iconRes = R.drawable.ic_receipt,
            title = "Belum ada penjualan",
            supportingText = "Transaksi yang selesai di Kasir akan tampil di sini pada periode ini.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        if (isExpanded) {
            items(
                items = dailySales.chunked(DAILY_BREAKDOWN_COLUMNS),
                key = { columns -> columns.first().epochDay },
            ) { columns ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
                ) {
                    columns.forEach { day ->
                        DailyRow(
                            day = day,
                            onDayDrillDown = onDayDrillDown,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(DAILY_BREAKDOWN_COLUMNS - columns.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            items(items = dailySales, key = { day -> day.epochDay }) { day ->
                DailyRow(
                    day = day,
                    onDayDrillDown = onDayDrillDown,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SecondaryTextAction(text = "Lihat semua", onClick = onSummaryDrillDown)
        }
    }
}

@Composable
private fun DailyRow(
    day: DailySales,
    onDayDrillDown: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDayDrillDown(day.epochDay) }
                .padding(vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${ReportDates.weekdayShortLabel(day.epochDay)}, ${ReportDates.formatEpochDayShort(day.epochDay)}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${day.transactionCount} transaksi",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AutoSizeNumberText(
                    text = Rupiah.format(day.totalSales),
                    style = LocalAppTextStyles.current.financialValue,
                    modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
                )
                AutoSizeNumberText(
                    text = "Laba ${Rupiah.format(day.netProfit)}",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun DrillDownSection(
    drillDown: ReportsDrillDown,
    timeZone: TimeZone,
    onCloseDrillDown: () -> Unit,
    onTransactionReturn: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PosSpacing.sm, end = PosSpacing.md, top = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseDrillDown) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Kembali",
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Transaksi",
                    style = LocalAppTextStyles.current.sectionHeading,
                )
                Text(
                    text = drillDownSubtitle(drillDown),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
        if (drillDown.transactions.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_receipt,
                title = "Belum ada transaksi",
                supportingText = "Tidak ada transaksi pada periode ini.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = PosSpacing.md),
            ) {
                items(items = drillDown.transactions, key = { transaction -> transaction.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        timeZone = timeZone,
                        onReturn = { onTransactionReturn(transaction.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun drillDownSubtitle(drillDown: ReportsDrillDown): String = when (drillDown) {
    is ReportsDrillDown.Summary -> "${drillDown.transactions.size} transaksi"
    is ReportsDrillDown.Day ->
        "${ReportDates.weekdayShortLabel(drillDown.epochDay)}, " +
            "${ReportDates.formatEpochDayShort(drillDown.epochDay)} · " +
            "${drillDown.transactions.size} transaksi"
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    timeZone: TimeZone,
    onReturn: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.invoiceNo,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(PosSpacing.sm))
                AutoSizeNumberText(
                    text = Rupiah.format(transaction.grandTotal),
                    style = LocalAppTextStyles.current.financialValue,
                    modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
                )
            }
            Text(
                text = "${ReportDates.formatTimestamp(transaction.createdAt, timeZone)} · " +
                    "${paymentMethodLabel(transaction.paymentMethod)} · ${statusLabel(transaction.status)}",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(PosSpacing.sm))
        SecondaryTextAction(text = "Retur", onClick = onReturn)
    }
}

private fun statusLabel(status: TransactionStatus): String = when (status) {
    TransactionStatus.COMPLETED -> "Lunas"
    TransactionStatus.PARTIAL_DEBT -> "Kasbon"
    TransactionStatus.RETURNED -> "Dikembalikan"
}

private fun previewTransactionsState(): ReportsUiState = ReportsUiState(
    range = ReportRange(startMillis = 1_704_758_400_000L, endMillis = 1_705_363_199_999L),
    preset = ReportsRangePreset.LAST_7_DAYS,
    summary = SalesSummary(
        totalSales = 3_042_000L,
        totalHpp = 1_995_000L,
        netProfit = 1_035_000L,
        transactionCount = 18,
    ),
    dailySales = listOf(
        DailySales(epochDay = 19_731L, totalSales = 512_000L, netProfit = 180_000L, transactionCount = 3),
        DailySales(epochDay = 19_737L, totalSales = 1_180_000L, netProfit = 410_000L, transactionCount = 6),
    ),
    isLoading = false,
)

@Preview(name = "Transaksi compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun TransactionsCompactPreview() {
    UmkmPosTheme {
        TransactionsScreen(
            uiState = previewTransactionsState(),
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onRefresh = {},
            onSummaryDrillDown = {},
            onDayDrillDown = {},
            onCloseDrillDown = {},
            onTransactionReturn = {},
        )
    }
}

@Preview(name = "Transaksi expanded", widthDp = 960, heightDp = 720, showBackground = true)
@Composable
private fun TransactionsExpandedPreview() {
    UmkmPosTheme {
        TransactionsScreen(
            uiState = previewTransactionsState(),
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onRefresh = {},
            onSummaryDrillDown = {},
            onDayDrillDown = {},
            onCloseDrillDown = {},
            onTransactionReturn = {},
        )
    }
}
