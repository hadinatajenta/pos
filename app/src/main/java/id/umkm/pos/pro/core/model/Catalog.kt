package id.umkm.pos.pro.core.model

data class Product(
    val id: Long,
    val sku: String,
    val barcode: String?,
    val name: String,
    val category: String?,
    val baseUnit: String,
    val baseHpp: Long,
    val basePrice: Long,
    val stockBaseQty: Double,
    val minStockAlert: Double,
    val isActive: Boolean,
)

data class ProductUnit(
    val id: Long,
    val productId: Long,
    val variantName: String,
    val conversionFactor: Double,
    val customBarcode: String?,
    val unitPrice: Long,
    val wholesaleMinQty: Int?,
    val wholesalePrice: Long?,
)
