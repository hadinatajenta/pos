package id.umkm.pos.pro.ui.cashier

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.shift.LocalShiftLauncher
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosPaneWeights
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import androidx.compose.runtime.getValue

private enum class CashierStage {
    CATALOG,
    CART,
    PAYMENT,
}

@Composable
fun CashierRoute(
    onShareReceipt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val cashierViewModelFactory = remember(container) {
        viewModelFactory {
            initializer {
                CashierViewModel(
                    productRepository = container.productRepository,
                    settingsRepository = container.settingsRepository,
                    priceEngine = container.priceEngine,
                    checkoutUseCase = CheckoutUseCase(
                        productRepository = container.productRepository,
                        transactionRepository = container.transactionRepository,
                        priceEngine = container.priceEngine,
                        clock = container.clock,
                    ),
                    receiptPrinter = container.receiptPrinter,
                    receiptRenderer = container.receiptRenderer,
                    scannerInput = container.scannerInput,
                    shiftRepository = container.shiftRepository,
                    staffRepository = container.staffRepository,
                )
            }
        }
    }
    val viewModel: CashierViewModel = viewModel(factory = cashierViewModelFactory)
    val state by viewModel.state.collectAsState()
    val shiftLauncher = LocalShiftLauncher.current
    val actions = remember(viewModel, shiftLauncher) { CashierActions.from(viewModel, shiftLauncher) }
    CashierScreen(
        state = state,
        actions = actions,
        onShareReceipt = onShareReceipt,
        modifier = modifier,
    )
}

@Composable
internal fun CashierScreen(
    state: CashierUiState,
    actions: CashierActions,
    onShareReceipt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.asTextOrNull()
    LaunchedEffect(messageText, state.shouldSuppressMessageSnackbar) {
        if (messageText == null) return@LaunchedEffect
        if (!state.shouldSuppressMessageSnackbar) {
            snackbarHostState.showSnackbar(messageText)
        }
        actions.onMessageConsumed()
    }
    LaunchedEffect(state.isShiftPromptVisible) {
        if (state.isShiftPromptVisible) {
            actions.onShiftPromptConsumed()
            if (state.activeShift != null) {
                actions.onShiftOpenedForPayment()
            } else {
                actions.onOpenQuickShift {
                    actions.onShiftOpenedForPayment()
                }
            }
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= PosBreakpoints.twoPane
        val showPayment = state.isPaymentSheetVisible
        val showCart = !isTwoPane && !showPayment && state.isCartSheetVisible
        val stage = when {
            showPayment -> CashierStage.PAYMENT
            showCart -> CashierStage.CART
            else -> CashierStage.CATALOG
        }
        BackHandler(enabled = showPayment) { actions.onClosePaymentSheet() }
        BackHandler(enabled = showCart) { actions.onCloseCartSheet() }
        LaunchedEffect(state.lineCount, state.isPaymentSheetVisible, state.isCartSheetVisible) {
            if (state.lineCount > 0) return@LaunchedEffect
            if (state.isPaymentSheetVisible) actions.onClosePaymentSheet()
            if (state.isCartSheetVisible) actions.onCloseCartSheet()
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    if (isForward) {
                        (slideInHorizontally(animationSpec = tween(240)) { width -> width } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(220)) { width -> -width / 4 } + fadeOut(tween(160)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(240)) { width -> -width } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(220)) { width -> width / 4 } + fadeOut(tween(160)))
                    }
                },
                label = "cashierCheckoutStage",
            ) { currentStage ->
                when (currentStage) {
                    CashierStage.PAYMENT -> CashierPaymentPage(
                        state = state,
                        actions = actions,
                        onBack = actions.onClosePaymentSheet,
                    )

                    CashierStage.CART -> CashierCartPage(
                        state = state,
                        actions = actions,
                        onBack = actions.onCloseCartSheet,
                    )

                    CashierStage.CATALOG -> {
                        if (isTwoPane) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                CashierCatalogPane(
                                    state = state,
                                    actions = actions,
                                    modifier = Modifier.weight(PosPaneWeights.catalog),
                                )
                                VerticalDivider()
                                CashierCartPane(
                                    state = state,
                                    actions = actions,
                                    modifier = Modifier.weight(PosPaneWeights.cart),
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CashierCatalogPane(
                                    state = state,
                                    actions = actions,
                                    modifier = Modifier.weight(1f),
                                )
                                CashierCartBar(state = state, actions = actions)
                            }
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PosSpacing.md),
            )
        }
        state.receipt?.let { receipt ->
            CashierReceiptSheet(
                receipt = receipt,
                isPrintingReceipt = state.isPrintingReceipt,
                receiptMessage = state.receiptMessage,
                actions = actions,
                onShareReceipt = onShareReceipt,
            )
        }
    }
}

