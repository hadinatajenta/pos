package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "returns",
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
        Index(value = ["created_at"]),
    ],
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "refund_amount") val refundAmount: Long,
    @ColumnInfo(name = "total_hpp_reversed") val totalHppReversed: Long,
    @ColumnInfo(name = "note") val note: String?,
)
