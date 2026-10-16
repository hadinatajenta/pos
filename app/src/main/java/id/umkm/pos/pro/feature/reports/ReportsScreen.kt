package id.umkm.pos.pro.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.ChartLegend
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.components.ChartSlice
import id.umkm.pos.pro.ui.components.MetricCard
import id.umkm.pos.pro.ui.components.PaymentMethodDonut
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.SalesLineChart
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.components.paymentSliceColor
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import java.util.TimeZone

private enum class DatePickerTarget { START, END }

private val VISIBLE_RANGE_PRESETS = listOf(
    ReportsRangePreset.TODAY,
    ReportsRangePreset.LAST_7_DAYS,
    ReportsRangePreset.LAST_30_DAYS,
    ReportsRangePreset.THIS_MONTH,
)

private const val DAILY_BREAKDOWN_COLUMNS = 2
private const val TOTAL_METRIC_WEIGHT = 1.3f

@Composable
internal fun ReportsScreen(
    uiState: ReportsUiState,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onRefresh: () -> Unit,
    onSummaryDrillDown: () -> Unit,
    onDayDrillDown: (Long) -> Unit,
    onCloseDrillDown: () -> Unit,
    onTransactionReturn: (Long) -> Unit,
    onExportCsv: () -> Unit,
    onDismissExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZone = remember { TimeZone.getDefault() }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= PosBreakpoints.twoPane
        Column(modifier = Modifier.fillMaxSize()) {
            ReportsHeader(
                uiState = uiState,
                timeZone = timeZone,
                onPresetSelected = onPresetSelected,
                onCustomRangeSelected = onCustomRangeSelected,
                onExportCsv = onExportCsv,
            )
            ExportStatus(exportState = uiState.exportState, onDismissExport = onDismissExport)
            if (uiState.isLoading && uiState.summary != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val error = uiState.error
                val drillDown = uiState.drillDown
                when {
                    error != null -> ErrorState(error = error, onRetry = onRefresh)
                    drillDown != null -> DrillDownSection(
                        drillDown = drillDown,
                        timeZone = timeZone,
                        onCloseDrillDown = onCloseDrillDown,
                        onTransactionReturn = onTransactionReturn,
                    )

                    uiState.summary == null -> LoadingState(label = "Memuat laporan")
                    else -> MainReportContent(
                        uiState = uiState,
                        isExpanded = isExpanded,
                        onSummaryDrillDown = onSummaryDrillDown,
                        onDayDrillDown = onDayDrillDown,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReportsHeader(
    uiState: ReportsUiState,
    timeZone: TimeZone,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onExportCsv: () -> Unit,
) {
    var activePicker by remember { mutableStateOf<DatePickerTarget?>(null) }
    val startEpochDay = ReportDates.epochDayForLocalMillis(uiState.range.startMillis, timeZone)
    val endEpochDay = ReportDates.epochDayForLocalMillis(uiState.range.endMillis, timeZone)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PosColors.ForestGreen)
            .padding(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Laporan",
                    style = LocalAppTextStyles.current.screenTitle,
                    color = Color.White,
                )
                Text(
                    text = "Ringkasan penjualan dan transaksi",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.width(PosSpacing.sm))
            HeaderAction(
                label = "Ekspor",
                icon = Icons.Filled.FileDownload,
                onClick = onExportCsv,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            VISIBLE_RANGE_PRESETS.forEach { preset ->
                HeaderChip(
                    label = preset.label,
                    selected = preset == uiState.preset,
                    onClick = { onPresetSelected(preset) },
                    maxLines = 1,
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            HeaderChip(
                label = "Dari ${ReportDates.formatLocalDateShort(uiState.range.startMillis, timeZone)}",
                selected = uiState.preset == ReportsRangePreset.CUSTOM,
                onClick = { activePicker = DatePickerTarget.START },
                maxLines = 2,
            )
            HeaderChip(
                label = "Sampai ${ReportDates.formatLocalDateShort(uiState.range.endMillis, timeZone)}",
                selected = uiState.preset == ReportsRangePreset.CUSTOM,
                onClick = { activePicker = DatePickerTarget.END },
                maxLines = 2,
            )
        }
    }

    activePicker?.let { target ->
        val initialEpochDay = if (target == DatePickerTarget.START) startEpochDay else endEpochDay
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochDay * ReportDates.MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedEpochDay = selectedMillis / ReportDates.MILLIS_PER_DAY
                            if (target == DatePickerTarget.START) {
                                onCustomRangeSelected(selectedEpochDay, endEpochDay)
                            } else {
                                onCustomRangeSelected(startEpochDay, selectedEpochDay)
                            }
                        }
                        activePicker = null
                    },
                ) {
                    Text(text = "Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) {
                    Text(text = "Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun HeaderAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = PosSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = label, style = LocalAppTextStyles.current.buttonLabel, color = Color.White)
            }
        }
    }
}

@Composable
private fun HeaderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    maxLines: Int,
) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0.9f else 0.35f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = LocalAppTextStyles.current.supportingLabel,
                color = Color.White,
                maxLines = maxLines,
                modifier = Modifier.padding(horizontal = PosSpacing.md),
            )
        }
    }
}

