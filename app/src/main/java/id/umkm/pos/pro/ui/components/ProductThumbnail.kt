package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import kotlin.math.abs

@Composable
fun ProductThumbnail(
    category: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    imagePath: String? = null,
    productName: String? = null,
) {
    val imageBitmap = rememberFileImageBitmap(imagePath)
    val (containerColor, contentColor) = thumbnailTone(category = category, productName = productName)
    val initials = rememberProductInitials(productName)
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(if (imageBitmap != null) LocalPosSemanticPalette.current.neutralContainer else containerColor),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageBitmap != null -> {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            initials != null -> {
                val fontSize = if (size >= 56.dp) 16.sp else 14.sp
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = contentColor,
                    maxLines = 1,
                )
            }

            else -> {
                Icon(
                    painter = painterResource(iconResForCategory(category)),
                    contentDescription = null,
                    modifier = Modifier.size(size / 2),
                    tint = contentColor,
                )
            }
        }
    }
}

@Composable
private fun thumbnailTone(
    category: String?,
    productName: String?,
): Pair<Color, Color> {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val seed = (productName?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        ?: category?.trim()?.lowercase().orEmpty())
    val bucket = abs(seed.hashCode()) % 6
    return if (!isDark) {
        when (bucket) {
            0 -> Color(0xFFE2F5EC) to Color(0xFF0B5D44) // Soft Mint Emerald
            1 -> Color(0xFFE8F1FF) to Color(0xFF1D4ED8) // Soft Royal Blue
            2 -> Color(0xFFFEF3C7) to Color(0xFF9A3412) // Warm Amber
            3 -> Color(0xFFF3E8FF) to Color(0xFF6D28D9) // Soft Violet
            4 -> Color(0xFFE0F2FE) to Color(0xFF0369A1) // Crisp Sky Teal
            else -> Color(0xFFFFE4E6) to Color(0xFFBE123C) // Soft Rose
        }
    } else {
        when (bucket) {
            0 -> Color(0xFF13392C) to Color(0xFF6EE7B7)
            1 -> Color(0xFF172B4D) to Color(0xFF93C5FD)
            2 -> Color(0xFF3B2A10) to Color(0xFFFCD34D)
            3 -> Color(0xFF2E1B4B) to Color(0xFFD8B4FE)
            4 -> Color(0xFF123243) to Color(0xFF7DD3FC)
            else -> Color(0xFF3F1A24) to Color(0xFFFDA4AF)
        }
    }
}

private fun rememberProductInitials(productName: String?): String? {
    val cleaned = productName?.trim().orEmpty()
    if (cleaned.isEmpty()) return null
    val words = cleaned
        .split(Regex("\\s+"))
        .mapNotNull { word -> word.firstOrNull { char -> char.isLetterOrDigit() } }
    return when {
        words.size >= 2 -> "${words[0]}${words[1]}".uppercase()
        cleaned.length >= 2 -> cleaned.take(2).uppercase()
        else -> cleaned.uppercase()
    }
}

private fun iconResForCategory(category: String?): Int {
    val normalized = category?.trim()?.lowercase().orEmpty()
    return when {
        normalized.contains("kopi") -> R.drawable.ic_coffee
        normalized.contains("minuman") -> R.drawable.ic_cup_soda
        normalized.contains("nasi") -> R.drawable.ic_utensils
        normalized.contains("roti") -> R.drawable.ic_croissant
        normalized.contains("makanan") -> R.drawable.ic_utensils_crossed
        normalized.contains("rokok") -> R.drawable.ic_cigarette
        else -> R.drawable.ic_package
    }
}
