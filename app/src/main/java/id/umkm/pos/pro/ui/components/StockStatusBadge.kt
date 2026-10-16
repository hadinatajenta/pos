package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.stockStatusLabel
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
fun StockStatusBadge(
    status: StockStatus,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPosSemanticPalette.current
    val containerColor: Color
    val contentColor: Color
    val icon: ImageVector
    when (status) {
        StockStatus.IN_STOCK -> {
            containerColor = palette.successContainer
            contentColor = palette.success
            icon = Icons.Filled.CheckCircle
        }

        StockStatus.LOW_STOCK -> {
            containerColor = palette.warningContainer
            contentColor = palette.warning
            icon = Icons.Filled.WarningAmber
        }

        StockStatus.OUT_OF_STOCK -> {
            containerColor = palette.dangerContainer
            contentColor = palette.danger
            icon = Icons.Filled.Cancel
        }

        StockStatus.OVERSOLD -> {
            containerColor = palette.dangerContainer
            contentColor = palette.danger
            icon = Icons.Filled.TrendingDown
        }
    }
    val label = stockStatusLabel(status)
    Box(
        modifier = modifier
            .background(color = containerColor, shape = MaterialTheme.shapes.small)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = contentColor)
            Text(text = label, style = LocalAppTextStyles.current.supportingLabel, color = contentColor)
        }
    }
}
