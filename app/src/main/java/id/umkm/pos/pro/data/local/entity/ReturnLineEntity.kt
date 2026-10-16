package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "return_lines",
    foreignKeys = [
        ForeignKey(
            entity = ReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["return_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["return_id"]),
        Index(value = ["transaction_item_id"]),
        Index(value = ["product_id"]),
    ],
)
data class ReturnLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "return_id") val returnId: Long,
    @ColumnInfo(name = "transaction_item_id") val transactionItemId: Long?,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "variant_name") val variantName: String?,
    @ColumnInfo(name = "qty") val qty: Double,
    @ColumnInfo(name = "conversion_factor") val conversionFactor: Double,
    @ColumnInfo(name = "refund_amount") val refundAmount: Long,
    @ColumnInfo(name = "hpp_reversed") val hppReversed: Long,
    @ColumnInfo(name = "base_qty_restocked") val baseQtyRestocked: Double,
)
