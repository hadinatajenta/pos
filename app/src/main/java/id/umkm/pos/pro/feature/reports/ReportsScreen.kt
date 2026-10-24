package id.umkm.pos.pro.feature.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.data.file.DownloadsFileSink
import id.umkm.pos.pro.monetization.admob.BannerAdSlot
import id.umkm.pos.pro.ui.common.CenteredScreenHeader
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.openDownloadedFile
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.common.shareDownloadedFile
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.ChartLegend
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.components.ChartSlice
import id.umkm.pos.pro.ui.components.PaymentMethodDonut
import id.umkm.pos.pro.ui.components.SalesLineChart
import id.umkm.pos.pro.ui.components.paymentSliceColor
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosFontFamily
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import java.util.Locale
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
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val isProfitVisible = gate.isUnlocked(CriticalAction.VIEW_PROFIT_AND_COST)
    val requestProfitUnlock: () -> Unit = {
        gate.request(CriticalAction.VIEW_PROFIT_AND_COST, strings.reportsNetProfit) {}
    }
    var isExportDialogVisible by remember { mutableStateOf(false) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    var adBannerHeight by remember { mutableStateOf(0.dp) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ReportsPalette.ScreenBackground),
    ) {
        val isExpanded = maxWidth >= PosBreakpoints.twoPane
        val horizontalPadding = if (isExpanded) 24.dp else 20.dp
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                CenteredScreenHeader(
                    title = strings.reportsTitle,
                    supportingText = periodLabel(uiState.preset, uiState.range, timeZone, strings),
                )
                ReportsPeriodBar(
                    preset = uiState.preset,
                    onPresetSelected = onPresetSelected,
                    onOpenCustomRange = { isFilterSheetVisible = true },
                    onExport = {
                        gate.request(CriticalAction.EXPORT_SALES_REPORT, strings.reportsExportButton) {
                            isExportDialogVisible = true
                        }
                    },
                    horizontalPadding = horizontalPadding,
                )
                if (uiState.isLoading && uiState.summary != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val error = uiState.error
                    when {
                        error != null -> ErrorState(error = error, onRetry = onRefresh)
                        uiState.summary == null -> LoadingState(label = strings.reportsLoading)
                        else -> MainReportContent(
                            uiState = uiState,
                            isExpanded = isExpanded,
                            isProfitVisible = isProfitVisible,
                            onRequestProfitUnlock = requestProfitUnlock,
                        )
                    }
                }
                BannerAdSlot(
                    placement = AdPlacement.REPORTS,
                    onHeightChanged = { adBannerHeight = it },
                )
            }
            ReportsExportToast(
                exportState = uiState.exportState,
                onDismissExport = onDismissExport,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp + adBannerHeight),
            )
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
private fun ReportsPeriodBar(
    preset: ReportsRangePreset,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onOpenCustomRange: () -> Unit,
    onExport: () -> Unit,
    horizontalPadding: Dp,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = PosSpacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            items(
                items = ReportsRangePreset.entries,
                key = { option -> option.name },
            ) { option ->
                val isCustom = option == ReportsRangePreset.CUSTOM
                ReportsFilterPill(
                    label = option.label(strings),
                    selected = preset == option,
                    onClick = {
                        if (isCustom) {
                            onOpenCustomRange()
                        } else {
                            onPresetSelected(option)
                        }
                    },
                    leadingIconRes = if (isCustom) R.drawable.ic_calendar else null,
                )
            }
        }
        ReportsExportPill(onClick = onExport)
    }
}

