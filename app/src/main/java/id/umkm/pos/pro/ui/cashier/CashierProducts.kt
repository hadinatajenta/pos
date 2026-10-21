package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.HeaderActionButton
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSearchField
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.StockStatusBadge
import id.umkm.pos.pro.ui.scanner.BarcodeScanDialog
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.items as lazyGridItems

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
    val strings = LocalPosStrings.current
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.cashierTitle,
            supportingText = catalogSupportingText(state, strings),
            actions = {
                HeaderActionButton(
                    text = if (state.activeShift == null) {
                        strings.cashierShiftOpenAction
                    } else {
                        strings.cashierShiftSwitchAction
                    },
                    iconRes = R.drawable.ic_users,
                    onClick = { actions.onOpenShift(state.lineCount, actions.onClearCart) },
                )
            },
        )
        CashierSearchField(state = state, actions = actions)
        CashierCategoryChips(state = state, actions = actions)
        OfflineNotice()
        UiStateContent(
            state = state.catalog,
            onLoading = {
                LoadingState(modifier = Modifier.weight(1f), label = strings.cashierLoadingProducts)
            },
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
    if (state.isScanDialogVisible) {
        BarcodeScanDialog(
            submitLabel = strings.scannerManualAdd,
            onSubmit = actions.onScannedCode,
            onDismiss = actions.onDismissScanDialog,
        )
    }
}

private fun catalogSupportingText(state: CashierUiState, strings: PosStrings): String? {
    val activeShift = state.activeShift ?: return strings.cashierShiftNotOpen
    val items = (state.catalog as? UiState.Success)?.data
    return buildString {
        append(
            items?.let { catalogItems ->
                strings.cashierShiftSupporting(activeShift.cashierName, catalogItems.size)
            } ?: activeShift.cashierName,
        )
        state.catalogFilter.category?.let { append(" · $it") }
        if (state.catalogFilter.isAvailableOnly) {
            append(" · ${strings.cashierFilterInStockOnly}")
        }
    }
}

@Composable
private fun CashierCategoryChips(
    state: CashierUiState,
    actions: CashierActions,
) {
    if (state.catalogCategories.isEmpty()) return
    val strings = LocalPosStrings.current
    val chipColors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = PosSpacing.xs),
        contentPadding = PaddingValues(horizontal = PosSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        item(key = "__all_categories__") {
            FilterChip(
                selected = state.catalogFilter.category == null,
                onClick = { actions.onCatalogCategoryChange(null) },
                label = { Text(strings.cashierFilterAllCategories) },
                shape = CircleShape,
                border = null,
                colors = chipColors,
            )
        }
        lazyColumnItems(items = state.catalogCategories, key = { it }) { category ->
            val isSelected = state.catalogFilter.category == category
            FilterChip(
                selected = isSelected,
                onClick = {
                    actions.onCatalogCategoryChange(if (isSelected) null else category)
                },
                label = { Text(category) },
                shape = CircleShape,
                border = null,
                colors = chipColors,
            )
        }
    }
}

@Composable
private fun CashierSearchField(
    state: CashierUiState,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PosSpacing.md, end = PosSpacing.md, bottom = PosSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        PosSearchField(
            value = state.searchQuery,
            onValueChange = actions.onSearchQueryChange,
            placeholder = strings.cashierSearchPlaceholder,
            modifier = Modifier.weight(1f),
        )
        CashierScanButton(actions = actions)
        CashierFilterButton(state = state, actions = actions)
    }
}

@Composable
private fun CashierScanButton(actions: CashierActions) {
    val strings = LocalPosStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = PosShapes.large,
        modifier = Modifier
            .size(CATALOG_FILTER_BUTTON_SIZE)
            .clickable(onClickLabel = strings.cashierScanAction, onClick = actions.onOpenScanDialog),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_scan_barcode),
                contentDescription = strings.cashierScanAction,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PosSizes.iconMedium),
            )
        }
    }
}

