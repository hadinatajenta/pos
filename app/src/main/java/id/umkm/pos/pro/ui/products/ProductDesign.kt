package id.umkm.pos.pro.ui.products

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.theme.PosColors
import kotlin.math.roundToLong

object ProductPalette {
    val Background = PosColors.Background
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF0F1A15)
    val Muted = Color(0xFF6F7B75)
    val MutedText = Color(0xFF68746E)
    val Price = Color(0xFF13796B)
    val Line = Color(0xFFE6E9E3)
    val Brand = Color(0xFF0D5A44)
    val BrandDeep = Color(0xFF052A20)
    val Focus = Color(0xFF14805F)
    val FocusRing = Color(0x2414805F)
    val Gold = Color(0xFFE3B94F)
    val OnGold = Color(0xFF1B1405)
    val NavSurface = Color(0xFF0F1A15)
    val SafeText = Color(0xFF13795B)
    val SafeContainer = Color(0xFFDDF3E9)
    val LowText = Color(0xFFA9570A)
    val LowContainer = Color(0xFFFDEBCB)
    val OutText = Color(0xFFB9301F)
    val OutContainer = Color(0xFFFBDEDA)
    val Glass = Color(0xE6FFFFFF)
    val GlassBorder = Color(0x2EFFFFFF)
}

object ProductRadii {
    val card = RoundedCornerShape(22.dp)
    val tile = RoundedCornerShape(20.dp)
    val section = RoundedCornerShape(24.dp)
    val input = RoundedCornerShape(16.dp)
    val stepper = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(18.dp)
    val heroBottom = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)
    val navigation = RoundedCornerShape(28.dp)
    val drawer = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    val pill = RoundedCornerShape(percent = 50)
}

enum class ProductStockState {
    OK,
    LOW,
    OUT,
}

fun resolveProductStockState(product: Product): ProductStockState = when {
    !product.stockBaseQty.isFinite() -> ProductStockState.OK
    product.stockBaseQty <= 0.0 -> ProductStockState.OUT
    product.stockBaseQty <= product.minStockAlert -> ProductStockState.LOW
    else -> ProductStockState.OK
}

fun formatCompactRupiah(amount: Long): String {
    if (amount < 1_000_000L) return Rupiah.format(amount)
    val isBillion = amount >= 1_000_000_000L
    val roundDivisor = if (isBillion) 10_000_000L else 10_000L
    val suffix = if (isBillion) " M" else " jt"
    val hundredths = (amount + roundDivisor / 2L) / roundDivisor
    val whole = hundredths / 100L
    val fraction = hundredths % 100L
    val fractionText = fraction.toString().padStart(2, '0')
    return "Rp ${groupDigits(whole)},$fractionText$suffix"
}

fun stockValueOf(products: Iterable<Product>): Long = products.sumOf { product ->
    val finiteStock = product.stockBaseQty.takeIf { it.isFinite() } ?: 0.0
    (finiteStock.coerceAtLeast(0.0) * product.basePrice).roundToLong()
}

fun categoryColorKey(category: String?): String {
    val normalized = category?.trim()?.lowercase().orEmpty()
    return when {
        normalized.isEmpty() -> CATEGORY_COLOR_UNCATEGORIZED
        normalized.contains("makanan") -> CATEGORY_COLOR_FOOD
        normalized.contains("minuman") -> CATEGORY_COLOR_DRINK
        normalized.contains("sembako") -> CATEGORY_COLOR_STAPLES
        normalized.contains("kebersihan") -> CATEGORY_COLOR_CLEANING
        else -> "palette-${categoryPaletteIndex(normalized)}"
    }
}

fun categoryGradient(key: String): Pair<Color, Color> = when (key) {
    CATEGORY_COLOR_FOOD -> Color(0xFFFFC38A) to Color(0xFFFF8A4C)
    CATEGORY_COLOR_DRINK -> Color(0xFF9CC8FF) to Color(0xFF4C86EE)
    CATEGORY_COLOR_STAPLES -> Color(0xFFB6E8CD) to Color(0xFF3FB583)
    CATEGORY_COLOR_CLEANING -> Color(0xFFD8C8FF) to Color(0xFF8B6CF0)
    CATEGORY_COLOR_UNCATEGORIZED -> Color(0xFFD5DDD8) to Color(0xFFA9B8B0)
    else -> CATEGORY_PALETTE[paletteIndexFromKey(key)]
}

private fun categoryPaletteIndex(category: String): Int {
    var hash = 2_166_136_261L
    category.forEach { character ->
        hash = hash xor character.code.toLong()
        hash *= 16_777_619L
    }
    return ((hash ushr 16).toInt() and Int.MAX_VALUE) % CATEGORY_PALETTE.size
}

private fun paletteIndexFromKey(key: String): Int {
    val index = key.removePrefix("palette-").toIntOrNull() ?: 0
    return index.coerceIn(0, CATEGORY_PALETTE.lastIndex)
}

private fun groupDigits(value: Long): String = value
    .toString()
    .reversed()
    .chunked(3)
    .joinToString(separator = ".")
    .reversed()

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

internal const val CATEGORY_COLOR_FOOD = "makanan"
internal const val CATEGORY_COLOR_DRINK = "minuman"
internal const val CATEGORY_COLOR_STAPLES = "sembako"
internal const val CATEGORY_COLOR_CLEANING = "kebersihan"
internal const val CATEGORY_COLOR_UNCATEGORIZED = "tanpa-kategori"

private val CATEGORY_PALETTE: List<Pair<Color, Color>> = listOf(
    Color(0xFFF7D3AB) to Color(0xFFE7A55E),
    Color(0xFFC9E9DA) to Color(0xFF6FBF9B),
    Color(0xFFE6D5F5) to Color(0xFFB58BDB),
    Color(0xFFF8D3D9) to Color(0xFFE08798),
    Color(0xFFCFE0F7) to Color(0xFF7FA8D9),
    Color(0xFFF3E3B8) to Color(0xFFD9BC6A),
)
