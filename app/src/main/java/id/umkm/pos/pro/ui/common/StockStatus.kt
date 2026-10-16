package id.umkm.pos.pro.ui.common

import id.umkm.pos.pro.core.model.Product

enum class StockStatus {
    OVERSOLD,
    OUT_OF_STOCK,
    LOW_STOCK,
    IN_STOCK,
}

fun resolveStockStatus(product: Product): StockStatus = when {
    product.stockBaseQty.isFinite() && product.stockBaseQty < 0.0 -> StockStatus.OVERSOLD
    product.stockBaseQty <= 0.0 -> StockStatus.OUT_OF_STOCK
    product.stockBaseQty <= product.minStockAlert -> StockStatus.LOW_STOCK
    else -> StockStatus.IN_STOCK
}

fun stockStatusLabel(status: StockStatus): String = when (status) {
    StockStatus.OVERSOLD -> "Oversold"
    StockStatus.OUT_OF_STOCK -> "Out of stock"
    StockStatus.LOW_STOCK -> "Low stock"
    StockStatus.IN_STOCK -> "In stock"
}
