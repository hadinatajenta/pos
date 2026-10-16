package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_items",
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
        Index(value = ["product_id"]),
    ],
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "variant_name") val variantName: String?,
    @ColumnInfo(name = "qty") val qty: Double,
    @ColumnInfo(name = "unit_hpp") val unitHpp: Long,
    @ColumnInfo(name = "applied_unit_price") val appliedUnitPrice: Long,
    @ColumnInfo(name = "is_nego_price") val isNegoPrice: Boolean,
    @ColumnInfo(name = "is_wholesale_applied") val isWholesaleApplied: Boolean,
    @ColumnInfo(name = "item_discount") val itemDiscount: Long,
    @ColumnInfo(name = "subtotal") val subtotal: Long,
    @ColumnInfo(name = "conversion_factor") val conversionFactor: Double?,
)
