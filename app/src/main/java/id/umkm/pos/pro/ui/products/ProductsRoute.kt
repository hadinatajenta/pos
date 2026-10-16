package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.monetization.admob.BannerAdSlot
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.PreviewAppContainerProvider
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.StockStatusChip
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosPaneWeights
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

private val PRODUCT_CARD_MIN_WIDTH = 320.dp
private const val MAX_PRODUCT_COLUMNS = 2
private const val FORM_SHEET_HEIGHT_FRACTION = 0.92f

@Composable
fun ProductsRoute(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val productsViewModelFactory = remember(container) {
        viewModelFactory {
            initializer {
                ProductsViewModel(
                    productRepository = container.productRepository,
                    clock = container.clock,
                )
            }
        }
    }
    val viewModel: ProductsViewModel = viewModel(factory = productsViewModelFactory)
    val state by viewModel.state.collectAsState()
    val actions = remember(viewModel) { ProductsActions.from(viewModel) }
    ProductsScreen(state = state, actions = actions, modifier = modifier)
}

@Composable
internal fun ProductsScreen(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        actions.onMessageConsumed()
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= PosBreakpoints.twoPane
        val form = state.form
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "Products",
                supportingText = "Prices, units, and stock",
                actions = {
                    Button(
                        onClick = actions.onOpenCreateForm,
                        modifier = Modifier.heightIn(min = PosSizes.touchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(PosSizes.iconSmall),
                        )
                        Spacer(modifier = Modifier.width(PosSpacing.xs))
                        Text(text = "Add", style = LocalAppTextStyles.current.buttonLabel)
                    }
                },
            )
            OfflineNotice()
            ProductsSearchField(state = state, actions = actions)
            Row(modifier = Modifier.weight(1f)) {
                ProductsListPane(state = state, actions = actions, modifier = Modifier.weight(1f))
                if (isTwoPane && form != null) {
                    VerticalDivider()
                    ProductFormPanel(
                        form = form,
                        isSaving = state.isSaving,
                        actions = actions,
                        modifier = Modifier.weight(PosPaneWeights.detail),
                    )
                }
            }
            BannerAdSlot(placement = AdPlacement.STOCK_MANAGEMENT)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(PosSpacing.md),
        )
        if (!isTwoPane && form != null) {
            ProductFormSheet(form = form, isSaving = state.isSaving, actions = actions)
        }
        if (state.pendingDeleteProductId != null) {
            DeleteProductDialog(actions = actions)
        }
    }
}

@Composable
private fun ProductsSearchField(
    state: ProductsUiState,
    actions: ProductsActions,
) {
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
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun ProductsListPane(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier,
) {
    UiStateContent(
        state = state.products,
        onLoading = { LoadingState(modifier = modifier, label = "Loading products") },
        onEmpty = { ProductsEmptyState(state = state, actions = actions, modifier = modifier) },
        onError = { error -> ErrorState(error = error, onRetry = actions.onRetry, modifier = modifier) },
        onSuccess = { items -> ProductsList(items = items, actions = actions, modifier = modifier) },
    )
}

@Composable
private fun ProductsEmptyState(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier,
) {
    val isSearching = state.searchQuery.isNotBlank()
    EmptyState(
        icon = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.Inventory2,
        title = if (isSearching) "No products match" else "No products yet",
        supportingText = if (isSearching) {
            "Try another name, SKU, or barcode."
        } else {
            "Add your first product to start tracking prices and stock."
        },
        modifier = modifier,
        actionLabel = if (isSearching) "Clear search" else "Add product",
        onAction = if (isSearching) {
            { actions.onSearchQueryChange("") }
        } else {
            actions.onOpenCreateForm
        },
    )
}

@Composable
private fun ProductsList(
    items: List<ProductListItem>,
    actions: ProductsActions,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = (maxWidth / PRODUCT_CARD_MIN_WIDTH).toInt().coerceIn(1, MAX_PRODUCT_COLUMNS)
        val contentPadding = PaddingValues(
            start = PosSpacing.md,
            end = PosSpacing.md,
            top = PosSpacing.xs,
            bottom = PosSizes.listBottomPadding,
        )
        if (columnCount <= 1) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                lazyColumnItems(items = items, key = { item -> item.product.id }) { item ->
                    ProductCard(item = item, actions = actions)
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
                    ProductCard(item = item, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    item: ProductListItem,
    actions: ProductsActions,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onOpenEditForm(item.product.id) },
    ) {
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
                    text = productSupportingText(item.product),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Rupiah.format(item.product.basePrice),
                    style = LocalAppTextStyles.current.financialValue,
                )
                StockStatusChip(status = item.stockStatus)
            }
            IconButton(onClick = { actions.onRequestDelete(item.product.id) }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${item.product.name}",
                )
            }
        }
    }
}

