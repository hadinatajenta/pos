package id.umkm.pos.pro.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReportDao {

    @Query(
        "SELECT COALESCE(SUM(subtotal - invoice_discount), 0) AS total_sales, " +
            "COALESCE(SUM(total_hpp), 0) AS total_hpp, " +
            "COALESCE(SUM(net_profit), 0) AS net_profit, " +
            "COUNT(*) AS transaction_count " +
            "FROM transactions WHERE created_at BETWEEN :startMillis AND :endMillis",
    )
    suspend fun salesSummary(startMillis: Long, endMillis: Long): SalesSummaryAggregate

    @Query(
        "SELECT CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) AS epoch_day, " +
            "COALESCE(SUM(subtotal - invoice_discount), 0) AS total_sales, " +
            "COALESCE(SUM(net_profit), 0) AS net_profit, " +
            "COUNT(*) AS transaction_count " +
            "FROM transactions WHERE created_at BETWEEN :startMillis AND :endMillis " +
            "GROUP BY CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) " +
            "ORDER BY CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) ASC",
    )
    suspend fun salesByDay(startMillis: Long, endMillis: Long): List<DailySalesAggregate>
}

data class SalesSummaryAggregate(
    @ColumnInfo(name = "total_sales") val totalSales: Long,
    @ColumnInfo(name = "total_hpp") val totalHpp: Long,
    @ColumnInfo(name = "net_profit") val netProfit: Long,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
)

data class DailySalesAggregate(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "total_sales") val totalSales: Long,
    @ColumnInfo(name = "net_profit") val netProfit: Long,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
)
