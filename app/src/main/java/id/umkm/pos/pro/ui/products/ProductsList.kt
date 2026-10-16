package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.items as lazyGridItems

private val GRID_THREE_COLUMN_MIN_WIDTH = 1200.dp

private const val CATALOG_CHROME_KEY = "products-catalog-chrome"
private const val CATALOG_FILTER_EMPTY_KEY = "products-catalog-filter-empty"

@Composable
internal fun ProductsCatalogList(
    catalog: ProductsCatalog,
    categoryFilter: String?,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columnCount = when {
            maxWidth >= GRID_THREE_COLUMN_MIN_WIDTH -> 3
            maxWidth >= PosBreakpoints.twoPane -> 2
            else -> 1
        }
        val contentPadding = PaddingValues(
            start = PosSpacing.md,
            end = PosSpacing.md,
            top = PosSpacing.sm,
            bottom = PosSizes.listBottomPadding,
        )
        if (columnCount == 1) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                productsCatalogItems(catalog = catalog, categoryFilter = categoryFilter, actions = actions)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                productsCatalogItems(catalog = catalog, categoryFilter = categoryFilter, actions = actions)
            }
        }
    }
}

private fun LazyListScope.productsCatalogItems(
    catalog: ProductsCatalog,
    categoryFilter: String?,
    actions: ProductsActions,
) {
    item(key = CATALOG_CHROME_KEY) {
        ProductsCatalogChrome(catalog = catalog, categoryFilter = categoryFilter, actions = actions)
    }
    if (catalog.items.isEmpty()) {
        item(key = CATALOG_FILTER_EMPTY_KEY) { ProductsFilterEmptyState(actions = actions) }
        return
    }
    lazyColumnItems(items = catalog.items, key = { productItem -> productItem.product.id }) { productItem ->
        ProductRow(productItem = productItem, actions = actions)
    }
}

private fun LazyGridScope.productsCatalogItems(
    catalog: ProductsCatalog,
    categoryFilter: String?,
    actions: ProductsActions,
) {
    item(
        key = CATALOG_CHROME_KEY,
        span = { GridItemSpan(maxLineSpan) },
    ) {
        ProductsCatalogChrome(catalog = catalog, categoryFilter = categoryFilter, actions = actions)
    }
    if (catalog.items.isEmpty()) {
        item(
            key = CATALOG_FILTER_EMPTY_KEY,
            span = { GridItemSpan(maxLineSpan) },
        ) {
            ProductsFilterEmptyState(actions = actions)
        }
        return
    }
    lazyGridItems(items = catalog.items, key = { productItem -> productItem.product.id }) { productItem ->
        ProductRow(productItem = productItem, actions = actions)
    }
}

@Composable
private fun ProductsCatalogChrome(
    catalog: ProductsCatalog,
    categoryFilter: String?,
    actions: ProductsActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        ProductsStatsRow(stats = catalog.stats)
        ProductsCategoryChips(
            categories = catalog.categories,
            selectedCategoryFilter = categoryFilter,
            onCategorySelect = actions.onCategoryFilterChange,
        )
    }
}
