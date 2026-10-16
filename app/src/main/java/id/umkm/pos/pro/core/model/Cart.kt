package id.umkm.pos.pro.core.model

data class CartLine(
    val lineId: String,
    val productId: Long,
    val unitId: Long?,
    val variantName: String?,
    val qty: Double,
    val negotiatedUnitPrice: Long?,
    val itemDiscount: Long,
)

data class PricingContext(
    val invoiceDiscount: Long = 0L,
    val taxRatePercent: Int = 0,
    val roundingIncrement: Long = 0L,
)

data class PricedCartLine(
    val line: CartLine,
    val product: Product,
    val unitPrice: Long,
    val isNegotiated: Boolean,
    val isWholesale: Boolean,
    val itemSubtotal: Long,
    val unitHpp: Long,
    val conversionFactor: Double = 1.0,
)

data class PricedCart(
    val lines: List<PricedCartLine>,
    val subtotal: Long,
    val invoiceDiscount: Long,
    val taxTotal: Long,
    val rounding: Long,
    val grandTotal: Long,
    val totalHpp: Long,
    val netProfit: Long,
)
