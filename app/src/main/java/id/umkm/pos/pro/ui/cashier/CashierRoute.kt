package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosPaneWeights
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

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
                    customerRepository = container.customerRepository,
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
                )
            }
        }
    }
    val viewModel: CashierViewModel = viewModel(factory = cashierViewModelFactory)
    val state by viewModel.state.collectAsState()
    val actions = remember(viewModel) { CashierActions.from(viewModel) }
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
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        actions.onMessageConsumed()
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= PosBreakpoints.twoPane
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
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PosSpacing.md),
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                CashierCatalogPane(
                    state = state,
                    actions = actions,
                    modifier = Modifier.weight(1f),
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(horizontal = PosSpacing.md),
                )
                CashierCartBar(state = state, actions = actions)
            }
        }
        state.cartLineEditor?.let { editor ->
            CashierLineEditorSheet(editor = editor, actions = actions)
        }
        if (state.isCartSheetVisible) {
            CashierCartSheet(state = state, actions = actions)
        }
        if (state.isPaymentSheetVisible) {
            CashierPaymentSheet(state = state, actions = actions)
        }
        CashierCustomerPicker(state = state, actions = actions)
        CashierDueDatePicker(state = state, actions = actions)
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
    category = "Drinks",
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
    )
}

private fun previewCashierActions(): CashierActions = CashierActions(
    onSearchQueryChange = {},
    onRetryCatalog = {},
    onAddProduct = {},
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
    onOpenCustomerPicker = {},
    onCloseCustomerPicker = {},
    onCustomerSelected = {},
    onOpenDueDatePicker = {},
    onCloseDueDatePicker = {},
    onDueDateChange = {},
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
