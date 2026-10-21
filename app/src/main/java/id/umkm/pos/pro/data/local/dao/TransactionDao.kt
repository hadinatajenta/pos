package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.entity.TransactionEntity
import id.umkm.pos.pro.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransactionDao {

    @Insert
    abstract suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert
    abstract suspend fun insertItems(items: List<TransactionItemEntity>): List<Long>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    abstract suspend fun getTransaction(transactionId: Long): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId ORDER BY id ASC")
    abstract suspend fun getItems(transactionId: Long): List<TransactionItemEntity>

    @Query(
        "SELECT * FROM transactions WHERE created_at BETWEEN :startMillis AND :endMillis " +
            "ORDER BY created_at DESC",
    )
    abstract fun observeTransactions(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE created_at BETWEEN :startMillis AND :endMillis " +
            "ORDER BY created_at ASC",
    )
    abstract suspend fun transactionsInRange(startMillis: Long, endMillis: Long): List<TransactionEntity>

    @Query(
        "SELECT COALESCE(SUM(" +
            "CASE " +
            "WHEN payment_method = 'CASH' THEN paid_amount - change_amount " +
            "WHEN payment_method IN ('SPLIT', 'KASBON') THEN paid_amount " +
            "ELSE 0 " +
            "END), 0) FROM transactions " +
            "WHERE created_at BETWEEN :startMillis AND :endMillis",
    )
    abstract suspend fun sumCashReceivedBetween(startMillis: Long, endMillis: Long): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE created_at BETWEEN :startMillis AND :endMillis")
    abstract suspend fun countTransactionsBetween(startMillis: Long, endMillis: Long): Int

    @Query("UPDATE transactions SET status = :status WHERE id = :transactionId")
    abstract suspend fun updateStatus(transactionId: Long, status: TransactionStatus)

    @Query("UPDATE transactions SET remaining_debt = :remainingDebt, status = :status WHERE id = :transactionId")
    abstract suspend fun updateDebtState(transactionId: Long, remainingDebt: Long, status: TransactionStatus)

    @Query("DELETE FROM transactions WHERE notes = :note")
    abstract suspend fun deleteTransactionsByNote(note: String): Int

    @Transaction
    open suspend fun insertTransactionWithItems(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
    ): Long {
        val generatedId = insertTransaction(transaction)
        insertItems(items.map { item -> item.copy(transactionId = generatedId) })
        return generatedId
    }

    @Transaction
    open suspend fun markTransactionReturned(
        transactionId: Long,
        returnedItems: List<TransactionItemEntity>,
    ) {
        updateStatus(transactionId, TransactionStatus.RETURNED)
        if (returnedItems.isNotEmpty()) {
            insertItems(returnedItems.map { item -> item.copy(transactionId = transactionId) })
        }
    }
}
