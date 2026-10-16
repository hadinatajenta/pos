package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.common.StockStatus

enum class ProductFormField {
    NAME,
    SKU,
    BARCODE,
    CATEGORY,
    BASE_UNIT,
    BASE_PRICE,
    BASE_HPP,
    STOCK_QUANTITY,
    MIN_STOCK_ALERT,
    VARIANT_NAME,
    CONVERSION_FACTOR,
    VARIANT_PRICE,
    WHOLESALE_MIN_QTY,
    WHOLESALE_PRICE,
}

enum class ProductStockFilter(val label: String) {
    ALL("Semua"),
    IN_STOCK("Stok Tersedia"),
    LOW_STOCK("Stok Menipis"),
    OUT_OF_STOCK("Stok Habis");

    fun matches(status: StockStatus): Boolean = when (this) {
        ALL -> true
        IN_STOCK -> status == StockStatus.IN_STOCK
        LOW_STOCK -> status == StockStatus.LOW_STOCK
        OUT_OF_STOCK -> status == StockStatus.OUT_OF_STOCK || status == StockStatus.OVERSOLD
    }
}

data class ProductListItem(
    val product: Product,
    val stockStatus: StockStatus,
)

data class ProductFormState(
    val productId: Long? = null,
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val category: String = "",
    val baseUnit: String = "pcs",
    val basePrice: String = "",
    val baseHpp: String = "",
    val stockQuantity: String = "0",
    val minStockAlert: String = "0",
    val isActive: Boolean = true,
    val isAdvancedExpanded: Boolean = false,
    val variantName: String = "",
    val conversionFactor: String = "",
    val variantPrice: String = "",
    val wholesaleMinQty: String = "",
    val wholesalePrice: String = "",
    val errors: Map<ProductFormField, String> = emptyMap(),
) {
    val isEditing: Boolean get() = productId != null

    fun errorFor(field: ProductFormField): String? = errors[field]
}

data class ProductsUiState(
    val searchQuery: String = "",
    val stockFilter: ProductStockFilter = ProductStockFilter.ALL,
    val products: UiState<List<ProductListItem>> = UiState.Loading,
    val form: ProductFormState? = null,
    val isSaving: Boolean = false,
    val pendingDeleteProductId: Long? = null,
    val message: String? = null,
)