@Composable
private fun ReportsExportPill(onClick: () -> Unit) {
    val strings = LocalPosStrings.current
    Surface(
        onClick = onClick,
        color = ReportsPalette.CardSurface,
        contentColor = ReportsPalette.Primary,
        shape = CircleShape,
        shadowElevation = 2.dp,
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = strings.reportsExportButton,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MainReportContent(
    uiState: ReportsUiState,
    isExpanded: Boolean,
    isProfitVisible: Boolean,
    onRequestProfitUnlock: () -> Unit,
) {
    val summary = uiState.summary ?: return
    val horizontalPadding = if (isExpanded) 24.dp else 20.dp
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ReportTotals(
                summary = summary,
                averageTransaction = uiState.averageTransaction,
                isProfitVisible = isProfitVisible,
                onRequestProfitUnlock = onRequestProfitUnlock,
            )
        }
        if (isExpanded && uiState.paymentShares.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
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
        if (uiState.topProducts.isNotEmpty()) {
            item {
                TopProductsCard(
                    products = uiState.topProducts,
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
    isProfitVisible: Boolean,
    onRequestProfitUnlock: () -> Unit,
) {
    val palette = LocalPosSemanticPalette.current
    val strings = LocalPosStrings.current
    val profitColor = if (summary.netProfit >= 0L) ReportsPalette.Primary else palette.danger
    val netProfitText = if (isProfitVisible) {
        Rupiah.format(summary.netProfit)
    } else {
        strings.securityMaskedValue
    }
    val totalHppText = if (isProfitVisible) {
        Rupiah.format(summary.totalHpp)
    } else {
        strings.securityMaskedValue
    }
    val unlockProfit: (() -> Unit)? = if (isProfitVisible) null else onRequestProfitUnlock
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ReportsCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 20.dp,
            elevation = 6.dp,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = strings.reportsTotalTurnover,
                        color = ReportsPalette.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    AutoSizeNumberText(
                        text = Rupiah.format(summary.totalSales),
                        style = TextStyle(
                            fontFamily = PosFontFamily,
                            fontSize = 34.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.8).sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = ReportsPalette.TextMain,
                        minFontSize = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ReportIconBadge(iconRes = R.drawable.ic_store)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ReportsPalette.Divider)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SummaryValueColumn(
                    label = strings.reportsNetProfit,
                    value = netProfitText,
                    valueColor = if (isProfitVisible) profitColor else ReportsPalette.TextMuted,
                    onValueClick = unlockProfit,
                    modifier = Modifier.weight(1f),
                )
                SummaryValueColumn(
                    label = strings.reportsHppModal,
                    value = totalHppText,
                    valueColor = if (isProfitVisible) ReportsPalette.TextMain else ReportsPalette.TextMuted,
                    onValueClick = unlockProfit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReportsMetricCard(
                label = strings.reportsTxCount,
                value = strings.reportsNoteCount(summary.transactionCount),
                modifier = Modifier.weight(1f),
            )
            ReportsMetricCard(
                label = strings.reportsAverageTx,
                value = Rupiah.format(averageTransaction),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryValueColumn(
    label: String,
    value: String,
    valueColor: Color,
    onValueClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .then(if (onValueClick == null) Modifier else Modifier.clickable(onClick = onValueClick)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = ReportsPalette.TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
        )
        AutoSizeNumberText(
            text = value,
            style = TextStyle(
                fontFamily = PosFontFamily,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                fontFeatureSettings = "tnum",
            ),
            color = valueColor,
            minFontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SalesChartCard(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    ReportsCard(modifier = modifier) {
        ReportsCardTitle(text = LocalPosStrings.current.reportsSalesChart)
        Spacer(modifier = Modifier.height(16.dp))
        SalesLineChart(points = points)
    }
}

@Composable
private fun PaymentMethodsCard(
    shares: List<ReportsPaymentShare>,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val slices = shares.map { share ->
        ChartSlice(
            label = paymentMethodLabel(share.paymentMethod, strings),
            value = share.amount.toDouble(),
            color = paymentSliceColor(share.paymentMethod),
        )
    }
    ReportsCard(modifier = modifier) {
        ReportsCardTitle(text = strings.reportsPaymentMethods)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PaymentMethodDonut(
                slices = slices,
                totalLabel = Rupiah.format(shares.sumOf { share -> share.amount }),
            )
            ChartLegend(
                slices = slices,
                valueFormatter = { value, total ->
                    val pct = if (total <= 0.0) 0 else (value / total * 100).toInt()
                    "${Rupiah.format(value.toLong())} ($pct%)"
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun TopProductsCard(
    products: List<ReportProductSummary>,
    modifier: Modifier = Modifier,
) {
    val topItems = products.take(5)
    val strings = LocalPosStrings.current
    ReportsCard(modifier = modifier) {
        ReportsCardTitle(
            text = strings.reportsTopProducts,
            supportingText = strings.reportsTopProductsHint,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            topItems.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (index == 0) ReportsPalette.PrimaryLight else ReportsPalette.Divider,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = LocalAppTextStyles.current.supportingLabel,
                            fontWeight = FontWeight.Bold,
                            color = if (index == 0) ReportsPalette.Primary else ReportsPalette.TextMuted,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = ReportsPalette.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = strings.reportsSoldQuantity(formatSoldQuantity(item.quantitySold)),
                            style = LocalAppTextStyles.current.supportingLabel,
                            color = ReportsPalette.TextMuted,
                        )
                    }
                    Text(
                        text = Rupiah.format(item.salesValue),
                        style = LocalAppTextStyles.current.catalogItemPrice,
                        fontWeight = FontWeight.SemiBold,
                        color = ReportsPalette.TextMain,
                    )
                }
            }
        }
    }
}

private fun formatSoldQuantity(qty: Double): String {
    val asLong = qty.toLong()
    return if (qty == asLong.toDouble()) {
        asLong.toString()
    } else {
        String.format(Locale.US, "%.1f", qty)
    }
}

@Composable
private fun ReportsExportToast(
    exportState: ReportsExportState,
    onDismissExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val context = LocalContext.current
    when (exportState) {
        ReportsExportState.Idle -> Unit

        ReportsExportState.Running -> ReportsToast(
            title = strings.reportsExportingMsg,
            message = null,
            modifier = modifier,
            isLoading = true,
            onDismiss = onDismissExport,
        )

        is ReportsExportState.Succeeded -> ReportsToast(
            title = strings.exportDone,
            message = exportState.artifactPath,
            modifier = modifier,
            onOpen = { openDownloadedFile(context, exportState.artifactPath) },
            onShare = { shareDownloadedFile(context, exportState.artifactPath) },
            onDismiss = onDismissExport,
        )

        is ReportsExportState.Failed -> ReportsToast(
            title = exportErrorMessage(exportState.error, strings),
            message = null,
            modifier = modifier,
            isError = true,
            onDismiss = onDismissExport,
        )

        ReportsExportState.PermissionDenied -> ReportsToast(
            title = strings.reportsPermissionNeeded(DownloadsFileSink.DESTINATION_LABEL),
            message = null,
            modifier = modifier,
            isError = true,
            onDismiss = onDismissExport,
        )

        ReportsExportState.AdRequired -> ReportsToast(
            title = strings.reportsAdRequired,
            message = null,
            modifier = modifier,
            isError = true,
            onDismiss = onDismissExport,
        )
    }
}

private fun exportErrorMessage(error: AppError, strings: PosStrings): String = when (error) {
    is AppError.Validation -> strings.reportsExportErrorValidation
    is AppError.Storage -> strings.reportsExportErrorStorage
    is AppError.NotFound -> strings.reportsExportErrorNotFound
    is AppError.Hardware -> strings.reportsExportErrorHardware
    is AppError.Network -> strings.reportsExportErrorNetwork
    is AppError.Unexpected -> strings.reportsExportErrorUnexpected
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
            ReportsPaymentShare(paymentMethod = PaymentMethod.TRANSFER, amount = 210_000L, transactionCount = 2),
        ),
        topProducts = listOf(
            ReportProductSummary(productId = 1L, name = "Kopi Susu Gula Aren", quantitySold = 42.0, salesValue = 756_000L),
            ReportProductSummary(productId = 2L, name = "Roti Bakar Cokelat Keju", quantitySold = 25.0, salesValue = 550_000L),
            ReportProductSummary(productId = 3L, name = "Air Mineral 600ml", quantitySold = 60.0, salesValue = 300_000L),
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
