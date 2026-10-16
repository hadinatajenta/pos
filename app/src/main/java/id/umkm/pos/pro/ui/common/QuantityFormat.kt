package id.umkm.pos.pro.ui.common

import java.util.Locale
import kotlin.math.floor
import kotlin.math.round

private const val QUANTITY_DECIMALS = 3
private const val DECIMAL_SCALE = 1000.0

fun formatQuantity(quantity: Double): String {
    if (quantity.isNaN() || quantity.isInfinite()) return "0"
    val scaled = round(quantity * DECIMAL_SCALE) / DECIMAL_SCALE
    if (scaled == floor(scaled)) return scaled.toLong().toString()
    return String.format(Locale.US, "%.${QUANTITY_DECIMALS}f", scaled).trimEnd('0').trimEnd('.')
}

fun formatQuantityWithUnit(quantity: Double, unit: String): String =
    "${formatQuantity(quantity)} $unit"
