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
    suspend fun transactionTotals(startMillis: Long, endMillis: Long): SalesSummaryAggregate

    @Query(
        "SELECT COALESCE(SUM(refund_amount), 0) AS refund_total, " +
            "COALESCE(SUM(total_hpp_reversed), 0) AS hpp_reversed_total " +
            "FROM returns WHERE created_at BETWEEN :startMillis AND :endMillis",
    )
    suspend fun returnsTotals(startMillis: Long, endMillis: Long): ReturnsTotalsAggregate

    @Query(
        "SELECT CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) AS epoch_day, " +
            "COALESCE(SUM(subtotal - invoice_discount), 0) AS total_sales, " +
            "COALESCE(SUM(net_profit), 0) AS net_profit, " +
            "COUNT(*) AS transaction_count " +
            "FROM transactions WHERE created_at BETWEEN :startMillis AND :endMillis " +
            "GROUP BY CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) " +
            "ORDER BY CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) ASC",
    )
    suspend fun transactionTotalsByDay(startMillis: Long, endMillis: Long): List<DailySalesAggregate>

    @Query(
        "SELECT CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) AS epoch_day, " +
            "COALESCE(SUM(refund_amount), 0) AS refund_total, " +
            "COALESCE(SUM(refund_amount - total_hpp_reversed), 0) AS net_refund " +
            "FROM returns WHERE created_at BETWEEN :startMillis AND :endMillis " +
            "GROUP BY CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) " +
            "ORDER BY CAST((julianday(created_at / 1000.0, 'unixepoch', 'localtime') - 2440587.5) AS INTEGER) ASC",
    )
    suspend fun returnsTotalsByDay(startMillis: Long, endMillis: Long): List<DailyReturnsAggregate>
}

data class SalesSummaryAggregate(
    @ColumnInfo(name = "total_sales") val totalSales: Long,
    @ColumnInfo(name = "total_hpp") val totalHpp: Long,
    @ColumnInfo(name = "net_profit") val netProfit: Long,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
)

data class ReturnsTotalsAggregate(
    @ColumnInfo(name = "refund_total") val refundTotal: Long,
    @ColumnInfo(name = "hpp_reversed_total") val hppReversedTotal: Long,
)

data class DailySalesAggregate(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "total_sales") val totalSales: Long,
    @ColumnInfo(name = "net_profit") val netProfit: Long,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
)

data class DailyReturnsAggregate(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "refund_total") val refundTotal: Long,
    @ColumnInfo(name = "net_refund") val netRefund: Long,
)
