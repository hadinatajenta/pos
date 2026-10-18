package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.monetization.admob.BannerAdSlot
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.HeaderActionButton
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val FORM_MAX_WIDTH = 560.dp
private const val FORM_HEIGHT_FRACTION = 0.92f

@Composable
private fun rememberProductsActions(viewModel: ProductsViewModel): ProductsActions =
    remember(viewModel) { ProductsActions.from(viewModel) }

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
    val actions = rememberProductsActions(viewModel)
    ProductsScreen(state = state, actions = actions, modifier = modifier)
}

@Composable
internal fun ProductsScreen(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalPosStrings.current
    val messageText = state.message?.asTextOrNull()
    LaunchedEffect(messageText) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        actions.onMessageConsumed()
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < PosBreakpoints.expandedNavigation
        Column(modifier = Modifier.fillMaxSize()) {
            OfflineNotice()
            ScreenHeader(
                title = strings.productsTitle,
                supportingText = state.catalog.successDataOrNull()?.let { catalog ->
                    strings.productsRegisteredCount(catalog.stats.totalCount)
                },
                actions = {
                    HeaderActionButton(
                        text = strings.productsAdd,
                        iconRes = R.drawable.ic_plus,
                        contentDescription = strings.productsAddAction,
                        onClick = actions.onOpenCreateForm,
                    )
                },
            )
            ProductsSearchAndSortBar(
                state = state,
                actions = actions,
                isCompact = isCompact,
            )
            ProductsCatalogSection(
                state = state,
                actions = actions,
                isCompact = isCompact,
                modifier = Modifier.weight(1f),
            )
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
private fun ProductsCatalogSection(
    state: ProductsUiState,
    actions: ProductsActions,
    isCompact: Boolean,
    modifier: Modifier,
) {
    val strings = LocalPosStrings.current
    UiStateContent(
        state = state.catalog,
        onLoading = { LoadingState(modifier = modifier, label = strings.productsLoading) },
        onEmpty = { ProductsEmptyCatalog(state = state, actions = actions, modifier = modifier) },
        onError = { error -> ErrorState(error = error, onRetry = actions.onRetry, modifier = modifier) },
        onSuccess = { catalog ->
            ProductsCatalogList(
                state = state,
                catalog = catalog,
                actions = actions,
                isCompact = isCompact,
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
    val strings = LocalPosStrings.current
    EmptyState(
        iconRes = if (isSearching) R.drawable.ic_search_x else R.drawable.ic_package,
        title = if (isSearching) strings.productsNotFoundTitle else strings.productsEmptyTitle,
        supportingText = if (isSearching) {
            strings.productsNotFoundHint
        } else {
            strings.productsEmptyHint
        },
        modifier = modifier,
        actionLabel = if (isSearching) strings.productsClearSearch else strings.productsAddFirst,
        onAction = if (isSearching) {
            { actions.onSearchQueryChange("") }
        } else {
            actions.onOpenCreateForm
        },
    )
}

@Composable
private fun DeleteProductDialog(actions: ProductsActions) {
    val strings = LocalPosStrings.current
    AlertDialog(
        onDismissRequest = actions.onCancelDelete,
        title = { Text(text = strings.productsDeleteConfirmTitle) },
        text = {
            Text(
                text = strings.productsDeleteConfirmMsg,
                style = LocalAppTextStyles.current.supportingLabel,
            )
        },
        confirmButton = {
            TextButton(
                onClick = actions.onConfirmDelete,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.actionDelete)
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onCancelDelete,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.actionCancel)
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
