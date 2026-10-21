package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.umkm.pos.pro.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {

    @Insert
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long

    @Query("SELECT * FROM debt_payments WHERE customer_id = :customerId ORDER BY paid_at DESC")
    fun observeDebtPayments(customerId: Long): Flow<List<DebtPaymentEntity>>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM debt_payments " +
            "WHERE payment_method = 'CASH' AND paid_at BETWEEN :startMillis AND :endMillis",
    )
    suspend fun sumCashPaymentsBetween(startMillis: Long, endMillis: Long): Long
}