private fun previewProduct(id: Long, name: String, price: Long, stock: Double): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = null,
    name = name,
    category = "Minuman",
    baseUnit = "pcs",
    baseHpp = price / 2,
    basePrice = price,
    stockBaseQty = stock,
    minStockAlert = 5.0,
    isActive = true,
)

private fun previewCartLine(product: Product, quantity: Double): CashierCartLine = CashierCartLine(
    lineId = "D${product.id}",
    productId = product.id,
    unitId = null,
    productName = product.name,
    variantName = null,
    unitLabel = product.baseUnit,
    quantity = quantity,
    unitPrice = product.basePrice,
    itemDiscount = 0L,
    lineSubtotal = product.basePrice * quantity.toLong(),
    isNegotiated = false,
    isWholesale = false,
    categoryName = product.category.orEmpty(),
    imagePath = product.imagePath,
    icon = product.icon,
    stock = product.stockBaseQty,
)

private fun previewPricedCart(grandTotal: Long): PricedCart = PricedCart(
    lines = emptyList(),
    subtotal = grandTotal,
    invoiceDiscount = 0L,
    taxTotal = 0L,
    rounding = 0L,
    grandTotal = grandTotal,
    totalHpp = grandTotal / 2,
    netProfit = grandTotal / 2,
)

private fun previewCashierState(): CashierUiState {
    val coffee = previewProduct(id = 1L, name = "Kopi Susu Gula Aren", price = 18_000L, stock = 24.0)
    val water = previewProduct(id = 2L, name = "Air Mineral 600ml", price = 4_000L, stock = 2.0)
    val soap = previewProduct(id = 3L, name = "Sabun Cuci Piring", price = 12_500L, stock = 0.0)
    val cartLines = listOf(
        previewCartLine(product = coffee, quantity = 2.0),
        previewCartLine(product = water, quantity = 1.0),
    )
    val grandTotal = cartLines.sumOf { it.lineSubtotal }
    return CashierUiState(
        searchQuery = "",
        catalog = UiState.Success(
            listOf(
                CashierProductItem(product = coffee, stockStatus = resolveStockStatus(coffee)),
                CashierProductItem(product = water, stockStatus = resolveStockStatus(water)),
                CashierProductItem(product = soap, stockStatus = resolveStockStatus(soap)),
            ),
        ),
        cartLines = cartLines,
        pricedCart = previewPricedCart(grandTotal),
        activeShift = CashierShift(
            id = 1L,
            cashierName = "Dini",
            openedAt = 0L,
            closedAt = null,
            startingCash = 100_000L,
            expectedEndingCash = null,
            actualEndingCash = null,
        ),
        activeStaff = Staff(id = 1L, name = "Dini"),
    )
}

private fun previewCashierActions(): CashierActions = CashierActions(
    onSearchQueryChange = {},
    onCatalogCategoryChange = {},
    onCatalogAvailableOnlyChange = {},
    onRetryCatalog = {},
    onAddProduct = {},
    onDecreaseProductQuantity = {},
    onIncreaseQuantity = {},
    onDecreaseQuantity = {},
    onRemoveLine = {},
    onOpenCartSheet = {},
    onCloseCartSheet = {},
    onOpenLineEditor = {},
    onCloseLineEditor = {},
    onLineEditorUnitChange = {},
    onLineEditorPriceChange = {},
    onLineEditorDiscountChange = {},
    onApplyLineEditor = {},
    onOpenPaymentSheet = {},
    onClosePaymentSheet = {},
    onPaymentMethodChange = {},
    onAmountReceivedChange = {},
    onConfirmCharge = {},
    onPrintReceipt = {},
    onNewSale = {},
    onMessageConsumed = {},
)

@Preview(name = "Cashier compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CashierCompactPreview() {
    UmkmPosTheme {
        CashierScreen(
            state = previewCashierState(),
            actions = previewCashierActions(),
            onShareReceipt = {},
        )
    }
}

@Preview(name = "Cashier two pane", widthDp = 960, heightDp = 720, showBackground = true)
@Composable
private fun CashierTwoPanePreview() {
    UmkmPosTheme {
        CashierScreen(
            state = previewCashierState(),
            actions = previewCashierActions(),
            onShareReceipt = {},
        )
    }
}