@Composable
private fun ExportStatus(
    exportState: ReportsExportState,
    onDismissExport: () -> Unit,
) {
    when (exportState) {
        ReportsExportState.Idle -> Unit
        ReportsExportState.Running -> ExportStatusRow {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(PosSpacing.sm))
            Text(text = "Mengekspor CSV…", style = LocalAppTextStyles.current.supportingLabel)
        }

        is ReportsExportState.Succeeded -> ExportStatusRow {
            Text(
                text = "Tersimpan: ${exportState.artifactPath.substringAfterLast('/')}",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SecondaryTextAction(text = "Tutup", onClick = onDismissExport)
        }

        is ReportsExportState.Failed -> ExportStatusRow {
            Text(
                text = "Ekspor gagal: ${exportState.error.message}",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            SecondaryTextAction(text = "Tutup", onClick = onDismissExport)
        }
    }
}

@Composable
private fun ExportStatusRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun MainReportContent(
    uiState: ReportsUiState,
    isExpanded: Boolean,
    onSummaryDrillDown: () -> Unit,
    onDayDrillDown: (Long) -> Unit,
) {
    val summary = uiState.summary ?: return
    if (summary.transactionCount == 0) {
        EmptyState(
            icon = Icons.Filled.BarChart,
            title = "Belum ada penjualan",
            supportingText = "Laporan akan terisi setelah ada transaksi selesai di Kasir pada rentang ini.",
        )
        return
    }

    val breakdownDays = uiState.dailySales
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        item {
            ReportTotals(
                summary = summary,
                averageTransaction = uiState.averageTransaction,
                isExpanded = isExpanded,
            )
        }
        if (isExpanded && uiState.paymentShares.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
                ) {
                    SalesChartCard(
                        points = uiState.salesChartPoints,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    PaymentMethodsCard(
                        shares = uiState.paymentShares,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        } else {
            item { SalesChartCard(points = uiState.salesChartPoints, modifier = Modifier.fillMaxWidth()) }
            if (uiState.paymentShares.isNotEmpty()) {
                item { PaymentMethodsCard(shares = uiState.paymentShares, modifier = Modifier.fillMaxWidth()) }
            }
        }
        item {
            PosSectionHeader(
                title = "Rincian Penjualan",
                supportingText = "${summary.transactionCount} transaksi",
                action = { SecondaryTextAction(text = "Lihat Semua", onClick = onSummaryDrillDown) },
            )
        }
        if (breakdownDays.isEmpty()) {
            item {
                Text(
                    text = "Belum ada rincian harian pada rentang ini.",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (isExpanded) {
            items(
                items = breakdownDays.chunked(DAILY_BREAKDOWN_COLUMNS),
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
            items(items = breakdownDays, key = { day -> day.epochDay }) { day ->
                DailyRow(
                    day = day,
                    onDayDrillDown = onDayDrillDown,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReportTotals(
    summary: SalesSummary,
    averageTransaction: Long,
    isExpanded: Boolean,
) {
    if (isExpanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            MetricCard(
                label = "Total Penjualan",
                value = Rupiah.format(summary.totalSales),
                icon = Icons.Filled.PointOfSale,
                emphasized = true,
                modifier = Modifier.weight(TOTAL_METRIC_WEIGHT).fillMaxHeight(),
            )
            MetricCard(
                label = "Jumlah Transaksi",
                value = summary.transactionCount.toString(),
                icon = Icons.Filled.Receipt,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            MetricCard(
                label = "Rata-rata Transaksi",
                value = Rupiah.format(averageTransaction),
                icon = Icons.Filled.Payments,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        MetricCard(
            label = "Total Penjualan",
            value = Rupiah.format(summary.totalSales),
            icon = Icons.Filled.PointOfSale,
            emphasized = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            MetricCard(
                label = "Jumlah Transaksi",
                value = summary.transactionCount.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Rata-rata Transaksi",
                value = Rupiah.format(averageTransaction),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SalesChartCard(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    PosCard(modifier = modifier) {
        PosSectionHeader(title = "Grafik Penjualan")
        SalesLineChart(points = points)
    }
}

@Composable
private fun PaymentMethodsCard(
    shares: List<ReportsPaymentShare>,
    modifier: Modifier = Modifier,
) {
    val slices = shares.map { share ->
        ChartSlice(
            label = paymentMethodLabel(share.paymentMethod),
            value = share.amount.toDouble(),
            color = paymentSliceColor(share.paymentMethod),
        )
    }
    PosCard(modifier = modifier) {
        PosSectionHeader(title = "Metode Pembayaran")
        Row(verticalAlignment = Alignment.CenterVertically) {
            PaymentMethodDonut(
                slices = slices,
                totalLabel = Rupiah.format(shares.sumOf { share -> share.amount }),
            )
            ChartLegend(
                slices = slices,
                valueFormatter = { value, total ->
                    if (total <= 0.0) "0%" else "${(value / total * 100).toInt()}%"
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = PosSpacing.md),
            )
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
                Text(
                    text = Rupiah.format(day.totalSales),
                    style = LocalAppTextStyles.current.financialValue,
                )
                Text(
                    text = "Laba ${Rupiah.format(day.netProfit)}",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (drillDown) {
                        is ReportsDrillDown.Summary -> "Rincian Penjualan"
                        is ReportsDrillDown.Day -> ReportDates.formatEpochDayShort(drillDown.epochDay)
                    },
                    style = LocalAppTextStyles.current.sectionHeading,
                )
                Text(
                    text = "${drillDown.transactions.size} transaksi",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
        if (drillDown.transactions.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Receipt,
                title = "Belum ada transaksi",
                supportingText = "Tidak ada transaksi pada rentang ini.",
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
                Text(
                    text = Rupiah.format(transaction.grandTotal),
                    style = LocalAppTextStyles.current.financialValue,
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

private fun previewReportsState(): ReportsUiState {
    val dailySales = listOf(
        DailySales(epochDay = 19_731L, totalSales = 512_000L, netProfit = 180_000L, transactionCount = 3),
        DailySales(epochDay = 19_732L, totalSales = 740_000L, netProfit = 240_000L, transactionCount = 5),
        DailySales(epochDay = 19_734L, totalSales = 610_000L, netProfit = 205_000L, transactionCount = 4),
        DailySales(epochDay = 19_737L, totalSales = 1_180_000L, netProfit = 410_000L, transactionCount = 6),
    )
    return ReportsUiState(
        range = ReportRange(startMillis = 1_704_758_400_000L, endMillis = 1_705_363_199_999L),
        preset = ReportsRangePreset.LAST_7_DAYS,
        summary = SalesSummary(
            totalSales = 3_042_000L,
            totalHpp = 1_995_000L,
            netProfit = 1_035_000L,
            transactionCount = 18,
        ),
        averageTransaction = 169_000L,
        dailySales = dailySales,
        salesChartPoints = listOf(
            ChartPoint(label = "Sel", value = 512_000.0),
            ChartPoint(label = "Rab", value = 740_000.0),
            ChartPoint(label = "Kam", value = 0.0),
            ChartPoint(label = "Jum", value = 610_000.0),
            ChartPoint(label = "Sab", value = 0.0),
            ChartPoint(label = "Min", value = 0.0),
            ChartPoint(label = "Sen", value = 1_180_000.0),
        ),
        paymentShares = listOf(
            ReportsPaymentShare(paymentMethod = PaymentMethod.CASH, amount = 1_825_200L, transactionCount = 11),
            ReportsPaymentShare(paymentMethod = PaymentMethod.QRIS, amount = 760_500L, transactionCount = 4),
            ReportsPaymentShare(paymentMethod = PaymentMethod.KASBON, amount = 456_300L, transactionCount = 3),
        ),
        isLoading = false,
    )
}

@Preview(name = "Reports compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ReportsCompactPreview() {
    UmkmPosTheme {
        ReportsScreen(
            uiState = previewReportsState(),
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onRefresh = {},
            onSummaryDrillDown = {},
            onDayDrillDown = {},
            onCloseDrillDown = {},
            onTransactionReturn = {},
            onExportCsv = {},
            onDismissExport = {},
        )
    }
}

@Preview(name = "Reports expanded", widthDp = 960, heightDp = 720, showBackground = true)
@Composable
private fun ReportsExpandedPreview() {
    UmkmPosTheme {
        ReportsScreen(
            uiState = previewReportsState(),
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onRefresh = {},
            onSummaryDrillDown = {},
            onDayDrillDown = {},
            onCloseDrillDown = {},
            onTransactionReturn = {},
            onExportCsv = {},
            onDismissExport = {},
        )
    }
}
