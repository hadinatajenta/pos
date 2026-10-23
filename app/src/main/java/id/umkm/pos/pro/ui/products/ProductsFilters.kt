package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.PosFontFamily

private val FILTER_CHIP_SHAPE = CircleShape
private val FILTER_ROW_HORIZONTAL_PADDING = 20.dp
private val FILTER_ROW_VERTICAL_PADDING = 6.dp
private val FILTER_CHIP_GAP = 8.dp
@Composable
internal fun ProductsQuickFilterBar(
    state: ProductsUiState,
    catalog: ProductsCatalog,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(ProductPalette.Background),
        contentPadding = PaddingValues(
            start = FILTER_ROW_HORIZONTAL_PADDING,
            end = FILTER_ROW_HORIZONTAL_PADDING,
            top = FILTER_ROW_VERTICAL_PADDING,
            bottom = FILTER_ROW_VERTICAL_PADDING,
        ),
        horizontalArrangement = Arrangement.spacedBy(FILTER_CHIP_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "filter-all") {
            ProductsFilterChip(
                label = strings.productsFilterAll(catalog.summary.totalProducts),
                isSelected = state.stockFilter == ProductStockFilter.ALL && state.categoryFilter == null,
                onClick = {
                    actions.onStockFilterChange(ProductStockFilter.ALL)
                    actions.onCategoryFilterChange(null)
                },
            )
        }
        item(key = "filter-low-stock") {
            ProductsFilterChip(
                label = strings.productsFilterLowStock(catalog.summary.lowCount),
                isSelected = state.stockFilter == ProductStockFilter.LOW_STOCK,
                onClick = {
                    val nextFilter = if (state.stockFilter == ProductStockFilter.LOW_STOCK) {
                        ProductStockFilter.ALL
                    } else {
                        ProductStockFilter.LOW_STOCK
                    }
                    actions.onStockFilterChange(nextFilter)
                },
            )
        }
        item(key = "filter-out-of-stock") {
            ProductsFilterChip(
                label = strings.productsFilterOutOfStock(catalog.summary.outCount),
                isSelected = state.stockFilter == ProductStockFilter.OUT_OF_STOCK,
                onClick = {
                    val nextFilter = if (state.stockFilter == ProductStockFilter.OUT_OF_STOCK) {
                        ProductStockFilter.ALL
                    } else {
                        ProductStockFilter.OUT_OF_STOCK
                    }
                    actions.onStockFilterChange(nextFilter)
                },
            )
        }
        items(
            items = catalog.categorySummaries,
            key = { summary -> "category-${summary.name.lowercase()}" },
        ) { summary ->
            val isSelected = state.categoryFilter.equals(summary.name, ignoreCase = true)
            ProductsFilterChip(
                label = "${summary.name} (${summary.count})",
                isSelected = isSelected,
                onClick = {
                    val nextCategory = if (isSelected) null else summary.name
                    actions.onCategoryFilterChange(nextCategory)
                },
            )
        }
    }
}

@Composable
private fun ProductsFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.widthIn(max = 240.dp),
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontFamily = PosFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
            )
        },
        shape = FILTER_CHIP_SHAPE,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else ProductPalette.Line,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = ProductPalette.Muted,
            selectedContainerColor = ProductPalette.Brand,
            selectedLabelColor = Color.White,
        ),
    )
}

@Composable
internal fun ProductsFilterEmptyState(
    actions: ProductsActions,
    isSearching: Boolean,
) {
    val strings = LocalPosStrings.current
    EmptyState(
        iconRes = R.drawable.ic_search_x,
        title = if (isSearching) strings.productsNotFoundTitle else strings.productsFilterEmptyTitle,
        supportingText = if (isSearching) strings.productsNotFoundHint else strings.productsFilterEmptyHint,
        actionLabel = strings.productsFilterShowAll,
        onAction = {
            actions.onSearchQueryChange("")
            actions.onStockFilterChange(ProductStockFilter.ALL)
            actions.onCategoryFilterChange(null)
        },
    )
}
