package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.monetization.admob.BannerAdSlot
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val PRODUCTS_CONTENT_SHAPE = RoundedCornerShape(topStart = PosSpacing.lg, topEnd = PosSpacing.lg)
private val FORM_MAX_WIDTH = 560.dp
private const val FORM_HEIGHT_FRACTION = 0.92f

@Composable
fun ProductsRoute(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val productsViewModelFactory = remember(container) {
        viewModelFactory {
            initializer {
                ProductsViewModel(
                    productRepository = container.productRepository,
                    productImageRepository = container.productImageRepository,
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
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PosColors.ForestGreen),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = PRODUCTS_CONTENT_SHAPE,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineNotice()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = PosSpacing.md, end = PosSpacing.md, top = PosSpacing.md),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AddProductButton(onClick = actions.onOpenCreateForm)
                    }
                    ProductsSearchField(state = state, actions = actions)
                    ProductsFilterRow(state = state, actions = actions)
                    ProductsCatalogPane(
                        state = state,
                        actions = actions,
                        modifier = Modifier.weight(1f),
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
        state.form?.let { form ->
            ProductFormDialog(form = form, isSaving = state.isSaving, actions = actions)
        }
        if (state.pendingDeleteProductId != null) {
            DeleteProductDialog(actions = actions)
        }
    }
}

@Composable
private fun ProductsCatalogPane(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier,
) {
    UiStateContent(
        state = state.catalog,
        onLoading = { LoadingState(modifier = modifier, label = "Memuat produk") },
        onEmpty = { ProductsEmptyCatalog(state = state, actions = actions, modifier = modifier) },
        onError = { error -> ErrorState(error = error, onRetry = actions.onRetry, modifier = modifier) },
        onSuccess = { catalog ->
            ProductsCatalogList(
                catalog = catalog,
                categoryFilter = state.categoryFilter,
                actions = actions,
                modifier = modifier,
            )
        },
    )
}

@Composable
private fun ProductsEmptyCatalog(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier,
) {
    val isSearching = state.searchQuery.isNotBlank()
    EmptyState(
        icon = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.Inventory2,
        title = if (isSearching) "Produk tidak ditemukan" else "Belum ada produk",
        supportingText = if (isSearching) {
            "Coba kata kunci lain atau periksa ejaan nama produk."
        } else {
            "Tambahkan produk pertama untuk mencatat harga dan stok."
        },
        modifier = modifier,
        actionLabel = if (isSearching) "Hapus pencarian" else "Tambah produk",
        onAction = if (isSearching) {
            { actions.onSearchQueryChange("") }
        } else {
            actions.onOpenCreateForm
        },
    )
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
            TextButton(
                onClick = actions.onConfirmDelete,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = "Hapus")
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onCancelDelete,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = "Batal")
            }
        },
    )
}

@Composable
private fun ProductFormDialog(
    form: ProductFormState,
    isSaving: Boolean,
    actions: ProductsActions,
) {
    Dialog(
        onDismissRequest = actions.onDismissForm,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(PosSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = FORM_MAX_WIDTH)
                    .fillMaxWidth()
                    .fillMaxHeight(FORM_HEIGHT_FRACTION),
            ) {
                ProductFormPanel(
                    form = form,
                    isSaving = isSaving,
                    actions = actions,
                )
            }
        }
    }
}
