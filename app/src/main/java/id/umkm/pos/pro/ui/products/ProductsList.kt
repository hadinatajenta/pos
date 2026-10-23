package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.PosSizes

private const val PRODUCT_GRID_COMPACT_COLUMNS = 1
private const val PRODUCT_GRID_EXPANDED_COLUMNS = 2
private const val PRODUCT_ENTRANCE_ANIMATION_LIMIT = 12
private val PRODUCT_GRID_GAP = 12.dp
private val PRODUCT_GRID_HORIZONTAL_PADDING = 16.dp
private val PRODUCT_GRID_TOP_PADDING = 8.dp
private val PRODUCT_GRID_BOTTOM_PADDING = PosSizes.listBottomPadding + 24.dp
private val PRODUCT_GRID_BOTTOM_PADDING_COMPACT = 96.dp
private val EMPTY_STATE_CONTENT_PADDING = 24.dp

@Composable
internal fun ProductsCatalogSection(
    state: ProductsUiState,
    actions: ProductsActions,
    isCompact: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val bottomPadding = if (isCompact) {
        PRODUCT_GRID_BOTTOM_PADDING_COMPACT
    } else {
        PRODUCT_GRID_BOTTOM_PADDING
    }
    when (val catalogState = state.catalog) {
        UiState.Loading -> CatalogScrollColumn(modifier = modifier, bottomPadding = bottomPadding) {
            LoadingState(label = strings.productsLoading)
        }

        UiState.Empty -> CatalogScrollColumn(modifier = modifier, bottomPadding = bottomPadding) {
            ProductsEmptyCatalog(state = state, actions = actions)
        }

        is UiState.Error -> CatalogScrollColumn(modifier = modifier, bottomPadding = bottomPadding) {
            ErrorState(error = catalogState.error, onRetry = actions.onRetry)
        }

        is UiState.Success -> {
            if (catalogState.data.stats.totalCount == 0) {
                CatalogScrollColumn(modifier = modifier, bottomPadding = bottomPadding) {
                    ProductsZeroState(onAddFirst = actions.onOpenCreateForm)
                }
            } else {
                ProductsCatalogContent(
                    state = state,
                    catalog = catalogState.data,
                    actions = actions,
                    isCompact = isCompact,
                    bottomPadding = bottomPadding,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun CatalogScrollColumn(
    modifier: Modifier,
    bottomPadding: Dp = PRODUCT_GRID_BOTTOM_PADDING,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        content = {
            content()
            Spacer(modifier = Modifier.height(bottomPadding))
        },
    )
}

@Composable
private fun ProductsCatalogContent(
    state: ProductsUiState,
    catalog: ProductsCatalog,
    actions: ProductsActions,
    isCompact: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    var hasInitialScroll by remember { mutableStateOf(false) }
    LaunchedEffect(state.searchQuery, state.categoryFilter, state.stockFilter, state.sortOrder) {
        if (!hasInitialScroll) {
            hasInitialScroll = true
            return@LaunchedEffect
        }
        gridState.scrollToItem(0)
    }
    Column(modifier = modifier.fillMaxSize()) {
        ProductsQuickFilterBar(
            state = state,
            catalog = catalog,
            actions = actions,
        )
        if (catalog.items.isEmpty()) {
            CatalogScrollColumn(modifier = Modifier.weight(1f), bottomPadding = bottomPadding) {
                ProductsFilterEmptyState(
                    actions = actions,
                    isSearching = state.searchQuery.isNotBlank(),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(
                    if (isCompact) PRODUCT_GRID_COMPACT_COLUMNS else PRODUCT_GRID_EXPANDED_COLUMNS,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = gridState,
                contentPadding = PaddingValues(
                    start = PRODUCT_GRID_HORIZONTAL_PADDING,
                    end = PRODUCT_GRID_HORIZONTAL_PADDING,
                    top = PRODUCT_GRID_TOP_PADDING,
                    bottom = bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(PRODUCT_GRID_GAP),
                horizontalArrangement = Arrangement.spacedBy(PRODUCT_GRID_GAP),
            ) {
                itemsIndexed(
                    items = catalog.cards,
                    key = { _, card -> card.id },
                ) { index, card ->
                    ProductCard(
                        card = card,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { actions.onOpenEditForm(card.id) },
                        onDuplicate = { actions.onDuplicateProduct(card.id) },
                        onToggleArchive = { actions.onToggleProductArchive(card.id) },
                        onDelete = { actions.onRequestDelete(card.id) },
                        entranceIndex = index,
                        animateEntrance = index < PRODUCT_ENTRANCE_ANIMATION_LIMIT,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductsEmptyCatalog(
    state: ProductsUiState,
    actions: ProductsActions,
) {
    val strings = LocalPosStrings.current
    if (state.searchQuery.isNotBlank()) {
        EmptyState(
            iconRes = R.drawable.ic_search_x,
            title = strings.productsNotFoundTitle,
            supportingText = strings.productsNotFoundHint,
            actionLabel = strings.productsClearSearch,
            onAction = { actions.onSearchQueryChange("") },
            modifier = Modifier.padding(top = EMPTY_STATE_CONTENT_PADDING),
        )
    } else {
        ProductsZeroState(onAddFirst = actions.onOpenCreateForm)
    }
}
