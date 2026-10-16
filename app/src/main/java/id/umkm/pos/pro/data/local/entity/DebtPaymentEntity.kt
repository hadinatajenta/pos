package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import id.umkm.pos.pro.core.model.PaymentMethod

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["customer_id"]),
        Index(value = ["paid_at"]),
    ],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "customer_id") val customerId: Long,
    @ColumnInfo(name = "paid_at") val paidAt: Long,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod,
    @ColumnInfo(name = "note") val note: String?,
)
