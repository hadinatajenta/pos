package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.StockStatusChip
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val MAX_PRODUCT_COLUMNS = 4

@Composable
internal fun CashierCatalogPane(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = actions.onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            singleLine = true,
            label = { Text("Search products") },
            placeholder = { Text("Name, SKU, or barcode") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { actions.onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        OfflineNotice()
        UiStateContent(
            state = state.catalog,
            onLoading = { LoadingState(modifier = Modifier.weight(1f), label = "Loading products") },
            onEmpty = {
                CashierEmptyCatalog(state = state, actions = actions, modifier = Modifier.weight(1f))
            },
            onError = { error ->
                ErrorState(error = error, onRetry = actions.onRetryCatalog, modifier = Modifier.weight(1f))
            },
            onSuccess = { items ->
                CashierProductList(items = items, actions = actions, modifier = Modifier.weight(1f))
            },
        )
    }
}

@Composable
private fun CashierEmptyCatalog(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier,
) {
    val isSearching = state.searchQuery.isNotBlank()
    EmptyState(
        icon = Icons.Filled.SearchOff,
        title = if (isSearching) "No products match" else "No products yet",
        supportingText = if (isSearching) {
            "Try another name, SKU, or barcode."
        } else {
            "Add products from the Products tab to start selling."
        },
        modifier = modifier,
        actionLabel = if (isSearching) "Clear search" else null,
        onAction = if (isSearching) {
            { actions.onSearchQueryChange("") }
        } else {
            null
        },
    )
}

@Composable
private fun CashierProductList(
    items: List<CashierProductItem>,
    actions: CashierActions,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = (maxWidth / PosSizes.productTileMinWidth).toInt().coerceIn(1, MAX_PRODUCT_COLUMNS)
        val contentPadding = PaddingValues(horizontal = PosSpacing.md, vertical = PosSpacing.sm)
        if (columnCount <= 1) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                lazyColumnItems(items = items, key = { item -> item.product.id }) { item ->
                    CashierProductRow(item = item, onAdd = { actions.onAddProduct(item.product.id) })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                lazyGridItems(items = items, key = { item -> item.product.id }) { item ->
                    CashierProductTile(item = item, onAdd = { actions.onAddProduct(item.product.id) })
                }
            }
        }
    }
}

@Composable
private fun CashierProductRow(
    item: CashierProductItem,
    onAdd: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(PosSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = item.product.name,
                    style = LocalAppTextStyles.current.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Rupiah.format(item.product.basePrice),
                    style = LocalAppTextStyles.current.financialValue,
                )
                StockStatusChip(status = item.stockStatus)
            }
            Icon(
                imageVector = Icons.Filled.AddCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PosSizes.iconMedium),
            )
        }
    }
}

@Composable
private fun CashierProductTile(
    item: CashierProductItem,
    onAdd: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Text(
                text = item.product.name,
                style = LocalAppTextStyles.current.body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = Rupiah.format(item.product.basePrice),
                style = LocalAppTextStyles.current.financialValue,
            )
            StockStatusChip(status = item.stockStatus)
        }
    }
}
