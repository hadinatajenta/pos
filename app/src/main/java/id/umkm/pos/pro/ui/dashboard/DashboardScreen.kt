package id.umkm.pos.pro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.ChartLegend
import id.umkm.pos.pro.ui.components.ChartSlice
import id.umkm.pos.pro.ui.components.DailySalesBarChart
import id.umkm.pos.pro.ui.components.PaymentMethodDonut
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.paymentSliceColor
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R

@Composable
internal fun DashboardScreen(
    state: DashboardUiState,
    onOpenCashier: () -> Unit,
    onOpenProducts: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DashboardHeader(state = state, onOpenCashier = onOpenCashier)
        when {
            state.isLoading -> LoadingState(label = "Memuat ringkasan")
            state.error != null -> ErrorState(error = state.error, onRetry = onRetry)
            else -> DashboardContent(state = state, onOpenProducts = onOpenProducts)
        }
    }
}

@Composable
private fun DashboardHeader(
    state: DashboardUiState,
    onOpenCashier: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PosColors.ForestGreen)
            .padding(horizontal = PosSpacing.md)
            .padding(top = PosSpacing.md, bottom = PosSpacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(PosColors.Gold),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_store),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PosColors.ForestGreen,
                )
            }
            Text(
                text = state.storeName.ifBlank { "POS UMKM Pro" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = PosSpacing.sm),
            )
        }
        Text(
            text = "Halo, ${state.cashierName?.takeIf { it.isNotBlank() } ?: "Pemilik Toko"}",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(top = PosSpacing.lg),
        )
        Text(
            text = "Semangat terus! Bisnis makin berkembang.",
            style = LocalAppTextStyles.current.supportingLabel,
            color = Color.White.copy(alpha = 0.72f),
        )
        Box(
            modifier = Modifier
                .padding(top = PosSpacing.sm)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onOpenCashier),
        ) {
            Text(
                text = "Buka Kasir",
                style = LocalAppTextStyles.current.supportingLabel,
                color = PosColors.Gold,
                modifier = Modifier.padding(vertical = PosSpacing.xs),
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onOpenProducts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .offset(y = (-PosSpacing.lg))
            .padding(horizontal = PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        SummaryCard(state = state)
        PosCard(modifier = Modifier.fillMaxWidth()) {
            PosSectionHeader(title = "Penjualan 7 Hari Terakhir")
            DailySalesBarChart(points = state.dailyPoints)
        }
        BestSellersCard(bestSellers = state.bestSellers, onOpenProducts = onOpenProducts)
        PaymentMethodsCard(state.paymentShares)
    }
}

@Composable
private fun SummaryCard(state: DashboardUiState) {
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Penjualan Hari Ini",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AutoSizeNumberText(
                    text = Rupiah.format(state.totalSales),
                    style = LocalAppTextStyles.current.financialValueLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.comparisonPercent != null) {
                    DeltaLabel(percent = state.comparisonPercent)
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(LocalPosSemanticPalette.current.goldContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chart_line),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = PosSpacing.sm),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCell(
                label = "Jumlah Transaksi",
                value = state.transactionCount.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricDivider()
            MetricCell(
                label = "Produk Terjual",
                value = formatQuantity(state.itemsSold),
                modifier = Modifier.weight(1f),
            )
            MetricDivider()
            MetricCell(
                label = "Rata-rata Transaksi",
                value = Rupiah.format(state.averageTransaction),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = PosSpacing.xs)) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AutoSizeNumberText(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PosSpacing.xxs),
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun DeltaLabel(percent: Int) {
    val palette = LocalPosSemanticPalette.current
    val isPositive = percent >= 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        modifier = Modifier.padding(top = PosSpacing.xxs),
    ) {
        Icon(
            painter = painterResource(if (isPositive) R.drawable.ic_trending_up else R.drawable.ic_trending_down),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isPositive) palette.success else palette.danger,
        )
        Text(
            text = "${if (isPositive) "+" else ""}$percent% dari periode sebelumnya",
            style = LocalAppTextStyles.current.supportingLabel,
            color = if (isPositive) palette.success else palette.danger,
        )
    }
}

@Composable
private fun BestSellersCard(
    bestSellers: List<BestSeller>,
    onOpenProducts: () -> Unit,
) {
    PosCard(modifier = Modifier.fillMaxWidth()) {
        PosSectionHeader(
            title = "Produk Terlaris",
            action = {
                Text(
                    text = "Lihat Semua",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onOpenProducts)
                        .padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
                )
            },
        )
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
                Column(modifier = Modifier.width(112.dp)) {
                    ProductThumbnail(category = seller.category, size = 72.dp)
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
                painter = painterResource(R.drawable.ic_store),
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
