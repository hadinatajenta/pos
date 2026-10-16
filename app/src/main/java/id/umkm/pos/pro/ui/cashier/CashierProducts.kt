package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.StockStatusBadge
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import androidx.annotation.DrawableRes

private const val MAX_PRODUCT_COLUMNS = 4
private val CATALOG_THUMBNAIL_SIZE = 52.dp
private val CATALOG_TILE_THUMBNAIL_SIZE = 48.dp
private val CATALOG_CONTROL_TOUCH_SIZE = 40.dp
private val CATALOG_CONTROL_SIZE = 32.dp
private val CATALOG_CONTROL_ICON_SIZE = 16.dp
private val CATALOG_QUANTITY_MIN_WIDTH = 26.dp
private val CATALOG_FILTER_BUTTON_SIZE = 56.dp
private const val CATALOG_UNSELECTED_BORDER_WIDTH = 0.5f
private const val CATALOG_SELECTED_BORDER_WIDTH = 1f
private const val CATALOG_CONTROL_BORDER_WIDTH = 1f

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = actions.onSearchQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = PosShapes.large,
            placeholder = {
                Text(
                    text = "Cari produk",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = LocalAppTextStyles.current.body,
                )
            },
            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { actions.onSearchQueryChange("") }) {
                        Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Hapus pencarian")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        CashierFilterButton(state = state, actions = actions)
    }
}

@Composable
private fun CashierFilterButton(
    state: CashierUiState,
    actions: CashierActions,
) {
    var isMenuVisible by remember { mutableStateOf(false) }
    val isFilterActive = state.catalogFilter.isActive
    Box {
        Surface(
            color = Color.White,
            contentColor = if (isFilterActive) PosColors.ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = PosShapes.large,
            border = BorderStroke(
                1.dp,
                if (isFilterActive) PosColors.ForestGreen else MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .size(CATALOG_FILTER_BUTTON_SIZE)
                .clickable(onClickLabel = "Filter produk", onClick = { isMenuVisible = true }),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_list_filter),
                    contentDescription = "Filter produk",
                    tint = if (isFilterActive) PosColors.ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PosSizes.iconMedium),
                )
            }
        }
        DropdownMenu(
            expanded = isMenuVisible,
            onDismissRequest = { isMenuVisible = false },
        ) {
            DropdownMenuItem(
                text = { Text("Semua kategori") },
                onClick = {
                    actions.onCatalogCategoryChange(null)
                    isMenuVisible = false
                },
                trailingIcon = {
                    if (state.catalogFilter.category == null) {
                        Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null)
                    }
                },
            )
            state.catalogCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        actions.onCatalogCategoryChange(category)
                        isMenuVisible = false
                    },
                    trailingIcon = {
                        if (state.catalogFilter.category == category) {
                            Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null)
                        }
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Hanya stok tersedia") },
                onClick = { actions.onCatalogAvailableOnlyChange(!state.catalogFilter.isAvailableOnly) },
                trailingIcon = {
                    if (state.catalogFilter.isAvailableOnly) {
                        Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null)
                    }
                },
            )
        }
    }
}

@Composable
private fun CashierEmptyCatalog(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier,
) {
    val isSearching = state.searchQuery.isNotBlank()
    val isFiltered = state.catalogFilter.isActive
    val isNarrowed = isSearching || isFiltered
    EmptyState(
        iconRes = R.drawable.ic_search_x,
        title = if (isNarrowed) "Produk tidak ditemukan" else "Belum ada produk",
        supportingText = when {
            isSearching && isFiltered -> "Coba ubah kata kunci atau filter."
            isSearching -> "Coba nama, SKU, atau barcode lain."
            isFiltered -> "Coba ubah atau hapus filter."
            else -> "Tambahkan produk dari tab Produk untuk mulai berjualan."
        },
        modifier = modifier,
        actionLabel = if (isNarrowed) "Reset pencarian dan filter" else null,
        onAction = if (isNarrowed) {
            {
                actions.onSearchQueryChange("")
                actions.onCatalogCategoryChange(null)
                actions.onCatalogAvailableOnlyChange(false)
            }
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
                    CashierProductRow(item = item, actions = actions)
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
                    CashierProductTile(item = item, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun CashierProductRow(
    item: CashierProductItem,
    actions: CashierActions,
) {
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Tambah ${item.product.name}",
                onClick = { actions.onAddProduct(item.product.id) },
            ),
        contentPadding = PosSpacing.sm,
        border = catalogItemBorder(isSelected = item.quantityInCart > 0.0),
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
                    style = LocalAppTextStyles.current.supportingLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Rupiah.format(item.product.basePrice),
                    style = LocalAppTextStyles.current.catalogItemPrice,
                )
                CashierStockBadge(status = item.stockStatus)
            }
            CatalogQuantityControl(item = item, actions = actions)
        }
    }
}

@Composable
private fun CashierProductTile(
    item: CashierProductItem,
    actions: CashierActions,
) {
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Tambah ${item.product.name}",
                onClick = { actions.onAddProduct(item.product.id) },
            ),
        contentPadding = PosSpacing.sm,
        border = catalogItemBorder(isSelected = item.quantityInCart > 0.0),
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = item.product.name,
                    style = LocalAppTextStyles.current.supportingLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Rupiah.format(item.product.basePrice),
                    style = LocalAppTextStyles.current.catalogItemPrice,
                )
                CashierStockBadge(status = item.stockStatus)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            CatalogQuantityControl(item = item, actions = actions)
        }
    }
}

@Composable
private fun CashierStockBadge(status: StockStatus) {
    if (status == StockStatus.IN_STOCK) return
    StockStatusBadge(status = status)
}

@Composable
private fun catalogItemBorder(isSelected: Boolean): BorderStroke = BorderStroke(
    width = if (isSelected) CATALOG_SELECTED_BORDER_WIDTH.dp else CATALOG_UNSELECTED_BORDER_WIDTH.dp,
    color = if (isSelected) PosColors.ForestGreen else MaterialTheme.colorScheme.outlineVariant,
)

@Composable
private fun CatalogQuantityControl(
    item: CashierProductItem,
    actions: CashierActions,
) {
    if (item.quantityInCart <= 0.0) {
        CatalogCircleButton(
            iconRes = R.drawable.ic_plus,
            contentDescription = "Tambah ${item.product.name}",
            onClick = { actions.onAddProduct(item.product.id) },
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        CatalogCircleButton(
            iconRes = R.drawable.ic_minus,
            contentDescription = "Kurangi ${item.product.name}",
            onClick = { actions.onDecreaseProductQuantity(item.product.id) },
        )
        Text(
            text = formatQuantity(item.quantityInCart),
            style = LocalAppTextStyles.current.catalogItemPrice,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = CATALOG_QUANTITY_MIN_WIDTH),
        )
        CatalogCircleButton(
            iconRes = R.drawable.ic_plus,
            contentDescription = "Tambah ${item.product.name}",
            onClick = { actions.onAddProduct(item.product.id) },
        )
    }
}

@Composable
private fun CatalogCircleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(CATALOG_CONTROL_TOUCH_SIZE)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(CATALOG_CONTROL_SIZE)
                .clip(CircleShape)
                .border(CATALOG_CONTROL_BORDER_WIDTH.dp, PosColors.ForestGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = PosColors.ForestGreen,
                modifier = Modifier.size(CATALOG_CONTROL_ICON_SIZE),
            )
        }
    }
}
