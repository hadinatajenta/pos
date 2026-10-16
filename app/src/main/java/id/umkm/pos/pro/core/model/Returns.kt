package id.umkm.pos.pro.core.model

data class ReturnRecord(
    val id: Long,
    val transactionId: Long,
    val createdAt: Long,
    val refundAmount: Long,
    val totalHppReversed: Long,
    val note: String?,
)

data class ReturnLine(
    val id: Long,
    val returnId: Long,
    val transactionItemId: Long?,
    val productId: Long,
    val variantName: String?,
    val qty: Double,
    val conversionFactor: Double,
    val refundAmount: Long,
    val hppReversed: Long,
    val baseQtyRestocked: Double,
)
