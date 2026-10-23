package id.umkm.pos.pro.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.LocalFullScreenContent
import id.umkm.pos.pro.ui.common.HeaderActionButton
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private const val TABLET_FORM_WIDTH_FRACTION = 0.40f
private const val COMPACT_FORM_WIDTH_FRACTION = 0.92f
private val TABLET_FORM_MIN_WIDTH = 360.dp
private val TABLET_FORM_MAX_WIDTH = 560.dp
private val SEARCH_BAR_HORIZONTAL_PADDING = 16.dp
private val SEARCH_BAR_VERTICAL_PADDING = 4.dp
private val SNACKBAR_BOTTOM_PADDING = 16.dp
private val FLOATING_ACTION_HEIGHT = 54.dp
private val FLOATING_ACTION_END_PADDING = 20.dp
private val FLOATING_ACTION_BOTTOM_PADDING = 20.dp

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
    ProductsScreen(
        state = state,
        actions = actions,
        modifier = modifier,
    )
}

@Composable
internal fun ProductsScreen(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val setFullScreenContent = LocalFullScreenContent.current
    val form = state.form
    val isFormOpen = form != null
    DisposableEffect(isFormOpen) {
        setFullScreenContent(isFormOpen)
        onDispose { setFullScreenContent(false) }
    }
    BackHandler(enabled = isFormOpen) { actions.onRequestDismissForm() }
    val messageText = state.message?.asTextOrNull()
    LaunchedEffect(messageText) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        actions.onMessageConsumed()
    }
    val categorySuggestions = state.catalog.successDataOrNull()?.categories.orEmpty()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ProductPalette.Background),
    ) {
        val isCompact = maxWidth < PosBreakpoints.expandedNavigation
        val tabletFormWidth = (maxWidth * TABLET_FORM_WIDTH_FRACTION)
            .coerceIn(TABLET_FORM_MIN_WIDTH, TABLET_FORM_MAX_WIDTH)
        Column(modifier = Modifier.fillMaxSize()) {
            OfflineNotice()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                ProductsCatalogPage(
                    state = state,
                    actions = actions,
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f),
                )
                if (!isCompact && form != null) {
                    TabletFormPanel(
                        form = form,
                        isSaving = state.isSaving,
                        categorySuggestions = categorySuggestions,
                        actions = actions,
                        width = tabletFormWidth,
                    )
                }
            }
        }
        if (isCompact && form != null) {
            CompactFormDrawer(
                form = form,
                isSaving = state.isSaving,
                categorySuggestions = categorySuggestions,
                actions = actions,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    horizontal = PosSpacing.md,
                    vertical = SNACKBAR_BOTTOM_PADDING,
                ),
        )
        if (state.pendingDeleteProductId != null) {
            DeleteProductDialog(productName = state.pendingDeleteProductName, actions = actions)
        }
        if (state.isDiscardConfirmationVisible) {
            DiscardChangesDialog(isSaving = state.isSaving, actions = actions)
        }
    }
}

@Composable
private fun ProductsCatalogPage(
    state: ProductsUiState,
    actions: ProductsActions,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = strings.productsTitle,
                supportingText = state.catalog.successDataOrNull()?.let { catalog ->
                    strings.productsRegisteredCount(catalog.stats.totalCount)
                },
                actions = {
                    if (!isCompact) {
                        HeaderActionButton(
                            text = strings.productsAdd,
                            iconRes = R.drawable.ic_plus,
                            contentDescription = strings.productsAddAction,
                            onClick = actions.onOpenCreateForm,
                        )
                    }
                },
            )
            ProductsSearchBar(
                state = state,
                actions = actions,
                modifier = Modifier.padding(
                    horizontal = SEARCH_BAR_HORIZONTAL_PADDING,
                    vertical = SEARCH_BAR_VERTICAL_PADDING,
                ),
            )
            ProductsCatalogSection(
                state = state,
                actions = actions,
                isCompact = isCompact,
                isLoading = state.catalog is UiState.Loading,
                modifier = Modifier.weight(1f),
            )
        }
        if (isCompact && !state.isCatalogTrulyEmpty()) {
            ProductsAddAction(
                onClick = actions.onOpenCreateForm,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = FLOATING_ACTION_END_PADDING,
                        bottom = FLOATING_ACTION_BOTTOM_PADDING,
                    ),
            )
        }
    }
}

