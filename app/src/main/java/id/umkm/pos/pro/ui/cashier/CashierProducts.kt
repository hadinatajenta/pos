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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.ui.common.CenteredScreenHeader
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.PosSearchField
import id.umkm.pos.pro.ui.products.ProductCard
import id.umkm.pos.pro.ui.products.ProductPalette
import id.umkm.pos.pro.ui.products.ProductRadii
import id.umkm.pos.pro.ui.products.buildProductCardView
import id.umkm.pos.pro.ui.scanner.BarcodeScanDialog
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosFontFamily
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import androidx.annotation.DrawableRes
import androidx.compose.foundation.lazy.items as lazyColumnItems

private val CASHIER_FILTER_CONTAINER_SHAPE = RoundedCornerShape(18.dp)
private val CASHIER_FILTER_CONTAINER_HORIZONTAL_MARGIN = 16.dp
private val CASHIER_FILTER_CONTAINER_PADDING = 6.dp
private val CASHIER_FILTER_CHIP_HEIGHT = 36.dp
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
        CenteredScreenHeader(
            title = strings.cashierTitle,
            supportingText = catalogSupportingText(state, strings),
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = CASHIER_FILTER_CONTAINER_HORIZONTAL_MARGIN,
                end = CASHIER_FILTER_CONTAINER_HORIZONTAL_MARGIN,
                top = 12.dp,
                bottom = 8.dp,
            )
            .shadow(
                elevation = 4.dp,
                shape = CASHIER_FILTER_CONTAINER_SHAPE,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .background(ProductPalette.Surface, CASHIER_FILTER_CONTAINER_SHAPE),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = CASHIER_FILTER_CONTAINER_PADDING),
            contentPadding = PaddingValues(horizontal = CASHIER_FILTER_CONTAINER_PADDING),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "__all_categories__") {
                CashierCategoryChip(
                    label = strings.cashierFilterAllCategories,
                    selected = state.catalogFilter.category == null,
                    onClick = { actions.onCatalogCategoryChange(null) },
                )
            }
            lazyColumnItems(items = state.catalogCategories, key = { it }) { category ->
                val isSelected = state.catalogFilter.category == category
                CashierCategoryChip(
                    label = category,
                    selected = isSelected,
                    onClick = {
                        actions.onCatalogCategoryChange(if (isSelected) null else category)
                    },
                )
            }
        }
    }
}

@Composable
private fun CashierCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) ProductPalette.Brand else Color.Transparent,
        contentColor = if (selected) Color.White else ProductPalette.Muted,
        shape = CircleShape,
        modifier = Modifier.height(CASHIER_FILTER_CHIP_HEIGHT),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
            modifier = Modifier
                .weight(1f)
                .shadow(
                    elevation = 2.dp,
                    shape = PosShapes.large,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.04f),
                ),
            containerColor = ProductPalette.Surface,
            focusedContainerColor = ProductPalette.Surface,
        )
        CashierScanButton(actions = actions)
    }
}

@Composable
private fun CashierScanButton(actions: CashierActions) {
    val strings = LocalPosStrings.current
    Surface(
        color = ProductPalette.Surface,
        contentColor = ProductPalette.Muted,
        shape = PosShapes.large,
        shadowElevation = 2.dp,
        modifier = Modifier
            .size(CATALOG_FILTER_BUTTON_SIZE)
            .clickable(onClickLabel = strings.cashierScanAction, onClick = actions.onOpenScanDialog),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_scan_barcode),
                contentDescription = strings.cashierScanAction,
                tint = ProductPalette.Muted,
                modifier = Modifier.size(PosSizes.iconMedium),
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
