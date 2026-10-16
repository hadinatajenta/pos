package id.umkm.pos.pro.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.ChartLegend
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.components.ChartSlice
import id.umkm.pos.pro.ui.components.CompactMetric
import id.umkm.pos.pro.ui.components.MetricCard
import id.umkm.pos.pro.ui.components.PaymentMethodDonut
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.SalesLineChart
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSpacing
import java.util.TimeZone

private enum class DatePickerTarget { START, END }

private val VISIBLE_RANGE_PRESETS = listOf(
    ReportsRangePreset.TODAY,
    ReportsRangePreset.LAST_7_DAYS,
    ReportsRangePreset.LAST_30_DAYS,
    ReportsRangePreset.THIS_MONTH,
)

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
    Column(modifier = modifier.fillMaxSize()) {
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
                    onSummaryDrillDown = onSummaryDrillDown,
                    onDayDrillDown = onDayDrillDown,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            HeaderChip(
                label = "Dari ${ReportDates.formatLocalDateShort(uiState.range.startMillis, timeZone)}",
                selected = uiState.preset == ReportsRangePreset.CUSTOM,
                onClick = { activePicker = DatePickerTarget.START },
            )
            HeaderChip(
                label = "Sampai ${ReportDates.formatLocalDateShort(uiState.range.endMillis, timeZone)}",
                selected = uiState.preset == ReportsRangePreset.CUSTOM,
                onClick = { activePicker = DatePickerTarget.END },
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
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = label, style = LocalAppTextStyles.current.buttonLabel, color = Color.White)
        }
    }
}

@Composable
private fun HeaderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0.9f else 0.35f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
        )
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        item {
            MetricCard(
                label = "Total Penjualan",
                value = Rupiah.format(summary.totalSales),
                icon = Icons.Filled.PointOfSale,
                emphasized = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            PosCard(modifier = Modifier.fillMaxWidth()) {
                CompactMetric(label = "Jumlah Transaksi", value = summary.transactionCount.toString())
                CompactMetric(label = "Rata-rata Transaksi", value = Rupiah.format(uiState.averageTransaction))
            }
        }
        item { SalesChartCard(days = uiState.dailySales) }
        if (uiState.paymentShares.isNotEmpty()) {
            item { PaymentMethodsCard(shares = uiState.paymentShares) }
        }
        item {
            PosSectionHeader(
                title = "Rincian Penjualan",
                supportingText = "${summary.transactionCount} transaksi",
                action = { SecondaryTextAction(text = "Lihat Semua", onClick = onSummaryDrillDown) },
            )
        }
        if (uiState.dailySales.isEmpty()) {
            item {
                Text(
                    text = "Belum ada rincian harian pada rentang ini.",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(items = uiState.dailySales, key = { day -> day.epochDay }) { day ->
                DailyRow(day = day, onDayDrillDown = onDayDrillDown)
            }
        }
    }
}

@Composable
private fun SalesChartCard(days: List<DailySales>) {
    val hasChartData = days.any { day -> day.totalSales > 0L }
    val points = if (hasChartData) {
        days.map { day ->
            ChartPoint(label = ReportDates.weekdayShortLabel(day.epochDay), value = day.totalSales.toDouble())
        }
    } else {
        emptyList()
    }
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = "Grafik Penjualan")
        SalesLineChart(points = points)
    }
}

@Composable
private fun PaymentMethodsCard(shares: List<ReportsPaymentShare>) {
    val slices = shares.map { share ->
        ChartSlice(
            label = paymentMethodLabel(share.paymentMethod),
            value = share.amount.toDouble(),
            color = paymentSliceColor(share.paymentMethod),
        )
    }
    PosCard(modifier = Modifier.fillMaxWidth()) {
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
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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

private fun paymentSliceColor(method: PaymentMethod): Color = when (method) {
    PaymentMethod.CASH -> Color(0xFF1E6B54)
    PaymentMethod.QRIS -> Color(0xFF21845D)
    PaymentMethod.TRANSFER -> Color(0xFFE7C46A)
    PaymentMethod.SPLIT -> Color(0xFF7C8A83)
    PaymentMethod.KASBON -> Color(0xFFB87917)
}

private fun statusLabel(status: TransactionStatus): String = when (status) {
    TransactionStatus.COMPLETED -> "Lunas"
    TransactionStatus.PARTIAL_DEBT -> "Kasbon"
    TransactionStatus.RETURNED -> "Dikembalikan"
}
