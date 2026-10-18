package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val CATALOG_FILTER_EMPTY_KEY = "products-catalog-filter-empty"

@Composable
internal fun ProductsCatalogList(
    state: ProductsUiState,
    catalog: ProductsCatalog,
    actions: ProductsActions,
    isCompact: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = if (isCompact) PosSpacing.md else PosSpacing.lg
    val contentPadding = PaddingValues(
        start = horizontalPadding,
        end = horizontalPadding,
        top = PosSpacing.xs,
        bottom = PosSizes.listBottomPadding,
    )
    Column(modifier = modifier.fillMaxSize()) {
        ProductsQuickFilterBar(
            state = state,
            catalog = catalog,
            actions = actions,
            horizontalPadding = horizontalPadding,
            modifier = Modifier.padding(top = PosSpacing.xs),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            if (catalog.items.isEmpty()) {
                item(key = CATALOG_FILTER_EMPTY_KEY) {
                    ProductsFilterEmptyState(actions = actions)
                }
            } else if (isCompact) {
                items(
                    items = catalog.items,
                    key = { productItem -> productItem.product.id },
                ) { productItem ->
                    ProductRow(
                        productItem = productItem,
                        actions = actions,
                    )
                }
            } else {
                val pairedRows = catalog.items.chunked(2)
                items(
                    items = pairedRows,
                    key = { rowItems -> "row-${rowItems.first().product.id}" },
                ) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                    ) {
                        rowItems.forEach { productItem ->
                            ProductRow(
                                productItem = productItem,
                                actions = actions,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProductsQuickFilterBar(
    state: ProductsUiState,
    catalog: ProductsCatalog,
    actions: ProductsActions,
    horizontalPadding: Dp = PosSpacing.md,
    modifier: Modifier = Modifier,
) {
    val chipColors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
    )
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        item(key = "filter-all") {
            FilterChip(
                selected = state.stockFilter == ProductStockFilter.ALL && state.categoryFilter == null,
                onClick = {
                    actions.onStockFilterChange(ProductStockFilter.ALL)
                    actions.onCategoryFilterChange(null)
                },
                label = { Text("Semua (${catalog.stats.totalCount})") },
                shape = androidx.compose.foundation.shape.CircleShape,
                border = null,
                colors = chipColors,
            )
        }
        item(key = "filter-low-stock") {
            FilterChip(
                selected = state.stockFilter == ProductStockFilter.LOW_STOCK,
                onClick = {
                    val next = if (state.stockFilter == ProductStockFilter.LOW_STOCK) {
                        ProductStockFilter.ALL
                    } else {
                        ProductStockFilter.LOW_STOCK
                    }
                    actions.onStockFilterChange(next)
                },
                label = { Text("Stok Menipis (${catalog.stats.lowStockCount})") },
                shape = androidx.compose.foundation.shape.CircleShape,
                border = null,
                colors = chipColors,
            )
        }
        item(key = "filter-out-of-stock") {
            FilterChip(
                selected = state.stockFilter == ProductStockFilter.OUT_OF_STOCK,
                onClick = {
                    val next = if (state.stockFilter == ProductStockFilter.OUT_OF_STOCK) {
                        ProductStockFilter.ALL
                    } else {
                        ProductStockFilter.OUT_OF_STOCK
                    }
                    actions.onStockFilterChange(next)
                },
                label = { Text("Stok Habis (${catalog.stats.outOfStockCount})") },
                shape = androidx.compose.foundation.shape.CircleShape,
                border = null,
                colors = chipColors,
            )
        }
        items(
            items = catalog.categories,
            key = { category -> "cat-${category.lowercase()}" },
        ) { category ->
            val isSelected = state.categoryFilter.equals(category, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val next = if (isSelected) null else category
                    actions.onCategoryFilterChange(next)
                },
                label = { Text(category) },
                shape = androidx.compose.foundation.shape.CircleShape,
                border = null,
                colors = chipColors,
            )
        }
    }
}
