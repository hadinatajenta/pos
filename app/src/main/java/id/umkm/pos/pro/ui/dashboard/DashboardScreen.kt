package id.umkm.pos.pro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.ChartLegend
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.components.ChartSlice
import id.umkm.pos.pro.ui.components.DailySalesBarChart
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.MetricCard
import id.umkm.pos.pro.ui.components.PaymentMethodDonut
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.paymentSliceColor
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val TOTAL_METRIC_WEIGHT = 1.3f
private val EMPTY_STATE_MAX_WIDTH = 560.dp

@Composable
internal fun DashboardScreen(
    state: DashboardUiState,
    onPresetSelected: (DashboardRangePreset) -> Unit,
    onOpenCashier: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= PosBreakpoints.twoPane
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            DashboardHeader(state = state, onPresetSelected = onPresetSelected, onOpenCashier = onOpenCashier)
            when {
                state.isLoading -> LoadingState(label = "Memuat ringkasan")
                state.error != null -> ErrorState(error = state.error, onRetry = onRetry)
                !state.hasAnySales -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PosSpacing.md),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    DashboardEmptyState(
                        modifier = if (isExpanded) Modifier.widthIn(max = EMPTY_STATE_MAX_WIDTH) else Modifier,
                    )
                }

                else -> DashboardContent(state = state, isExpanded = isExpanded)
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    state: DashboardUiState,
    onPresetSelected: (DashboardRangePreset) -> Unit,
    onOpenCashier: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PosColors.ForestGreen)
            .padding(horizontal = PosSpacing.md)
            .padding(top = PosSpacing.sm, bottom = PosSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadgeGold()
            Column(modifier = Modifier.padding(start = PosSpacing.sm)) {
                Text(
                    text = state.storeName.ifBlank { "POS UMKM Pro" },
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Halo, ${state.cashierName?.takeIf { it.isNotBlank() } ?: "Pemilik Toko"}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "Semangat terus! Berikut ringkasan bisnis Anda.",
            style = LocalAppTextStyles.current.supportingLabel,
            color = Color.White.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PosSpacing.xs)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            DashboardRangePreset.entries.forEach { preset ->
                HeaderChip(
                    label = preset.label,
                    selected = preset == state.preset,
                    onClick = { onPresetSelected(preset) },
                )
            }
        }
        GoldAction(label = "Buka Kasir", onClick = onOpenCashier, modifier = Modifier.padding(top = PosSpacing.xs))
    }
}

@Composable
private fun IconBadgeGold() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = PosColors.Gold, shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PointOfSale,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = PosColors.ForestGreen,
        )
    }
}

@Composable
private fun GoldAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = PosColors.Gold,
        contentColor = PosColors.ForestGreen,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = LocalAppTextStyles.current.buttonLabel,
                modifier = Modifier.padding(start = PosSpacing.sm),
            )
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
        color = if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
        )
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    isExpanded: Boolean,
) {
    Column(
        modifier = Modifier
            .offset(y = (-PosSpacing.lg))
            .padding(horizontal = PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        DashboardMetricRow(state = state, isExpanded = isExpanded)
        val chartTitle = "Penjualan ${state.preset.label}"
        if (isExpanded) {
            DashboardChartsRow(state = state, chartTitle = chartTitle)
            BestSellersCard(state.bestSellers)
        } else {
            SalesChartCard(points = state.dailyPoints, title = chartTitle)
            BestSellersCard(state.bestSellers)
            PaymentMethodsCard(state.paymentShares)
        }
    }
}

@Composable
private fun DashboardMetricRow(
    state: DashboardUiState,
    isExpanded: Boolean,
) {
    val totalLabel = "Total Penjualan ${state.preset.label}"
    val deltaText = state.comparisonPercent?.let { percent -> comparisonLabel(percent) }
    if (isExpanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            MetricCard(
                label = totalLabel,
                value = Rupiah.format(state.totalSales),
                icon = Icons.Filled.PointOfSale,
                deltaText = deltaText,
                isPositive = (state.comparisonPercent ?: 0) >= 0,
                emphasized = true,
                modifier = Modifier.weight(TOTAL_METRIC_WEIGHT).fillMaxHeight(),
            )
            MetricCard(
                label = "Jumlah Transaksi",
                value = state.transactionCount.toString(),
                icon = Icons.Filled.Receipt,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            MetricCard(
                label = "Produk Terjual",
                value = formatQuantity(state.itemsSold),
                icon = Icons.Filled.ShoppingBag,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            MetricCard(
                label = "Rata-rata",
                value = Rupiah.format(state.averageTransaction),
                icon = Icons.Filled.Payments,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        return
    }
    MetricCard(
        label = totalLabel,
        value = Rupiah.format(state.totalSales),
        icon = Icons.Filled.PointOfSale,
        deltaText = deltaText,
        isPositive = (state.comparisonPercent ?: 0) >= 0,
        emphasized = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
        CompactMetricCard("Jumlah Transaksi", state.transactionCount.toString(), Modifier.weight(1f))
        CompactMetricCard("Produk Terjual", formatQuantity(state.itemsSold), Modifier.weight(1f))
        CompactMetricCard("Rata-rata", Rupiah.format(state.averageTransaction), Modifier.weight(1f))
    }
}

@Composable
private fun DashboardChartsRow(
    state: DashboardUiState,
    chartTitle: String,
) {
    if (state.paymentShares.isEmpty()) {
        SalesChartCard(points = state.dailyPoints, title = chartTitle)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        SalesChartCard(
            points = state.dailyPoints,
            title = chartTitle,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        PaymentMethodsCard(
            shares = state.paymentShares,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun SalesChartCard(
    points: List<ChartPoint>,
    title: String,
    modifier: Modifier = Modifier,
) {
    PosCard(modifier = modifier.fillMaxWidth()) {
        PosSectionHeader(title = title)
        DailySalesBarChart(points = points)
    }
}

private fun comparisonLabel(percent: Int): String {
    val sign = if (percent >= 0) "+" else ""
    return "$sign$percent% dari periode sebelumnya"
}

@Composable
private fun CompactMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(PosSpacing.sm)) {
            Text(
                text = label,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = PosSpacing.xxs),
            )
        }
    }
}

@Composable
private fun BestSellersCard(bestSellers: List<BestSeller>) {
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(title = "Produk Terlaris")
        if (bestSellers.isEmpty()) {
            Text(
                text = "Belum ada produk terjual pada periode ini.",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@PosCard
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            items(bestSellers) { seller ->
                Column(modifier = Modifier.width(118.dp)) {
                    ProductThumbnail(category = seller.category, size = 64.dp)
                    Text(
                        text = seller.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = PosSpacing.xs),
                    )
                    Text(
                        text = "${formatQuantity(seller.quantitySold)} terjual",
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodsCard(
    shares: List<PaymentShare>,
    modifier: Modifier = Modifier,
) {
    if (shares.isEmpty()) return
    val slices = shares.map { share ->
        ChartSlice(label = share.label, value = share.amount.toDouble(), color = paymentSliceColor(share.method))
    }
    PosCard(modifier = modifier.fillMaxWidth()) {
        PosSectionHeader(title = "Metode Pembayaran")
        Row(verticalAlignment = Alignment.CenterVertically) {
            PaymentMethodDonut(
                slices = slices,
                totalLabel = Rupiah.format(shares.sumOf { it.amount }),
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
private fun DashboardEmptyState(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.PointOfSale,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "Belum ada penjualan", style = LocalAppTextStyles.current.sectionHeading)
            Text(
                text = "Mulai transaksi pertama di Kasir, lalu ringkasan penjualan akan muncul di sini.",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
