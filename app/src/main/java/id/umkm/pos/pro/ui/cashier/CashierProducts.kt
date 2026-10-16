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
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.StockStatusBadge
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val MAX_PRODUCT_COLUMNS = 4
private val CATALOG_THUMBNAIL_SIZE = 44.dp
private val CATALOG_TILE_THUMBNAIL_SIZE = 40.dp

@Composable
internal fun CashierCatalogPane(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Kasir",
            supportingText = "Cari produk dan proses pembayaran",
        )
        CashierSearchField(state = state, actions = actions)
        OfflineNotice()
        UiStateContent(
            state = state.catalog,
            onLoading = { LoadingState(modifier = Modifier.weight(1f), label = "Memuat produk") },
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
private fun CashierSearchField(
    state: CashierUiState,
    actions: CashierActions,
) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = actions.onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        singleLine = true,
        label = { Text("Cari produk") },
        placeholder = { Text("Nama, SKU, atau barcode") },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (state.searchQuery.isNotEmpty()) {
                IconButton(onClick = { actions.onSearchQueryChange("") }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Hapus pencarian")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
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
        title = if (isSearching) "Produk tidak ditemukan" else "Belum ada produk",
        supportingText = if (isSearching) {
            "Coba nama, SKU, atau barcode lain."
        } else {
            "Tambahkan produk dari tab Produk untuk mulai berjualan."
        },
        modifier = modifier,
        actionLabel = if (isSearching) "Hapus pencarian" else null,
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
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Tambah ${item.product.name}", onClick = onAdd),
        contentPadding = PosSpacing.sm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            ProductThumbnail(
                category = item.product.category,
                size = CATALOG_THUMBNAIL_SIZE,
                imagePath = item.product.imagePath,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = item.product.name,
                    style = LocalAppTextStyles.current.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                ) {
                    Text(
                        text = Rupiah.format(item.product.basePrice),
                        style = LocalAppTextStyles.current.financialValue,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StockStatusBadge(status = item.stockStatus)
                }
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
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Tambah ${item.product.name}", onClick = onAdd),
        contentPadding = PosSpacing.sm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            ProductThumbnail(
                category = item.product.category,
                size = CATALOG_TILE_THUMBNAIL_SIZE,
                imagePath = item.product.imagePath,
            )
            Text(
                text = item.product.name,
                style = LocalAppTextStyles.current.body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = Rupiah.format(item.product.basePrice),
            style = LocalAppTextStyles.current.financialValue,
        )
        StockStatusBadge(status = item.stockStatus)
    }
}
