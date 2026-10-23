package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["barcode"], unique = true),
        Index(value = ["name"]),
    ],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "sku") val sku: String,
    @ColumnInfo(name = "barcode") val barcode: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "base_unit") val baseUnit: String,
    @ColumnInfo(name = "base_hpp") val baseHpp: Long,
    @ColumnInfo(name = "base_price") val basePrice: Long,
    @ColumnInfo(name = "stock_base_qty") val stockBaseQty: Double,
    @ColumnInfo(name = "min_stock_alert") val minStockAlert: Double,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @ColumnInfo(name = "icon") val icon: String? = null,
)
