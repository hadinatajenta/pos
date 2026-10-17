package id.umkm.pos.pro.feature.reports

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.CircularProgressIndicator
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
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.ChartLegend
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.components.ChartSlice
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.PaymentMethodDonut
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.SalesLineChart
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.components.paymentSliceColor
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import java.util.TimeZone

@Composable
internal fun ReportsScreen(
    uiState: ReportsUiState,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onRefresh: () -> Unit,
    onExport: (ReportExportFormat) -> Unit,
    onDismissExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZone = remember { TimeZone.getDefault() }
    var isExportDialogVisible by remember { mutableStateOf(false) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= PosBreakpoints.twoPane
        Column(modifier = Modifier.fillMaxSize()) {
            ExportStatus(exportState = uiState.exportState, onDismissExport = onDismissExport)
            if (uiState.isLoading && uiState.summary != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val error = uiState.error
                when {
                    error != null -> ErrorState(error = error, onRetry = onRefresh)
                    uiState.summary == null -> LoadingState(label = "Memuat laporan")
                    else -> MainReportContent(
                        uiState = uiState,
                        timeZone = timeZone,
                        isExpanded = isExpanded,
                        onFilter = { isFilterSheetVisible = true },
                        onExport = { isExportDialogVisible = true },
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
    if (isExportDialogVisible) {
        ExportDialog(
            onConfirm = { format ->
                isExportDialogVisible = false
                onExport(format)
            },
            onDismiss = { isExportDialogVisible = false },
        )
    }
}

@Composable
private fun MainReportContent(
    uiState: ReportsUiState,
    timeZone: TimeZone,
    isExpanded: Boolean,
    onFilter: () -> Unit,
    onExport: () -> Unit,
) {
    val summary = uiState.summary ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        item {
            PosSectionHeader(
                title = "Laporan",
                supportingText = periodLabel(uiState.preset, uiState.range, timeZone),
                action = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onExport) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = "Ekspor laporan",
                            )
                        }
                        IconButton(onClick = onFilter) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "Filter periode",
                            )
                        }
                    }
                },
            )
        }
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    SalesChartCard(
                        points = uiState.salesChartPoints,
                        modifier = Modifier.weight(1f),
                    )
                    PaymentMethodsCard(
                        shares = uiState.paymentShares,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            item { SalesChartCard(points = uiState.salesChartPoints, modifier = Modifier.fillMaxWidth()) }
            if (uiState.paymentShares.isNotEmpty()) {
                item { PaymentMethodsCard(shares = uiState.paymentShares, modifier = Modifier.fillMaxWidth()) }
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            ReportMetricCard(
                label = "Total Penjualan",
                value = Rupiah.format(summary.totalSales),
                iconRes = R.drawable.ic_store,
                modifier = Modifier.weight(1f),
            )
            ReportMetricCard(
                label = "Jumlah Transaksi",
                value = summary.transactionCount.toString(),
                iconRes = R.drawable.ic_receipt,
                modifier = Modifier.weight(1f),
            )
            ReportMetricCard(
                label = "Rata-rata Transaksi",
                value = Rupiah.format(averageTransaction),
                iconRes = R.drawable.ic_banknote,
                modifier = Modifier.weight(1f),
            )
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        ReportMetricCard(
            label = "Total Penjualan",
            value = Rupiah.format(summary.totalSales),
            iconRes = R.drawable.ic_store,
            emphasized = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            ReportMetricCard(
                label = "Jumlah Transaksi",
                value = summary.transactionCount.toString(),
                modifier = Modifier.weight(1f),
            )
            ReportMetricCard(
                label = "Rata-rata Transaksi",
                value = Rupiah.format(averageTransaction),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReportMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    emphasized: Boolean = false,
) {
    PosCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.size(PosSpacing.xs))
                AutoSizeNumberText(
                    text = value,
                    style = if (emphasized) {
                        LocalAppTextStyles.current.financialValueLarge
                    } else {
                        LocalAppTextStyles.current.financialValue
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (iconRes != null) {
                IconBadge(iconRes = iconRes)
            }
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
private fun ExportStatus(
    exportState: ReportsExportState,
    onDismissExport: () -> Unit,
) {
    when (exportState) {
        ReportsExportState.Idle -> Unit
        ReportsExportState.Running -> ExportStatusRow {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(PosSpacing.sm))
            Text(text = "Mengekspor laporan…", style = LocalAppTextStyles.current.supportingLabel)
        }

        is ReportsExportState.Succeeded -> ExportStatusRow {
            Text(
                text = "Tersimpan di ${DownloadsReportSink.DESTINATION_LABEL}/${exportState.artifactPath}",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SecondaryTextAction(text = "Tutup", onClick = onDismissExport)
        }

        is ReportsExportState.Failed -> ExportStatusRow {
            Text(
                text = exportErrorMessage(exportState.error),
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            SecondaryTextAction(text = "Tutup", onClick = onDismissExport)
        }

        ReportsExportState.PermissionDenied -> ExportStatusRow {
            Text(
                text = "Izin penyimpanan diperlukan untuk menyimpan laporan ke folder " +
                    "${DownloadsReportSink.DESTINATION_LABEL}. Aktifkan izin lalu coba lagi.",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            SecondaryTextAction(text = "Tutup", onClick = onDismissExport)
        }
    }
}

private fun exportErrorMessage(error: AppError): String = when (error) {
    is AppError.Validation -> "Rentang tanggal tidak valid untuk diekspor."
    is AppError.Storage -> "Ekspor gagal menyimpan berkas. Periksa ruang penyimpanan lalu coba lagi."
    is AppError.NotFound -> "Data laporan tidak ditemukan."
    is AppError.Hardware -> "Perangkat tidak mendukung ekspor saat ini."
    is AppError.Network -> "Ekspor gagal. Coba lagi."
    is AppError.Unexpected -> "Ekspor gagal. Coba lagi."
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

private fun previewReportsState(): ReportsUiState {
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

@Preview(name = "Laporan compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ReportsCompactPreview() {
    UmkmPosTheme {
        ReportsScreen(
            uiState = previewReportsState(),
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onRefresh = {},
            onExport = {},
            onDismissExport = {},
        )
    }
}

@Preview(name = "Laporan expanded", widthDp = 960, heightDp = 720, showBackground = true)
@Composable
private fun ReportsExpandedPreview() {
    UmkmPosTheme {
        ReportsScreen(
            uiState = previewReportsState(),
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            onRefresh = {},
            onExport = {},
            onDismissExport = {},
        )
    }
}
