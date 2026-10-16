package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.R
import androidx.compose.ui.res.painterResource

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
                painter = painterResource(iconResForCategory(category)),
                contentDescription = null,
                modifier = Modifier.size(size / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun iconResForCategory(category: String?): Int = when (category?.trim()?.lowercase()) {
    "minuman" -> R.drawable.ic_cup_soda
    "kopi" -> R.drawable.ic_coffee
    "makanan" -> R.drawable.ic_utensils_crossed
    "nasi" -> R.drawable.ic_utensils
    "roti" -> R.drawable.ic_croissant
    "rokok" -> R.drawable.ic_cigarette
    else -> R.drawable.ic_package
}
