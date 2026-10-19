package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.stockStatusLabel
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.R
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource

@Composable
fun StockStatusBadge(
    status: StockStatus,
    modifier: Modifier = Modifier,
) {
    val visuals = stockStatusVisuals(status)
    val label = stockStatusLabel(status, LocalPosStrings.current)
    Box(
        modifier = modifier
            .background(color = visuals.containerColor, shape = MaterialTheme.shapes.small)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Icon(
                painter = painterResource(visuals.iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = visuals.contentColor,
            )
            Text(text = label, style = LocalAppTextStyles.current.supportingLabel, color = visuals.contentColor)
        }
    }
}

@Immutable
internal data class StockStatusVisuals(
    val containerColor: Color,
    val contentColor: Color,
    @DrawableRes val iconRes: Int,
)

@Composable
internal fun stockStatusVisuals(status: StockStatus): StockStatusVisuals {
    val palette = LocalPosSemanticPalette.current
    return when (status) {
        StockStatus.IN_STOCK -> StockStatusVisuals(
            containerColor = palette.successContainer,
            contentColor = palette.successText,
            iconRes = R.drawable.ic_circle_check,
        )

        StockStatus.LOW_STOCK -> StockStatusVisuals(
            containerColor = palette.warningContainer,
            contentColor = palette.warningText,
            iconRes = R.drawable.ic_triangle_alert,
        )

        StockStatus.OUT_OF_STOCK -> StockStatusVisuals(
            containerColor = palette.dangerContainer,
            contentColor = palette.dangerText,
            iconRes = R.drawable.ic_circle_x,
        )

        StockStatus.OVERSOLD -> StockStatusVisuals(
            containerColor = palette.dangerContainer,
            contentColor = palette.dangerText,
            iconRes = R.drawable.ic_trending_down,
        )
    }
}
