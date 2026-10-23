package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.UiText

const val PRODUCT_NAME_MAX_LENGTH = 40
const val DEFAULT_LOW_STOCK_THRESHOLD = "6"

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
    PRICE_DESC,
    LOWEST_STOCK,
}

fun ProductSortOrder.label(strings: PosStrings): String = when (this) {
    ProductSortOrder.NEWEST -> strings.productsSortNewest
    ProductSortOrder.NAME_ASC -> strings.productsSortNameAsc
    ProductSortOrder.PRICE_DESC -> strings.productsSortPriceDesc
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

data class ProductCardView(
    val id: Long,
    val name: String,
    val categoryName: String,
    val categoryColorKey: String,
    val priceFormatted: String,
    val stock: Double,
    val iconOrImageUrl: String?,
    val imagePath: String? = null,
    val icon: String? = null,
    val isActive: Boolean = true,
    val duplicateDiscriminator: String? = null,
)

data class ProductListSummary(
    val totalProducts: Int,
    val lowCount: Int,
    val outCount: Int,
    val stockValue: Long,
    val stockValueCompact: String,
)

data class ProductCategorySummary(
    val name: String,
    val count: Int,
    val colorKey: String,
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
    val cards: List<ProductCardView>,
    val categories: List<String>,
    val categorySummaries: List<ProductCategorySummary>,
    val stats: ProductStats,
    val summary: ProductListSummary,
)

data class ProductFormState(
    val formInstanceId: Long = 0L,
    val productId: Long? = null,
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val category: String = "",
    val baseUnit: String = "pcs",
    val basePrice: String = "",
    val baseHpp: String = "",
    val stockQuantity: String = "0",
    val minStockAlert: String = DEFAULT_LOW_STOCK_THRESHOLD,
    val isActive: Boolean = true,
    val isAdvancedExpanded: Boolean = false,
    val variantName: String = "",
    val conversionFactor: String = "",
    val variantPrice: String = "",
    val wholesaleMinQty: String = "",
    val wholesalePrice: String = "",
    val imagePath: String? = null,
    val icon: String? = null,
    val persistedPricing: ProductPricingSnapshot? = null,
    val persistedImagePath: String? = null,
    val isImageRemoved: Boolean = false,
    val isImageImporting: Boolean = false,
    val isDirty: Boolean = false,
    val duplicateNameWarning: Boolean = false,
    val errors: Map<ProductFormField, UiText> = emptyMap(),
    val validationFailures: Long = 0L,
) {
    val isEditing: Boolean get() = productId != null

    val unsavedImagePath: String? get() = imagePath?.takeIf { path -> path != persistedImagePath }

    val storedImagePathToDiscard: String? get() = persistedImagePath?.takeIf { path -> path != imagePath }

    val canSubmit: Boolean
        get() {
            val normalizedName = name.trim()
            val parsedPrice = basePrice.trim().toLongOrNull()
            return normalizedName.isNotEmpty() &&
                normalizedName.length <= PRODUCT_NAME_MAX_LENGTH &&
                parsedPrice != null &&
                parsedPrice > 0L
        }

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
    val pendingDeleteProductName: String? = null,
    val isDiscardConfirmationVisible: Boolean = false,
    val message: UiText? = null,
)

fun buildProductsCatalog(
    productItems: List<ProductListItem>,
    categoryFilter: String?,
    sortOrder: ProductSortOrder,
    stockFilter: ProductStockFilter = ProductStockFilter.ALL,
    searchQuery: String = "",
): ProductsCatalog {
    val categories = productItems.distinctCategories()
    val duplicateKeys = productItems
        .groupingBy { item -> item.product.duplicateKey() }
        .eachCount()
        .filterValues { count -> count > 1 }
        .keys
    val visibleItems = productItems
        .filter { productItem ->
            productItem.matchesSearch(searchQuery) &&
                productItem.matchesCategory(categoryFilter) &&
                productItem.matchesStockFilter(stockFilter)
        }
        .sortedWith(sortOrder.comparator())
    return ProductsCatalog(
        items = visibleItems,
        cards = visibleItems.map { item ->
            item.product.toCardView(isAmbiguous = item.product.duplicateKey() in duplicateKeys)
        },
        categories = categories,
        categorySummaries = productItems.categorySummaries(categories),
        stats = productItems.computeProductStats(categoryCount = categories.size),
        summary = productItems.computeSummary(),
    )
}

fun buildProductCardView(product: Product): ProductCardView = product.toCardView(isAmbiguous = false)

private fun Product.toCardView(isAmbiguous: Boolean): ProductCardView {
    val normalizedCategory = category?.trim()?.takeIf { value -> value.isNotEmpty() }
    return ProductCardView(
        id = id,
        name = name,
        categoryName = normalizedCategory.orEmpty(),
        categoryColorKey = categoryColorKey(normalizedCategory),
        priceFormatted = Rupiah.format(basePrice),
        stock = stockBaseQty,
        iconOrImageUrl = imagePath ?: icon,
        imagePath = imagePath,
        icon = icon,
        isActive = isActive,
        duplicateDiscriminator = if (isAmbiguous) sku.takeIf { value -> value.isNotBlank() } else null,
    )
}

private fun Product.duplicateKey(): String =
    "${name.trim().lowercase()}|${category?.trim()?.lowercase().orEmpty()}"

private fun ProductSortOrder.comparator(): Comparator<ProductListItem> = when (this) {
    ProductSortOrder.NEWEST ->
        Comparator { left, right -> right.product.id.compareTo(left.product.id) }

    ProductSortOrder.NAME_ASC ->
        Comparator { left, right -> left.product.name.compareTo(right.product.name, ignoreCase = true) }

    ProductSortOrder.PRICE_DESC ->
        Comparator { left, right -> right.product.basePrice.compareTo(left.product.basePrice) }

    ProductSortOrder.LOWEST_STOCK ->
        Comparator { left, right -> left.product.stockBaseQty.compareTo(right.product.stockBaseQty) }
}

private fun ProductListItem.matchesSearch(searchQuery: String): Boolean {
    val normalizedQuery = searchQuery.trim()
    if (normalizedQuery.isEmpty()) return true
    val product = product
    return product.name.contains(normalizedQuery, ignoreCase = true) ||
        product.sku.contains(normalizedQuery, ignoreCase = true) ||
        product.barcode.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
        product.category.orEmpty().contains(normalizedQuery, ignoreCase = true)
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

private fun List<ProductListItem>.categorySummaries(categories: List<String>): List<ProductCategorySummary> =
    categories.map { category ->
        ProductCategorySummary(
            name = category,
            count = count { item -> item.product.category?.trim().equals(category, ignoreCase = true) },
            colorKey = categoryColorKey(category),
        )
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

private fun List<ProductListItem>.computeSummary(): ProductListSummary {
    val stockValue = stockValueOf(map { item -> item.product })
    return ProductListSummary(
        totalProducts = size,
        lowCount = count { item -> item.stockStatus == StockStatus.LOW_STOCK },
        outCount = count { item ->
            item.stockStatus == StockStatus.OUT_OF_STOCK || item.stockStatus == StockStatus.OVERSOLD
        },
        stockValue = stockValue,
        stockValueCompact = formatCompactRupiah(stockValue),
    )
}
