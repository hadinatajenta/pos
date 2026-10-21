package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.UiText

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

enum class ProductSortOrder {
    NEWEST,
    NAME_ASC,
    LOWEST_STOCK,
}

fun ProductSortOrder.label(strings: PosStrings): String = when (this) {
    ProductSortOrder.NEWEST -> strings.productsSortNewest
    ProductSortOrder.NAME_ASC -> strings.productsSortNameAsc
    ProductSortOrder.LOWEST_STOCK -> strings.productsSortLowestStock
}

enum class ProductStockFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK,
}

data class ProductListItem(
    val product: Product,
    val stockStatus: StockStatus,
)

data class ProductStats(
    val totalCount: Int,
    val categoryCount: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val activeCount: Int,
    val uncategorizedCount: Int,
)

data class ProductsCatalog(
    val items: List<ProductListItem>,
    val categories: List<String>,
    val stats: ProductStats,
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
    val minStockAlert: String = "5",
    val isActive: Boolean = true,
    val isAdvancedExpanded: Boolean = false,
    val variantName: String = "",
    val conversionFactor: String = "",
    val variantPrice: String = "",
    val wholesaleMinQty: String = "",
    val wholesalePrice: String = "",
    val imagePath: String? = null,
    val persistedPricing: ProductPricingSnapshot? = null,
    val persistedImagePath: String? = null,
    val isImageRemoved: Boolean = false,
    val isImageImporting: Boolean = false,
    val errors: Map<ProductFormField, UiText> = emptyMap(),
    val validationFailures: Long = 0L,
) {
    val isEditing: Boolean get() = productId != null

    val unsavedImagePath: String? get() = imagePath?.takeIf { path -> path != persistedImagePath }

    val storedImagePathToDiscard: String? get() = persistedImagePath?.takeIf { path -> path != imagePath }

    fun errorFor(field: ProductFormField): UiText? = errors[field]
}

data class ProductsUiState(
    val searchQuery: String = "",
    val categoryFilter: String? = null,
    val stockFilter: ProductStockFilter = ProductStockFilter.ALL,
    val sortOrder: ProductSortOrder = ProductSortOrder.NEWEST,
    val catalog: UiState<ProductsCatalog> = UiState.Loading,
    val form: ProductFormState? = null,
    val isSaving: Boolean = false,
    val pendingDeleteProductId: Long? = null,
    val message: UiText? = null,
)

fun buildProductsCatalog(
    productItems: List<ProductListItem>,
    categoryFilter: String?,
    sortOrder: ProductSortOrder,
    stockFilter: ProductStockFilter = ProductStockFilter.ALL,
): ProductsCatalog {
    val categories = productItems.distinctCategories()
    return ProductsCatalog(
        items = productItems
            .filter { productItem ->
                productItem.matchesCategory(categoryFilter) && productItem.matchesStockFilter(stockFilter)
            }
            .sortedWith(sortOrder.comparator()),
        categories = categories,
        stats = productItems.computeProductStats(categoryCount = categories.size),
    )
}

private fun ProductSortOrder.comparator(): Comparator<ProductListItem> = when (this) {
    ProductSortOrder.NEWEST ->
        Comparator { left, right -> right.product.id.compareTo(left.product.id) }

    ProductSortOrder.NAME_ASC ->
        Comparator { left, right -> left.product.name.compareTo(right.product.name, ignoreCase = true) }

    ProductSortOrder.LOWEST_STOCK ->
        Comparator { left, right -> left.product.stockBaseQty.compareTo(right.product.stockBaseQty) }
}

private fun ProductListItem.matchesCategory(categoryFilter: String?): Boolean {
    if (categoryFilter.isNullOrBlank()) return true
    val category = product.category?.trim().orEmpty()
    return category.equals(categoryFilter.trim(), ignoreCase = true)
}

private fun ProductListItem.matchesStockFilter(stockFilter: ProductStockFilter): Boolean = when (stockFilter) {
    ProductStockFilter.ALL -> true
    ProductStockFilter.LOW_STOCK -> stockStatus == StockStatus.LOW_STOCK
    ProductStockFilter.OUT_OF_STOCK ->
        stockStatus == StockStatus.OUT_OF_STOCK || stockStatus == StockStatus.OVERSOLD
}

private fun List<ProductListItem>.distinctCategories(): List<String> {
    val categories = mapNotNull { productItem ->
        productItem.product.category?.trim()?.takeIf { category -> category.isNotEmpty() }
    }
    return categories
        .distinctBy { category -> category.lowercase() }
        .sortedBy { category -> category.lowercase() }
}

private fun List<ProductListItem>.computeProductStats(categoryCount: Int): ProductStats = ProductStats(
    totalCount = size,
    categoryCount = categoryCount,
    lowStockCount = count { productItem -> productItem.stockStatus == StockStatus.LOW_STOCK },
    outOfStockCount = count { productItem ->
        productItem.stockStatus == StockStatus.OUT_OF_STOCK || productItem.stockStatus == StockStatus.OVERSOLD
    },
    activeCount = count { productItem -> productItem.product.isActive },
    uncategorizedCount = count { productItem -> productItem.product.category.isNullOrBlank() },
)