@Composable
private fun CashierFilterButton(
    state: CashierUiState,
    actions: CashierActions,
) {
    var isMenuVisible by remember { mutableStateOf(false) }
    val strings = LocalPosStrings.current
    val isFilterActive = state.catalogFilter.isActive
    val containerColor = if (isFilterActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isFilterActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = PosShapes.large,
            modifier = Modifier
                .size(CATALOG_FILTER_BUTTON_SIZE)
                .clickable(onClickLabel = strings.cashierFilterProducts, onClick = { isMenuVisible = true }),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_list_filter),
                    contentDescription = strings.cashierFilterProducts,
                    tint = contentColor,
                    modifier = Modifier.size(PosSizes.iconMedium),
                )
            }
        }
        DropdownMenu(
            expanded = isMenuVisible,
            onDismissRequest = { isMenuVisible = false },
        ) {
            DropdownMenuItem(
                text = { Text(strings.cashierFilterAllCategoriesLabel) },
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
                text = { Text(strings.cashierFilterInStockOnly) },
                onClick = { actions.onCatalogAvailableOnlyChange(!state.catalogFilter.isAvailableOnly) },
                trailingIcon = {
                    if (state.catalogFilter.isAvailableOnly) {
                        Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null)
                    }
                },
            )
            if (isFilterActive) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(strings.cashierFilterReset) },
                    onClick = {
                        actions.onCatalogCategoryChange(null)
                        actions.onCatalogAvailableOnlyChange(false)
                        isMenuVisible = false
                    },
                )
            }
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
    val strings = LocalPosStrings.current
    EmptyState(
        iconRes = R.drawable.ic_search_x,
        title = if (isNarrowed) strings.cashierNoProductsSearchTitle else strings.cashierNoProductsTitle,
        supportingText = when {
            isSearching && isFiltered -> strings.cashierNoProductsSearchHint
            isSearching -> strings.cashierNoProductsKeywordHint
            isFiltered -> strings.cashierNoProductsFilterHint
            else -> strings.cashierNoProductsEmptyHint
        },
        modifier = modifier,
        actionLabel = if (isNarrowed) strings.cashierResetSearchAndFilter else null,
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
    val isDepleted = (item.stockStatus == StockStatus.OUT_OF_STOCK || item.stockStatus == StockStatus.OVERSOLD) &&
        item.quantityInCart <= 0.0
    val strings = LocalPosStrings.current
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDepleted) Modifier.graphicsLayer(alpha = 0.65f) else Modifier)
            .clickable(
                onClickLabel = strings.cashierAddProduct(item.product.name),
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
                productName = item.product.name,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Rupiah.format(item.product.basePrice),
                    style = LocalAppTextStyles.current.catalogItemPrice,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                CashierStockBadge(status = item.stockStatus)
            }
            CatalogQuantityControl(item = item, actions = actions, isMuted = isDepleted)
        }
    }
}

@Composable
private fun CashierProductTile(
    item: CashierProductItem,
    actions: CashierActions,
) {
    val product = item.product
    val isDepleted = (item.stockStatus == StockStatus.OUT_OF_STOCK || item.stockStatus == StockStatus.OVERSOLD) &&
        item.quantityInCart <= 0.0
    val strings = LocalPosStrings.current
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDepleted) Modifier.graphicsLayer(alpha = 0.65f) else Modifier)
            .clickable(
                onClickLabel = strings.cashierAddProduct(product.name),
                onClick = { actions.onAddProduct(product.id) },
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
                category = product.category,
                size = CATALOG_TILE_THUMBNAIL_SIZE,
                imagePath = product.imagePath,
                productName = product.name,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Rupiah.format(product.basePrice),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (item.quantityInCart <= 0.0) {
                CatalogCircleButton(
                    iconRes = R.drawable.ic_plus,
                    contentDescription = strings.cashierAddProduct(product.name),
                    onClick = { actions.onAddProduct(product.id) },
                    isMuted = isDepleted,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = product.category ?: strings.commonNoCategory,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.quantityInCart > 0.0) {
                    CatalogQuantityControl(item = item, actions = actions)
                } else {
                    ProductStockPill(status = item.stockStatus, stockBaseQty = product.stockBaseQty)
                }
            }
        }
    }
}

@Composable
private fun ProductStockPill(
    status: StockStatus,
    stockBaseQty: Double,
) {
    if (status != StockStatus.IN_STOCK) {
        StockStatusBadge(status = status)
        return
    }
    val strings = LocalPosStrings.current
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = PosSpacing.xs, vertical = 2.dp),
    ) {
        Text(
            text = strings.cashierStockRemaining(formatQuantity(stockBaseQty)),
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    isMuted: Boolean = false,
) {
    val strings = LocalPosStrings.current
    if (item.quantityInCart <= 0.0) {
        CatalogCircleButton(
            iconRes = R.drawable.ic_plus,
            contentDescription = strings.cashierAddProduct(item.product.name),
            onClick = { actions.onAddProduct(item.product.id) },
            isPrimary = false,
            isMuted = isMuted,
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        CatalogCircleButton(
            iconRes = R.drawable.ic_minus,
            contentDescription = strings.cashierDecreaseProduct(item.product.name),
            onClick = { actions.onDecreaseProductQuantity(item.product.id) },
            isPrimary = false,
        )
        Text(
            text = formatQuantity(item.quantityInCart),
            style = LocalAppTextStyles.current.catalogItemPrice,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = CATALOG_QUANTITY_MIN_WIDTH),
        )
        CatalogCircleButton(
            iconRes = R.drawable.ic_plus,
            contentDescription = strings.cashierAddProduct(item.product.name),
            onClick = { actions.onAddProduct(item.product.id) },
            isPrimary = true,
        )
    }
}

@Composable
private fun CatalogCircleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    isMuted: Boolean = false,
) {
    val containerColor = when {
        isPrimary -> MaterialTheme.colorScheme.primary
        isMuted -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        isPrimary -> MaterialTheme.colorScheme.onPrimary
        isMuted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
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
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(CATALOG_CONTROL_ICON_SIZE),
            )
        }
    }
}
