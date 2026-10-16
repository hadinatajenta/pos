package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.PreviewAppContainerProvider
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.StockStatusBadge
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosPaneWeights
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

private val PRODUCT_ROW_THUMBNAIL_SIZE = 44.dp
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
                title = "Produk",
                supportingText = "Harga, satuan, dan stok",
                actions = { AddProductAction(onClick = actions.onOpenCreateForm) },
            )
            OfflineNotice()
            ProductsSearchField(state = state, actions = actions)
            ProductsStockFilterRow(selected = state.stockFilter, onSelect = actions.onStockFilterChange)
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
private fun AddProductAction(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = PosSizes.touchTarget),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(PosSpacing.xs))
        Text(text = "Tambah", style = LocalAppTextStyles.current.buttonLabel)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductsStockFilterRow(
    selected: ProductStockFilter,
    onSelect: (ProductStockFilter) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        ProductStockFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun ProductsListPane(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier,
) {
    UiStateContent(
        state = state.products,
        onLoading = { LoadingState(modifier = modifier, label = "Memuat produk") },
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
    val isFiltered = state.stockFilter != ProductStockFilter.ALL
    val icon = when {
        isSearching -> Icons.Filled.SearchOff
        isFiltered -> Icons.Filled.FilterAlt
        else -> Icons.Filled.Inventory2
    }
    val title = when {
        isSearching -> "Produk tidak ditemukan"
        isFiltered -> "Tidak ada produk pada filter ini"
        else -> "Belum ada produk"
    }
    val supportingText = when {
        isSearching -> "Coba kata kunci, SKU, atau barcode lain."
        isFiltered -> "Pilih filter lain atau tambahkan produk baru."
        else -> "Tambahkan produk pertama untuk mencatat harga dan stok."
    }
    val actionLabel = when {
        isSearching -> "Hapus pencarian"
        isFiltered -> "Tampilkan semua"
        else -> "Tambah produk"
    }
    val onAction = when {
        isSearching -> { { actions.onSearchQueryChange("") } }
        isFiltered -> { { actions.onStockFilterChange(ProductStockFilter.ALL) } }
        else -> actions.onOpenCreateForm
    }
    EmptyState(
        icon = icon,
        title = title,
        supportingText = supportingText,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

@Composable
private fun ProductsList(
    items: List<ProductListItem>,
    actions: ProductsActions,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = PosSpacing.md,
            end = PosSpacing.md,
            top = PosSpacing.xs,
            bottom = PosSizes.listBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        lazyColumnItems(items = items, key = { item -> item.product.id }) { item ->
            ProductRow(item = item, actions = actions)
        }
    }
}

@Composable
private fun ProductRow(
    item: ProductListItem,
    actions: ProductsActions,
) {
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onOpenEditForm(item.product.id) },
        contentPadding = PosSpacing.sm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            ProductThumbnail(category = item.product.category, size = PRODUCT_ROW_THUMBNAIL_SIZE)
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
                Text(
                    text = Rupiah.format(item.product.basePrice),
                    style = LocalAppTextStyles.current.financialValue,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                ) {
                    Text(
                        text = stockText(item.product),
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StockStatusBadge(status = item.stockStatus)
                }
            }
            IconButton(onClick = { actions.onRequestDelete(item.product.id) }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Hapus ${item.product.name}",
                )
            }
        }
    }
}

@Composable
private fun DeleteProductDialog(actions: ProductsActions) {
    AlertDialog(
        onDismissRequest = actions.onCancelDelete,
        title = { Text(text = "Hapus produk") },
        text = {
            Text(
                text = "Produk akan dihapus dari katalog. Riwayat penjualan tetap tersimpan.",
                style = LocalAppTextStyles.current.supportingLabel,
            )
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirmDelete) {
                Text(text = "Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCancelDelete) {
                Text(text = "Batal")
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

private fun stockText(product: Product): String =
    "Stok ${formatQuantity(product.stockBaseQty)} ${product.baseUnit}"

private fun previewProduct(id: Long, name: String, price: Long, stock: Double, unit: String): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = null,
    name = name,
    category = "Minuman",
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
    onStockFilterChange = {},
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
