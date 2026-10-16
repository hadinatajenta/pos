package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

private val STAT_CARD_MIN_WIDTH = 148.dp
private val STAT_BADGE_SIZE = 26.dp

private data class StatBadge(
    val label: String,
    val value: String,
    val supportingText: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
internal fun ProductsStatsRow(
    stats: ProductStats,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPosSemanticPalette.current
    val badges = listOf(
        StatBadge(
            label = "Total Produk",
            value = stats.totalCount.toString(),
            supportingText = totalSupportText(stats),
            icon = Icons.Filled.Inventory2,
            containerColor = palette.successContainer,
            contentColor = palette.success,
        ),
        StatBadge(
            label = "Kategori",
            value = stats.categoryCount.toString(),
            supportingText = categorySupportText(stats),
            icon = Icons.Filled.Category,
            containerColor = palette.successContainer,
            contentColor = palette.success,
        ),
        StatBadge(
            label = "Stok Menipis",
            value = stats.lowStockCount.toString(),
            supportingText = "Perlu perhatian",
            icon = Icons.Filled.WarningAmber,
            containerColor = palette.warningContainer,
            contentColor = palette.warning,
        ),
        StatBadge(
            label = "Stok Habis",
            value = stats.outOfStockCount.toString(),
            supportingText = "Segera restok",
            icon = Icons.Filled.Cancel,
            containerColor = palette.dangerContainer,
            contentColor = palette.danger,
        ),
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = if (maxWidth >= statRowMinWidth(badges.size)) badges.size else 2
        Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            badges.chunked(columnCount).forEach { badgeRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                ) {
                    badgeRow.forEach { badge ->
                        StatCard(
                            badge = badge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columnCount - badgeRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    badge: StatBadge,
    modifier: Modifier = Modifier,
) {
    PosCard(modifier = modifier, contentPadding = PosSpacing.sm) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Text(
                text = badge.label,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(STAT_BADGE_SIZE)
                    .clip(CircleShape)
                    .background(badge.containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = badge.icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = badge.contentColor,
                )
            }
        }
        Text(
            text = badge.value,
            style = LocalAppTextStyles.current.financialValue,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = badge.supportingText,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun statRowMinWidth(cardCount: Int): Dp = STAT_CARD_MIN_WIDTH * cardCount + PosSpacing.sm * (cardCount - 1)

private fun totalSupportText(stats: ProductStats): String {
    val inactiveCount = stats.totalCount - stats.activeCount
    return if (inactiveCount <= 0) "Semua aktif" else "$inactiveCount nonaktif"
}

private fun categorySupportText(stats: ProductStats): String =
    if (stats.uncategorizedCount <= 0) "Semua terkategori" else "${stats.uncategorizedCount} tanpa kategori"
