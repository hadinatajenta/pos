package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import id.umkm.pos.pro.data.local.entity.ReturnEntity
import id.umkm.pos.pro.data.local.entity.ReturnLineEntity

@Dao
abstract class ReturnDao {

    @Insert
    abstract suspend fun insertReturn(record: ReturnEntity): Long

    @Insert
    abstract suspend fun insertLines(lines: List<ReturnLineEntity>): List<Long>

    @Query("SELECT * FROM returns WHERE transaction_id = :transactionId ORDER BY created_at DESC")
    abstract suspend fun getReturnsForTransaction(transactionId: Long): List<ReturnEntity>

    @Query("SELECT * FROM returns WHERE created_at BETWEEN :startMillis AND :endMillis ORDER BY created_at ASC")
    abstract suspend fun getReturnsInRange(startMillis: Long, endMillis: Long): List<ReturnEntity>

    @Query(
        "SELECT COALESCE(SUM(refund_amount), 0) FROM returns " +
            "WHERE created_at BETWEEN :startMillis AND :endMillis",
    )
    abstract suspend fun sumRefundsBetween(startMillis: Long, endMillis: Long): Long

    @Query("SELECT * FROM return_lines WHERE return_id = :returnId ORDER BY id ASC")
    abstract suspend fun getLines(returnId: Long): List<ReturnLineEntity>

    @Transaction
    open suspend fun insertReturnWithLines(
        record: ReturnEntity,
        lines: List<ReturnLineEntity>,
    ): Long {
        val returnId = insertReturn(record)
        insertLines(lines.map { line -> line.copy(returnId = returnId) })
        return returnId
    }
}
