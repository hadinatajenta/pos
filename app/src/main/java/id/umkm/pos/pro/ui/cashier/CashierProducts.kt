package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.PosSearchField
import id.umkm.pos.pro.ui.products.ProductCard
import id.umkm.pos.pro.ui.products.ProductPalette
import id.umkm.pos.pro.ui.products.ProductRadii
import id.umkm.pos.pro.ui.products.buildProductCardView
import id.umkm.pos.pro.ui.scanner.BarcodeScanDialog
import id.umkm.pos.pro.ui.theme.PosFontFamily
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items as lazyColumnItems

private val CASHIER_LIST_HORIZONTAL_PADDING = 20.dp
private val CASHIER_LIST_TOP_PADDING = 8.dp
private val CASHIER_LIST_BOTTOM_PADDING = 24.dp
private val CASHIER_LIST_GAP = 10.dp
private val CASHIER_ACTION_WIDTH = 104.dp
private val CASHIER_ADD_BUTTON_SIZE = 46.dp
private val CASHIER_ADD_ICON_SIZE = 20.dp
private val CASHIER_STEPPER_HEIGHT = 40.dp
private val CASHIER_STEPPER_BUTTON_WIDTH = 34.dp
private val CASHIER_STEPPER_VISUAL_SIZE = 30.dp
private val CASHIER_STEPPER_ICON_SIZE = 16.dp
private val CASHIER_STEPPER_CORNER = RoundedCornerShape(16.dp)
private val CASHIER_STEPPER_BUTTON_CORNER = RoundedCornerShape(10.dp)
private val CATALOG_FILTER_BUTTON_SIZE = 56.dp

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
                CashierIdentityPill(
                    cashierName = state.activeStaff?.name ?: state.activeShift?.cashierName,
                    isShiftOpen = state.activeShift != null,
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
    if (state.activeShift == null) return strings.cashierShiftNotOpen
    val items = (state.catalog as? UiState.Success)?.data ?: return null
    return buildString {
        append(strings.cashierProductCount(items.size))
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = CASHIER_LIST_HORIZONTAL_PADDING,
            end = CASHIER_LIST_HORIZONTAL_PADDING,
            top = CASHIER_LIST_TOP_PADDING,
            bottom = CASHIER_LIST_BOTTOM_PADDING,
        ),
        verticalArrangement = Arrangement.spacedBy(CASHIER_LIST_GAP),
    ) {
        lazyColumnItems(items = items, key = { item -> item.product.id }) { item ->
            CashierProductRow(item = item, actions = actions)
        }
    }
}

@Composable
private fun CashierProductRow(
    item: CashierProductItem,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    ProductCard(
        card = buildProductCardView(item.product),
        modifier = Modifier.fillMaxWidth(),
        isSelected = item.quantityInCart > 0.0,
        onClickLabel = strings.cashierAddProduct(item.product.name),
        metaLabel = strings.productsStockSummary(formatQuantity(item.product.stockBaseQty)),
        onClick = { actions.onAddProduct(item.product.id) },
        trailingContent = {
            Box(
                modifier = Modifier.width(CASHIER_ACTION_WIDTH),
                contentAlignment = Alignment.CenterEnd,
            ) {
                CashierQuantityControl(item = item, actions = actions)
            }
        },
    )
}

@Composable
private fun CashierQuantityControl(
    item: CashierProductItem,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    if (item.quantityInCart <= 0.0) {
        CashierAddButton(
            contentDescription = strings.cashierAddProduct(item.product.name),
            onClick = { actions.onAddProduct(item.product.id) },
        )
        return
    }
    Row(
        modifier = Modifier
            .width(CASHIER_ACTION_WIDTH)
            .height(CASHIER_STEPPER_HEIGHT)
            .clip(CASHIER_STEPPER_CORNER)
            .background(ProductPalette.Brand),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CashierStepperButton(
            iconRes = R.drawable.ic_minus,
            contentDescription = strings.cashierDecreaseProduct(item.product.name),
            onClick = { actions.onDecreaseProductQuantity(item.product.id) },
        )
        Text(
            text = formatQuantity(item.quantityInCart),
            modifier = Modifier.weight(1f),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                fontFamily = PosFontFamily,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum",
            ),
        )
        CashierStepperButton(
            iconRes = R.drawable.ic_plus,
            contentDescription = strings.cashierAddProduct(item.product.name),
            onClick = { actions.onAddProduct(item.product.id) },
        )
    }
}

@Composable
private fun CashierAddButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = ProductPalette.SafeContainer,
        contentColor = ProductPalette.SafeText,
        shape = ProductRadii.input,
        modifier = Modifier.size(CASHIER_ADD_BUTTON_SIZE),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = contentDescription,
                tint = ProductPalette.SafeText,
                modifier = Modifier.size(CASHIER_ADD_ICON_SIZE),
            )
        }
    }
}

@Composable
private fun CashierStepperButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(CASHIER_STEPPER_BUTTON_WIDTH)
            .fillMaxHeight()
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(CASHIER_STEPPER_VISUAL_SIZE)
                .clip(CASHIER_STEPPER_BUTTON_CORNER)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(CASHIER_STEPPER_ICON_SIZE),
            )
        }
    }
}
