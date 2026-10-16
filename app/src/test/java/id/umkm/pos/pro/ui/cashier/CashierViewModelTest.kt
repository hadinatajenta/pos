package id.umkm.pos.pro.ui.cashier

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.domain.usecase.FakeClock
import id.umkm.pos.pro.ui.FakeCatalogRepository
import id.umkm.pos.pro.ui.FakeCustomerStore
import id.umkm.pos.pro.ui.FakeReceiptPrinter
import id.umkm.pos.pro.ui.FakeReceiptRenderer
import id.umkm.pos.pro.ui.FakeScannerInput
import id.umkm.pos.pro.ui.FakeSettingsRepository
import id.umkm.pos.pro.ui.FakeTransactionStore
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.uiSampleCustomer
import id.umkm.pos.pro.ui.uiSampleProduct
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CashierViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock(currentMillis = 5_000L)
    private val products = FakeCatalogRepository().apply {
        setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu", basePrice = 2_000L),
                uiSampleProduct(id = 2L, name = "Teh Manis", basePrice = 3_000L),
            ),
        )
    }
    private val customers = FakeCustomerStore()
    private val transactions = FakeTransactionStore()
    private val settings = FakeSettingsRepository()
    private val printer = FakeReceiptPrinter()
    private val renderer = FakeReceiptRenderer()
    private val scanner = FakeScannerInput()

    private fun createViewModel(): CashierViewModel = CashierViewModel(
        productRepository = products,
        customerRepository = customers,
        settingsRepository = settings,
        priceEngine = DefaultPriceEngine(),
        checkoutUseCase = CheckoutUseCase(products, transactions, DefaultPriceEngine(), clock),
        receiptPrinter = printer,
        receiptRenderer = renderer,
        scannerInput = scanner,
    )

    @Test
    fun addingAProductBuildsTheCartAndTotal() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onAddProduct(1L)

        val state = viewModel.state.value
        assertThat(state.cartLines).hasSize(1)
        assertThat(state.cartLines.single().productName).isEqualTo("Kopi Susu")
        assertThat(state.grandTotal).isEqualTo(2_000L)
        assertThat(state.message).isEqualTo("Kopi Susu ditambahkan")
    }

    @Test
    fun addingTheSameProductTwiceMergesTheQuantity() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onAddProduct(1L)
        viewModel.onAddProduct(1L)

        val state = viewModel.state.value
        assertThat(state.cartLines).hasSize(1)
        assertThat(state.cartLines.single().quantity).isEqualTo(2.0)
        assertThat(state.grandTotal).isEqualTo(4_000L)
    }

    @Test
    fun increasingQuantityRecomputesTheTotal() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        val lineId = viewModel.state.value.cartLines.single().lineId

        viewModel.onIncreaseQuantity(lineId)

        assertThat(viewModel.state.value.grandTotal).isEqualTo(4_000L)
    }

    @Test
    fun decreasingQuantityToZeroRemovesTheLine() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        val lineId = viewModel.state.value.cartLines.single().lineId

        viewModel.onDecreaseQuantity(lineId)

        assertThat(viewModel.state.value.cartLines).isEmpty()
        assertThat(viewModel.state.value.pricedCart).isNull()
    }

    @Test
    fun searchQueryFiltersTheCatalog() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Teh")

        val catalog = viewModel.state.value.catalog
        assertThat(catalog).isInstanceOf(UiState.Success::class.java)
        val items = catalog.successDataOrNull().orEmpty()
        assertThat(items.map { it.product.name }).containsExactly("Teh Manis")
    }

    @Test
    fun addingAProductExposesItsCartQuantityOnTheCatalogItem() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onAddProduct(1L)

        val items = viewModel.state.value.catalog.successDataOrNull().orEmpty()
        assertThat(items.single { item -> item.product.id == 1L }.quantityInCart).isEqualTo(1.0)
        assertThat(items.single { item -> item.product.id == 2L }.quantityInCart).isEqualTo(0.0)
    }

    @Test
    fun decreasingTheCatalogQuantityRemovesTheProductFromTheCart() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAddProduct(1L)

            viewModel.onDecreaseProductQuantity(1L)

            assertThat(viewModel.state.value.cartLines).isEmpty()
            val quantityAfterDecrease = viewModel.state.value.catalog.successDataOrNull().orEmpty()
                .single { item -> item.product.id == 1L }
                .quantityInCart
            assertThat(quantityAfterDecrease).isEqualTo(0.0)
        }

    @Test
    fun catalogCategoryFilterNarrowsTheVisibleProducts() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onCatalogCategoryChange("Makanan")
        assertThat(viewModel.state.value.catalog).isEqualTo(UiState.Empty)

        viewModel.onCatalogCategoryChange(null)
        assertThat(viewModel.state.value.catalog.successDataOrNull().orEmpty()).hasSize(2)
    }

    @Test
    fun availabilityFilterHidesOutOfStockProducts() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu", basePrice = 2_000L),
                uiSampleProduct(id = 2L, name = "Teh Manis", basePrice = 3_000L, stockBaseQty = 0.0),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onCatalogAvailableOnlyChange(true)

        val names = viewModel.state.value.catalog.successDataOrNull().orEmpty().map { item -> item.product.name }
        assertThat(names).containsExactly("Kopi Susu")
    }

    @Test
    fun scanEventAddsTheMatchingUnit() = runTest(mainDispatcherRule.testDispatcher) {
        products.setUnits(
            1L,
            listOf(
                ProductUnit(
                    id = 10L,
                    productId = 1L,
                    variantName = "Dus",
                    conversionFactor = 40.0,
                    customBarcode = "899900",
                    unitPrice = 78_000L,
                    wholesaleMinQty = null,
                    wholesalePrice = null,
                ),
            ),
        )
        val viewModel = createViewModel()

        scanner.emitScan("899900")

        val line = viewModel.state.value.cartLines.single()
        assertThat(line.unitLabel).isEqualTo("Dus")
        assertThat(line.unitPrice).isEqualTo(78_000L)
        assertThat(viewModel.state.value.grandTotal).isEqualTo(78_000L)
    }

    @Test
    fun unknownScanCodeReportsAMessage() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        scanner.emitScan("000000")

        assertThat(viewModel.state.value.message).isEqualTo("Tidak ada produk dengan kode 000000")
        assertThat(viewModel.state.value.cartLines).isEmpty()
    }

    @Test
    fun cashBelowTotalReportsValidationAndKeepsTheCart() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()

        viewModel.onAmountReceivedChange("1000")
        viewModel.onConfirmCharge()

        assertThat(viewModel.state.value.submitErrorMessage).isEqualTo("Jumlah diterima kurang dari total.")
        assertThat(viewModel.state.value.cartLines).hasSize(1)
        assertThat(transactions.savedTransactions()).isEmpty()
    }

    @Test
    fun cashSaleCompletesAndBuildsAReceipt() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()
        viewModel.onAmountReceivedChange("5000")

        viewModel.onConfirmCharge()

        val state = viewModel.state.value
        val receipt = state.receipt
        assertThat(receipt).isNotNull()
        assertThat(receipt?.grandTotal).isEqualTo(2_000L)
        assertThat(receipt?.paidAmount).isEqualTo(5_000L)
        assertThat(receipt?.changeAmount).isEqualTo(3_000L)
        assertThat(receipt?.remainingDebt).isEqualTo(0L)
        assertThat(state.cartLines).isEmpty()
        assertThat(state.isPaymentSheetVisible).isFalse()
        assertThat(renderer.lastWidthChars).isEqualTo(32)
        assertThat(transactions.savedTransactions().single().status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun kasbonWithoutACustomerIsRejected() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()

        viewModel.onPaymentMethodChange(PaymentMethod.KASBON)
        viewModel.onConfirmCharge()

        assertThat(viewModel.state.value.submitErrorMessage)
            .isEqualTo("Pilih pelanggan untuk sisa tagihan.")
        assertThat(transactions.savedTransactions()).isEmpty()
    }

    @Test
    fun kasbonWithACustomerCreatesPartialDebtWithADueDate() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(listOf(uiSampleCustomer(id = 7L, name = "Pak Budi", totalDebt = 0L)))
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()
        viewModel.onPaymentMethodChange(PaymentMethod.KASBON)
        viewModel.onCustomerSelected(7L)
        viewModel.onDueDateChange(50_000L)

        viewModel.onConfirmCharge()

        val state = viewModel.state.value
        assertThat(state.receipt?.remainingDebt).isEqualTo(2_000L)
        val saved = transactions.savedTransactions().single()
        assertThat(saved.customerId).isEqualTo(7L)
        assertThat(saved.dueDate).isEqualTo(50_000L)
        assertThat(saved.status).isEqualTo(TransactionStatus.PARTIAL_DEBT)
    }

    @Test
    fun printingTheReceiptSendsTheDocumentToThePrinter() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()
        viewModel.onAmountReceivedChange("5000")
        viewModel.onConfirmCharge()

        viewModel.onPrintReceipt()

        assertThat(printer.lastPrintedDocument).isNotNull()
        assertThat(printer.lastPrintedDocument?.storeName).isEqualTo("Toko Uji")
        assertThat(viewModel.state.value.receiptMessage).isEqualTo("Struk terkirim ke printer.")
    }

    @Test
    fun startingANewSaleClearsTheReceipt() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()
        viewModel.onAmountReceivedChange("5000")
        viewModel.onConfirmCharge()

        viewModel.onNewSale()

        assertThat(viewModel.state.value.receipt).isNull()
    }

    @Test
    fun lineEditorAppliesANegotiatedPrice() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        val lineId = viewModel.state.value.cartLines.single().lineId

        viewModel.onOpenLineEditor(lineId)
        viewModel.onLineEditorPriceChange("1500")
        viewModel.onApplyLineEditor()

        val line = viewModel.state.value.cartLines.single()
        assertThat(line.unitPrice).isEqualTo(1_500L)
        assertThat(line.isNegotiated).isTrue()
        assertThat(viewModel.state.value.cartLineEditor).isNull()
    }

    @Test
    fun messageCanBeConsumed() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)

        viewModel.onMessageConsumed()

        assertThat(viewModel.state.value.message).isNull()
    }
}
