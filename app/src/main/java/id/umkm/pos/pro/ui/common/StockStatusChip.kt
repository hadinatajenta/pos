package id.umkm.pos.pro.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun StockStatusChip(
    status: StockStatus,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val icon: ImageVector = when (status) {
        StockStatus.OUT_OF_STOCK -> Icons.Filled.Block
        StockStatus.LOW_STOCK -> Icons.Filled.WarningAmber
        StockStatus.IN_STOCK -> Icons.Filled.CheckCircle
    }
    val containerColor = when (status) {
        StockStatus.OUT_OF_STOCK -> colorScheme.errorContainer
        StockStatus.LOW_STOCK -> colorScheme.tertiaryContainer
        StockStatus.IN_STOCK -> colorScheme.secondaryContainer
    }
    val contentColor = when (status) {
        StockStatus.OUT_OF_STOCK -> colorScheme.onErrorContainer
        StockStatus.LOW_STOCK -> colorScheme.onTertiaryContainer
        StockStatus.IN_STOCK -> colorScheme.onSecondaryContainer
    }
    StatusChip(
        icon = icon,
        label = stockStatusLabel(status),
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    )
}
