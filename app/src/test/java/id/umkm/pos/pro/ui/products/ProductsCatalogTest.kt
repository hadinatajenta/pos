package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.uiSampleProduct
import org.junit.Test

class ProductsCatalogTest {

    @Test
    fun categoriesAreTrimmedDistinctAndSorted() {
        val catalog = buildProductsCatalog(
            productItems = listOf(
                productItem(id = 1L, category = "Minuman"),
                productItem(id = 2L, category = "Makanan"),
                productItem(id = 3L, category = "  minuman  "),
                productItem(id = 4L, category = null),
                productItem(id = 5L, category = "   "),
            ),
            categoryFilter = null,
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(catalog.categories).containsExactly("Makanan", "Minuman").inOrder()
    }

    @Test
    fun categoryFilterMatchesTheCategoryIgnoringCaseAndSpacing() {
        val productItems = listOf(
            productItem(id = 1L, name = "Kopi Susu", category = "Minuman"),
            productItem(id = 2L, name = "Roti Cokelat", category = "Makanan"),
            productItem(id = 3L, name = "Tanpa Kategori", category = null),
        )

        val filteredCatalog = buildProductsCatalog(
            productItems = productItems,
            categoryFilter = "  minuman ",
            sortOrder = ProductSortOrder.NEWEST,
        )
        val unfilteredCatalog = buildProductsCatalog(
            productItems = productItems,
            categoryFilter = " ",
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(filteredCatalog.items.map { productItem -> productItem.product.name }).containsExactly("Kopi Susu")
        assertThat(unfilteredCatalog.items).hasSize(3)
    }

    @Test
    fun categoryFilterWithoutMatchesKeepsTheCatalogAndItsStats() {
        val catalog = buildProductsCatalog(
            productItems = listOf(productItem(id = 1L, category = "Minuman")),
            categoryFilter = "Peralatan",
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(catalog.items).isEmpty()
        assertThat(catalog.categories).containsExactly("Minuman")
        assertThat(catalog.stats.totalCount).isEqualTo(1)
    }

    @Test
    fun newestSortPlacesTheMostRecentlyAddedProductFirst() {
        val catalog = buildProductsCatalog(
            productItems = listOf(
                productItem(id = 7L, name = "Kopi Susu"),
                productItem(id = 2L, name = "Air Mineral"),
                productItem(id = 11L, name = "Teh Manis"),
            ),
            categoryFilter = null,
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(catalog.items.map { productItem -> productItem.product.id }).containsExactly(11L, 7L, 2L).inOrder()
    }

    @Test
    fun nameSortIsAlphabeticalIgnoringCase() {
        val catalog = buildProductsCatalog(
            productItems = listOf(
                productItem(id = 1L, name = "teh manis"),
                productItem(id = 2L, name = "Air Mineral"),
                productItem(id = 3L, name = "Kopi Susu"),
            ),
            categoryFilter = null,
            sortOrder = ProductSortOrder.NAME_ASC,
        )

        assertThat(catalog.items.map { productItem -> productItem.product.name })
            .containsExactly("Air Mineral", "Kopi Susu", "teh manis")
            .inOrder()
    }

    @Test
    fun priceSortPlacesTheMostExpensiveProductFirst() {
        val catalog = buildProductsCatalog(
            productItems = listOf(
                productItem(id = 1L, name = "Air Mineral", basePrice = 5_000L),
                productItem(id = 2L, name = "Kopi Susu", basePrice = 25_000L),
                productItem(id = 3L, name = "Teh Manis", basePrice = 15_000L),
            ),
            categoryFilter = null,
            sortOrder = ProductSortOrder.PRICE_DESC,
        )

        assertThat(catalog.items.map { productItem -> productItem.product.id })
            .containsExactly(2L, 3L, 1L)
            .inOrder()
    }

    @Test
    fun lowestStockSortPlacesTheScarcestProductFirst() {
        val catalog = buildProductsCatalog(
            productItems = listOf(
                productItem(id = 1L, name = "Kopi Susu", stockBaseQty = 24.0),
                productItem(id = 2L, name = "Air Mineral", stockBaseQty = -2.0),
                productItem(id = 3L, name = "Gula Pasir", stockBaseQty = 0.0),
            ),
            categoryFilter = null,
            sortOrder = ProductSortOrder.LOWEST_STOCK,
        )

        assertThat(catalog.items.map { productItem -> productItem.product.id }).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun statsCountTotalsCategoriesAndStockAlerts() {
        val catalog = buildProductsCatalog(
            productItems = listOf(
                productItem(id = 1L, category = "Minuman", stockBaseQty = 100.0, minStockAlert = 5.0),
                productItem(id = 2L, category = "Minuman", stockBaseQty = 2.0, minStockAlert = 5.0),
                productItem(id = 3L, category = "Makanan", stockBaseQty = 0.0, minStockAlert = 5.0),
                productItem(id = 4L, category = null, stockBaseQty = -1.0, minStockAlert = 5.0),
                productItem(id = 5L, category = "Makanan", stockBaseQty = 40.0, minStockAlert = 5.0, isActive = false),
            ),
            categoryFilter = null,
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(catalog.stats.totalCount).isEqualTo(5)
        assertThat(catalog.stats.categoryCount).isEqualTo(2)
        assertThat(catalog.stats.lowStockCount).isEqualTo(1)
        assertThat(catalog.stats.outOfStockCount).isEqualTo(2)
        assertThat(catalog.stats.activeCount).isEqualTo(4)
        assertThat(catalog.stats.uncategorizedCount).isEqualTo(1)
    }

    @Test
    fun lowStockFilterIncludesOnlyLowStockProductsAndPreservesCatalogStats() {
        val productItems = listOf(
            productItem(id = 1L, name = "Kopi Susu", stockBaseQty = 20.0, minStockAlert = 5.0),
            productItem(id = 2L, name = "Teh Manis", stockBaseQty = 3.0, minStockAlert = 5.0),
            productItem(id = 3L, name = "Gula Pasir", stockBaseQty = 0.0, minStockAlert = 5.0),
        )

        val catalog = buildProductsCatalog(
            productItems = productItems,
            categoryFilter = null,
            sortOrder = ProductSortOrder.NEWEST,
            stockFilter = ProductStockFilter.LOW_STOCK,
        )

        assertThat(catalog.items.map { item -> item.product.name }).containsExactly("Teh Manis")
        assertThat(catalog.stats.totalCount).isEqualTo(3)
        assertThat(catalog.stats.lowStockCount).isEqualTo(1)
        assertThat(catalog.stats.outOfStockCount).isEqualTo(1)
    }

    @Test
    fun outOfStockFilterIncludesZeroAndNegativeStockProductsAndPreservesStats() {
        val productItems = listOf(
            productItem(id = 1L, name = "Kopi Susu", stockBaseQty = 20.0, minStockAlert = 5.0),
            productItem(id = 2L, name = "Teh Manis", stockBaseQty = 3.0, minStockAlert = 5.0),
            productItem(id = 3L, name = "Gula Pasir", stockBaseQty = 0.0, minStockAlert = 5.0),
            productItem(id = 4L, name = "Air Mineral", stockBaseQty = -2.0, minStockAlert = 5.0),
        )

        val catalog = buildProductsCatalog(
            productItems = productItems,
            categoryFilter = null,
            sortOrder = ProductSortOrder.NEWEST,
            stockFilter = ProductStockFilter.OUT_OF_STOCK,
        )

        assertThat(catalog.items.map { item -> item.product.name })
            .containsExactly("Air Mineral", "Gula Pasir")
            .inOrder()
        assertThat(catalog.stats.totalCount).isEqualTo(4)
        assertThat(catalog.stats.outOfStockCount).isEqualTo(2)
    }

    @Test
    fun summaryUsesEveryLoadedProductRegardlessOfTheFilter() {
        val productItems = listOf(
            productItem(
                id = 1L,
                name = "Kopi Susu",
                category = "Minuman",
                stockBaseQty = 10.0,
                minStockAlert = 5.0,
                basePrice = 15_000L,
            ),
            productItem(
                id = 2L,
                name = "Teh Manis",
                category = "Minuman",
                stockBaseQty = 3.0,
                minStockAlert = 5.0,
                basePrice = 2_000L,
            ),
            productItem(
                id = 3L,
                name = "Roti Cokelat",
                category = "Makanan",
                stockBaseQty = -2.0,
                minStockAlert = 5.0,
                basePrice = 20_000L,
            ),
            productItem(
                id = 4L,
                name = "Sabun Cuci",
                category = null,
                stockBaseQty = 0.0,
                minStockAlert = 5.0,
                basePrice = 5_000L,
            ),
        )

        val catalog = buildProductsCatalog(
            productItems = productItems,
            categoryFilter = "Minuman",
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(catalog.items).hasSize(2)
        assertThat(catalog.summary.totalProducts).isEqualTo(4)
        assertThat(catalog.summary.lowCount).isEqualTo(1)
        assertThat(catalog.summary.outCount).isEqualTo(2)
        assertThat(catalog.summary.stockValue).isEqualTo(156_000L)
        assertThat(catalog.summary.stockValueCompact).isEqualTo("Rp 156.000")
    }

    @Test
    fun cardsMirrorTheVisibleItemsAndCarryPresentationValues() {
        val catalog = buildProductsCatalog(
            productItems = listOf(
                productItem(
                    id = 1L,
                    name = "Kopi Susu",
                    category = "Minuman",
                    stockBaseQty = 12.0,
                    basePrice = 15_000L,
                    icon = "☕",
                ),
                productItem(
                    id = 2L,
                    name = "Teh Manis",
                    category = "Minuman",
                    stockBaseQty = 0.0,
                    basePrice = 5_000L,
                    imagePath = "product-images/teh.jpg",
                    icon = "🍵",
                ),
                productItem(id = 3L, name = "Roti Cokelat", category = "Makanan", basePrice = 8_000L),
                productItem(id = 4L, name = "Sabun Cuci", category = null, basePrice = 3_000L),
            ),
            categoryFilter = null,
            sortOrder = ProductSortOrder.NEWEST,
        )

        assertThat(catalog.cards.map { card -> card.id })
            .containsExactlyElementsIn(catalog.items.map { item -> item.product.id })
            .inOrder()

        val coffeeCard = catalog.cards.first { card -> card.id == 1L }
        assertThat(coffeeCard.name).isEqualTo("Kopi Susu")
        assertThat(coffeeCard.categoryName).isEqualTo("Minuman")
        assertThat(coffeeCard.categoryColorKey).isEqualTo("minuman")
        assertThat(coffeeCard.priceFormatted).isEqualTo("Rp 15.000")
        assertThat(coffeeCard.stock).isEqualTo(12.0)
        assertThat(coffeeCard.icon).isEqualTo("☕")
        assertThat(coffeeCard.iconOrImageUrl).isEqualTo("☕")
        assertThat(coffeeCard.imagePath).isNull()
        assertThat(coffeeCard.isActive).isTrue()

        val teaCard = catalog.cards.first { card -> card.id == 2L }
        assertThat(teaCard.imagePath).isEqualTo("product-images/teh.jpg")
        assertThat(teaCard.icon).isEqualTo("🍵")
        assertThat(teaCard.iconOrImageUrl).isEqualTo("product-images/teh.jpg")

        assertThat(catalog.cards.first { card -> card.id == 3L }.categoryColorKey).isEqualTo("makanan")
        assertThat(catalog.cards.first { card -> card.id == 4L }.categoryColorKey).isEqualTo("tanpa-kategori")
    }

    @Test
    fun cardsDoNotCarryAStockStatusField() {
        val fieldNames = ProductCardView::class.java.declaredFields.map { field -> field.name }
        val stockField = ProductCardView::class.java.getDeclaredField("stock")

        assertThat(fieldNames).containsAtLeast("id", "name", "stock", "isActive")
        assertThat(fieldNames).doesNotContain("stockStatus")
        assertThat(stockField.type).isEqualTo(Double::class.javaPrimitiveType)
    }

    @Test
    fun categoryColorKeysAreDeterministic() {
        assertThat(categoryColorKey("Makanan")).isEqualTo("makanan")
        assertThat(categoryColorKey("  minuman ")).isEqualTo("minuman")
        assertThat(categoryColorKey(null)).isEqualTo("tanpa-kategori")
        assertThat(categoryColorKey("   ")).isEqualTo("tanpa-kategori")

        val electronicsKey = categoryColorKey("Elektronik")

        assertThat(categoryColorKey("elektronik")).isEqualTo(electronicsKey)
        assertThat(categoryColorKey(" ELEKTRONIK ")).isEqualTo(electronicsKey)
        assertThat(electronicsKey).startsWith("palette-")
    }

    private fun productItem(
        id: Long,
        name: String = "Produk $id",
        category: String? = "Umum",
        stockBaseQty: Double = 100.0,
        minStockAlert: Double = 5.0,
        isActive: Boolean = true,
        basePrice: Long = 2_000L,
        imagePath: String? = null,
        icon: String? = null,
    ): ProductListItem {
        val product = uiSampleProduct(
            id = id,
            name = name,
            stockBaseQty = stockBaseQty,
            minStockAlert = minStockAlert,
            isActive = isActive,
            basePrice = basePrice,
            icon = icon,
        ).copy(category = category, imagePath = imagePath)
        return ProductListItem(product = product, stockStatus = resolveStockStatus(product))
    }
}
