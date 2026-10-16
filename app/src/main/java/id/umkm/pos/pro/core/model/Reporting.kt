package id.umkm.pos.pro.core.model

data class ReportRange(
    val startMillis: Long,
    val endMillis: Long,
)

data class SalesSummary(
    val totalSales: Long,
    val totalHpp: Long,
    val netProfit: Long,
    val transactionCount: Int,
)

data class DailySales(
    val epochDay: Long,
    val totalSales: Long,
    val netProfit: Long,
    val transactionCount: Int,
)
