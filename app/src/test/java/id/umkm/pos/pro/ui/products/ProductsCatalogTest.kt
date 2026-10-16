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

    private fun productItem(
        id: Long,
        name: String = "Produk $id",
        category: String? = "Umum",
        stockBaseQty: Double = 100.0,
        minStockAlert: Double = 5.0,
        isActive: Boolean = true,
    ): ProductListItem {
        val product = uiSampleProduct(
            id = id,
            name = name,
            stockBaseQty = stockBaseQty,
            minStockAlert = minStockAlert,
            isActive = isActive,
        ).copy(category = category)
        return ProductListItem(product = product, stockStatus = resolveStockStatus(product))
    }
}
