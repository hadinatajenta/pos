package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette

@Composable
fun ProductThumbnail(
    category: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    imagePath: String? = null,
) {
    val imageBitmap = rememberFileImageBitmap(imagePath)
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(LocalPosSemanticPalette.current.neutralContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = iconForCategory(category),
                contentDescription = null,
                modifier = Modifier.size(size / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun iconForCategory(category: String?): ImageVector = when (category?.trim()?.lowercase()) {
    "minuman" -> Icons.Filled.LocalDrink
    "kopi" -> Icons.Filled.Coffee
    "makanan" -> Icons.Filled.Restaurant
    "nasi" -> Icons.Filled.RiceBowl
    "roti" -> Icons.Filled.BakeryDining
    "rokok" -> Icons.Filled.SmokingRooms
    else -> Icons.Filled.Inventory2
}
