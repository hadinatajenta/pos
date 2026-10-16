package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_variants_units",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["custom_barcode"], unique = true),
    ],
)
data class ProductUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "variant_name") val variantName: String,
    @ColumnInfo(name = "conversion_factor") val conversionFactor: Double,
    @ColumnInfo(name = "custom_barcode") val customBarcode: String?,
    @ColumnInfo(name = "unit_price") val unitPrice: Long,
    @ColumnInfo(name = "wholesale_min_qty") val wholesaleMinQty: Int?,
    @ColumnInfo(name = "wholesale_price") val wholesalePrice: Long?,
)
