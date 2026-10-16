package id.umkm.pos.pro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.ChartLegend
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.components.ChartSlice
import id.umkm.pos.pro.ui.components.DailySalesBarChart
import id.umkm.pos.pro.ui.components.MetricCard
import id.umkm.pos.pro.ui.components.PaymentMethodDonut
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.SalesLineChart
import id.umkm.pos.pro.ui.components.paymentSliceColor
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun DashboardScreen(
    state: DashboardUiState,
    onPresetSelected: (DashboardRangePreset) -> Unit,
    onOpenCashier: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DashboardHeader(state = state, onPresetSelected = onPresetSelected, onOpenCashier = onOpenCashier)
        when {
            state.isLoading -> LoadingState(label = "Memuat ringkasan")
            state.error != null -> ErrorState(error = state.error, onRetry = onRetry)
            !state.hasAnySales -> DashboardEmptyState()
            else -> DashboardContent(state = state)
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
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Text(
            text = state.storeName.ifBlank { "POS UMKM Pro" },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text = "Halo, ${state.cashierName?.takeIf { it.isNotBlank() } ?: "Pemilik Toko"}",
            style = LocalAppTextStyles.current.screenTitle,
            color = Color.White,
        )
        Text(
            text = "Semangat terus! Berikut ringkasan bisnis Anda.",
            style = LocalAppTextStyles.current.supportingLabel,
            color = Color.White.copy(alpha = 0.75f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            HeaderAction(
                label = "Buka Kasir",
                icon = { Icon(Icons.Filled.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp), tint = PosColors.ForestGreen) },
                containerColor = PosColors.Gold,
                contentColor = PosColors.ForestGreen,
                onClick = onOpenCashier,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            DashboardRangePreset.entries.forEach { preset ->
                HeaderChip(
                    label = preset.label,
                    selected = preset == state.preset,
                    onClick = { onPresetSelected(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeaderAction(
    label: String,
    icon: @Composable () -> Unit,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()
            Text(
                text = label,
                style = LocalAppTextStyles.current.buttonLabel,
                color = contentColor,
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
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = modifier.clickable(onClick = onClick),
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
private fun DashboardContent(state: DashboardUiState) {
    Column(
        modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        MetricCard(
            label = "Total Penjualan",
            value = Rupiah.format(state.totalSales),
            icon = Icons.Filled.PointOfSale,
            deltaText = state.comparisonPercent?.let { percent ->
                val sign = if (percent >= 0) "+" else ""
                "$sign$percent% dari periode sebelumnya"
            },
            isPositive = (state.comparisonPercent ?: 0) >= 0,
            emphasized = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            MetricCard(
                label = "Transaksi",
                value = state.transactionCount.toString(),
                icon = Icons.Filled.Receipt,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Produk Terjual",
                value = formatQuantity(state.itemsSold),
                icon = Icons.Filled.Inventory2,
                modifier = Modifier.weight(1f),
            )
        }
        MetricCard(
            label = "Rata-rata Transaksi",
            value = Rupiah.format(state.averageTransaction),
            icon = Icons.Filled.ShoppingBag,
            modifier = Modifier.fillMaxWidth(),
        )
        PosCard(modifier = Modifier.fillMaxWidth()) {
            PosSectionHeader(title = "Grafik Penjualan")
            SalesLineChart(points = state.dailyPoints)
        }
        PosCard(modifier = Modifier.fillMaxWidth()) {
            PosSectionHeader(title = "Penjualan Terakhir")
            DailySalesBarChart(points = state.dailyPoints)
        }
        BestSellersCard(state.bestSellers)
        PaymentMethodsCard(state.paymentShares)
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
        bestSellers.forEach { seller ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                ProductThumbnail(category = seller.category, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seller.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${formatQuantity(seller.quantitySold)} terjual",
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = Rupiah.format(seller.salesValue),
                    style = LocalAppTextStyles.current.financialValue,
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodsCard(shares: List<PaymentShare>) {
    if (shares.isEmpty()) return
    val slices = shares.map { share ->
        ChartSlice(label = share.label, value = share.amount.toDouble(), color = paymentSliceColor(share.method))
    }
    PosCard(modifier = Modifier.fillMaxWidth()) {
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = shares.joinToString("  •  ") { "${it.label} ${it.transactionCount} trx" },
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DashboardEmptyState() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PosSpacing.md),
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