@Composable
private fun ProductsAddAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Surface(
        onClick = onClick,
        color = ProductPalette.Brand,
        contentColor = Color.White,
        shape = ProductRadii.button,
        shadowElevation = 12.dp,
        modifier = modifier.height(FLOATING_ACTION_HEIGHT),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = strings.productsAdd,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

private fun ProductsUiState.isCatalogTrulyEmpty(): Boolean {
    val catalogData = catalog.successDataOrNull()
    if (catalogData != null) return catalogData.stats.totalCount == 0
    return catalog is UiState.Empty
}

@Composable
private fun TabletFormPanel(
    form: ProductFormState,
    isSaving: Boolean,
    categorySuggestions: List<String>,
    actions: ProductsActions,
    width: Dp,
) {
    androidx.compose.runtime.key(form.formInstanceId) {
        Surface(
            color = ProductPalette.Surface,
            contentColor = ProductPalette.Ink,
            shape = ProductRadii.drawer,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(width)
                .fillMaxHeight(),
        ) {
            ProductFormPanel(
                form = form,
                isSaving = isSaving,
                categorySuggestions = categorySuggestions,
                actions = actions,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CompactFormDrawer(
    form: ProductFormState,
    isSaving: Boolean,
    categorySuggestions: List<String>,
    actions: ProductsActions,
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        val scrimInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = scrimInteractionSource,
                    indication = null,
                    onClickLabel = LocalPosStrings.current.actionCancel,
                    onClick = actions.onRequestDismissForm,
                ),
        )
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            androidx.compose.runtime.key(form.formInstanceId) {
                Surface(
                    color = ProductPalette.Surface,
                    contentColor = ProductPalette.Ink,
                    shape = ProductRadii.drawer,
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(COMPACT_FORM_WIDTH_FRACTION),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        ProductFormPanel(
                            form = form,
                            isSaving = isSaving,
                            categorySuggestions = categorySuggestions,
                            actions = actions,
                            modifier = Modifier
                                .width(FORM_CONTENT_MAX_WIDTH)
                                .fillMaxWidth()
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteProductDialog(
    productName: String?,
    actions: ProductsActions,
) {
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val trimmedName = productName?.trim()?.takeIf { name -> name.isNotEmpty() }
    AlertDialog(
        onDismissRequest = actions.onCancelDelete,
        title = {
            Text(
                text = trimmedName?.let { name -> strings.productsDeleteConfirmTitleNamed(name) }
                    ?: strings.productsDeleteConfirmTitle,
            )
        },
        text = {
            Text(
                text = strings.productsDeleteConfirmMsg,
                style = LocalAppTextStyles.current.supportingLabel,
            )
        },
        confirmButton = {
            DangerActionButton(
                text = strings.actionDelete,
                onClick = {
                    gate.request(CriticalAction.DELETE_PRODUCT, strings.actionDelete) {
                        actions.onConfirmDelete()
                    }
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = actions.onCancelDelete,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.actionCancel, style = LocalAppTextStyles.current.buttonLabel)
            }
        },
    )
}

@Composable
private fun DiscardChangesDialog(
    isSaving: Boolean,
    actions: ProductsActions,
) {
    val strings = LocalPosStrings.current
    AlertDialog(
        onDismissRequest = actions.onCancelDiscardForm,
        title = { Text(text = strings.productsDiscardTitle) },
        text = {
            Text(
                text = strings.productsDiscardMessage,
                style = LocalAppTextStyles.current.supportingLabel,
            )
        },
        confirmButton = {
            DangerActionButton(
                text = strings.productsDiscardConfirm,
                onClick = actions.onConfirmDiscardForm,
                enabled = !isSaving,
            )
        },
        dismissButton = {
            TextButton(
                onClick = actions.onCancelDiscardForm,
                enabled = !isSaving,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.productsDiscardKeepEditing, style = LocalAppTextStyles.current.buttonLabel)
            }
        },
    )
}

@Composable
private fun DangerActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val palette = LocalPosSemanticPalette.current
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.dangerText,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier.heightIn(min = PosSizes.touchTarget),
    ) {
        Text(text = text, style = LocalAppTextStyles.current.buttonLabel)
    }
}