@Composable
private fun DeleteProductDialog(actions: ProductsActions) {
    AlertDialog(
        onDismissRequest = actions.onCancelDelete,
        title = { Text(text = "Delete product") },
        text = {
            Text(
                text = "This removes the product from your catalog. Past sales stay unchanged.",
                style = LocalAppTextStyles.current.supportingLabel,
            )
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirmDelete) {
                Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCancelDelete) {
                Text(text = "Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormSheet(
    form: ProductFormState,
    isSaving: Boolean,
    actions: ProductsActions,
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * FORM_SHEET_HEIGHT_FRACTION).dp
    ModalBottomSheet(
        onDismissRequest = actions.onDismissForm,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ProductFormPanel(
            form = form,
            isSaving = isSaving,
            actions = actions,
            modifier = Modifier.heightIn(max = maxSheetHeight),
        )
    }
}

private fun productSupportingText(product: Product): String {
    val category = product.category?.takeIf { it.isNotBlank() }
    return when {
        category != null -> "$category · ${product.sku}"
        else -> product.sku
    }
}

private fun previewProduct(id: Long, name: String, price: Long, stock: Double, unit: String): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = null,
    name = name,
    category = "Drinks",
    baseUnit = unit,
    baseHpp = price / 2,
    basePrice = price,
    stockBaseQty = stock,
    minStockAlert = 5.0,
    isActive = true,
)

private fun previewProductsState(): ProductsUiState {
    val coffee = previewProduct(id = 1L, name = "Kopi Susu Gula Aren", price = 18_000L, stock = 24.0, unit = "pcs")
    val water = previewProduct(id = 2L, name = "Air Mineral 600ml", price = 4_000L, stock = 2.0, unit = "pcs")
    val soap = previewProduct(id = 3L, name = "Sabun Cuci Piring 1L", price = 12_500L, stock = 0.0, unit = "botol")
    val sugar = previewProduct(id = 4L, name = "Gula Pasir 1kg", price = 15_000L, stock = -3.0, unit = "pack")
    return ProductsUiState(
        searchQuery = "",
        products = UiState.Success(
            listOf(
                ProductListItem(product = coffee, stockStatus = StockStatus.IN_STOCK),
                ProductListItem(product = water, stockStatus = StockStatus.LOW_STOCK),
                ProductListItem(product = soap, stockStatus = StockStatus.OUT_OF_STOCK),
                ProductListItem(product = sugar, stockStatus = StockStatus.OVERSOLD),
            ),
        ),
        form = null,
    )
}

private fun previewProductsActions(): ProductsActions = ProductsActions(
    onSearchQueryChange = {},
    onRetry = {},
    onOpenCreateForm = {},
    onOpenEditForm = {},
    onDismissForm = {},
    onFormFieldChange = { _, _ -> },
    onFormActiveChange = {},
    onFormAdvancedToggle = {},
    onSubmitForm = {},
    onRequestDelete = {},
    onCancelDelete = {},
    onConfirmDelete = {},
    onMessageConsumed = {},
)

@Preview(name = "Products compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProductsCompactPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(state = previewProductsState(), actions = previewProductsActions())
        }
    }
}

@Preview(name = "Products form", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProductsFormPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = previewProductsState().copy(form = ProductFormState(name = "Kopi Susu", basePrice = "18000")),
                actions = previewProductsActions(),
            )
        }
    }
}

@Preview(name = "Products expanded", widthDp = 960, heightDp = 720, showBackground = true)
@Composable
private fun ProductsExpandedPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = previewProductsState().copy(form = ProductFormState(name = "Kopi Susu", basePrice = "18000")),
                actions = previewProductsActions(),
            )
        }
    }
}
