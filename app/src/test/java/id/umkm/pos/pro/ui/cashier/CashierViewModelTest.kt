package id.umkm.pos.pro.ui.cashier

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.contract.StaffRepository
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.AppLanguage
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.ShiftSwitchResult
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.security.PinPolicy
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.domain.usecase.FakeClock
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.FakeCatalogRepository
import id.umkm.pos.pro.ui.FakeReceiptPrinter
import id.umkm.pos.pro.ui.FakeReceiptRenderer
import id.umkm.pos.pro.ui.FakeScannerInput
import id.umkm.pos.pro.ui.FakeSettingsRepository
import id.umkm.pos.pro.ui.FakeTransactionStore
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.staff.FakeStaffRepository
import id.umkm.pos.pro.ui.uiSampleProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
                uiSampleProduct(id = 1L, name = "Kopi Susu", basePrice = 2_000L, barcode = "8991234567890"),
                uiSampleProduct(id = 2L, name = "Teh Manis", basePrice = 3_000L),
            ),
        )
    }
    private val transactions = FakeTransactionStore()
    private val settings = FakeSettingsRepository()
    private val printer = FakeReceiptPrinter()
    private val renderer = FakeReceiptRenderer()
    private val scanner = FakeScannerInput()

    private fun createViewModel(
        settingsRepository: FakeSettingsRepository = settings,
        shiftRepository: ShiftRepository? = null,
        staffRepository: StaffRepository? = null,
    ): CashierViewModel = CashierViewModel(
        productRepository = products,
        settingsRepository = settingsRepository,
        priceEngine = DefaultPriceEngine(),
        checkoutUseCase = CheckoutUseCase(products, transactions, DefaultPriceEngine(), clock),
        receiptPrinter = printer,
        receiptRenderer = renderer,
        scannerInput = scanner,
        shiftRepository = shiftRepository,
        staffRepository = staffRepository,
    )

    @Test
    fun addingAProductBuildsTheCartAndTotal() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onAddProduct(1L)

        val state = viewModel.state.value
        assertThat(state.cartLines).hasSize(1)
        assertThat(state.cartLines.single().productName).isEqualTo("Kopi Susu")
        assertThat(state.grandTotal).isEqualTo(2_000L)
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("Kopi Susu ditambahkan")
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
    fun outOfStockAndOversoldProductsAreSortedBelowAvailableProducts() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Air Mineral", basePrice = 4_000L, stockBaseQty = 0.0),
                uiSampleProduct(id = 2L, name = "Beras 5kg", basePrice = 72_000L, stockBaseQty = -2.0),
                uiSampleProduct(id = 3L, name = "Es Teh Manis", basePrice = 5_000L, stockBaseQty = 20.0),
                uiSampleProduct(id = 4L, name = "Gula Pasir", basePrice = 15_000L, stockBaseQty = 3.0),
            ),
        )
        val viewModel = createViewModel()

        val names = viewModel.state.value.catalog.successDataOrNull().orEmpty().map { item -> item.product.name }
        assertThat(names).containsExactly("Es Teh Manis", "Gula Pasir", "Air Mineral", "Beras 5kg").inOrder()
    }

    @Test
    fun scanEventAddsTheMatchingUnit() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Indomie", stockBaseQty = 80.0),
            ),
        )
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

        assertThat(viewModel.state.value.message?.resolve(IdStrings))
            .isEqualTo("Tidak ada produk dengan kode 000000")
        assertThat(viewModel.state.value.cartLines).isEmpty()
    }

    @Test
    fun cashBelowTotalReportsValidationAndKeepsTheCart() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()

        viewModel.onAmountReceivedChange("1000")
        viewModel.onConfirmCharge()

        assertThat(viewModel.state.value.submitErrorMessage?.resolve(IdStrings))
            .isEqualTo("Jumlah diterima kurang dari total.")
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
        assertThat(receipt?.paymentMethod).isEqualTo(PaymentMethod.CASH)
        assertThat(receipt?.itemCount).isEqualTo(1)
        assertThat(state.cartLines).isEmpty()
        assertThat(state.isPaymentSheetVisible).isFalse()
        val shareText = receipt?.shareText.orEmpty()
        assertThat(shareText).contains("INV-")
        assertThat(shareText).contains("Rincian pesanan")
        assertThat(shareText).contains("Kopi Susu")
        assertThat(shareText).contains("TOTAL")
        assertThat(shareText).contains("Kembalian")
        assertThat(shareText).doesNotContain("Sisa utang")
        assertThat(transactions.savedTransactions().single().status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun englishLanguageSettingProducesAnEnglishShareText() = runTest(mainDispatcherRule.testDispatcher) {
        val englishSettings = FakeSettingsRepository(
            initialSettings = AppSettings(
                storeName = "Toko Uji",
                cashierName = null,
                taxRatePercent = 0,
                roundingIncrement = 0L,
                isPinEnabled = false,
                printerAddress = null,
                languageCode = AppLanguage.ENGLISH.code,
                qrisImagePath = null,
            ),
        )
        val viewModel = createViewModel(settingsRepository = englishSettings)
        viewModel.onAddProduct(1L)
        viewModel.onOpenPaymentSheet()
        viewModel.onAmountReceivedChange("5000")

        viewModel.onConfirmCharge()

        val shareText = viewModel.state.value.receipt?.shareText.orEmpty()
        assertThat(shareText).contains("Order details")
        assertThat(shareText).contains(EnStrings.receiptChange(Rupiah.format(3_000L)))
        assertThat(shareText).doesNotContain(IdStrings.receiptItemsTitle)
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
        assertThat(viewModel.state.value.receiptMessage?.resolve(IdStrings))
            .isEqualTo("Struk terkirim ke printer.")
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
    fun openingTheLineEditorExposesSubtotalAndUnitCostForThePinDecision() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAddProduct(1L)
            val lineId = viewModel.state.value.cartLines.single().lineId

            viewModel.onOpenLineEditor(lineId)

            val pricing = requireNotNull(viewModel.state.value.cartLineEditor?.pricing)
            assertThat(pricing.lineSubtotal).isEqualTo(2_000L)
            assertThat(pricing.unitHpp).isEqualTo(1_000L)
            assertThat(pricing.itemDiscount).isEqualTo(0L)
            assertThat(resolveLineEditorPinRequest(PinPolicy(), pricing)).isNull()
        }

    @Test
    fun editingTheDiscountRepricesTheEditorPreviewBeforeItIsApplied() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAddProduct(1L)
            val lineId = viewModel.state.value.cartLines.single().lineId
            viewModel.onOpenLineEditor(lineId)

            viewModel.onLineEditorDiscountChange("500")

            val pricing = requireNotNull(viewModel.state.value.cartLineEditor?.pricing)
            assertThat(pricing.itemDiscount).isEqualTo(500L)
            assertThat(viewModel.state.value.cartLines.single().itemDiscount).isEqualTo(0L)
            assertThat(resolveLineEditorPinRequest(PinPolicy(), pricing))
                .isEqualTo(LineEditorPinRequest.ITEM_DISCOUNT)

            viewModel.onApplyLineEditor()

            assertThat(viewModel.state.value.cartLines.single().itemDiscount).isEqualTo(500L)
            assertThat(viewModel.state.value.grandTotal).isEqualTo(1_500L)
        }

    @Test
    fun messageCanBeConsumed() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)

        viewModel.onMessageConsumed()

        assertThat(viewModel.state.value.message).isNull()
    }

    @Test
    fun addingOutOfStockProductIsBlockedAndSetsMessage() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu", basePrice = 2_000L, stockBaseQty = 0.0),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onAddProduct(1L)

        val state = viewModel.state.value
        assertThat(state.cartLines).isEmpty()
        assertThat(state.pricedCart).isNull()
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("Stok Kopi Susu habis")
    }

    @Test
    fun increasingQuantityBeyondStockBaseQtyIsBlockedAndSetsMessage() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu", basePrice = 2_000L, stockBaseQty = 1.0),
            ),
        )
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        val lineId = viewModel.state.value.cartLines.single().lineId

        viewModel.onIncreaseQuantity(lineId)

        val state = viewModel.state.value
        assertThat(state.cartLines.single().quantity).isEqualTo(1.0)
        assertThat(state.message?.resolve(IdStrings))
            .isEqualTo("Stok Kopi Susu tidak mencukupi (tersisa 1 pcs)")
    }

    @Test
    fun onClearCartEmptiesTheCartAndResetsPricedCart() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onAddProduct(1L)
        viewModel.onOpenCartSheet()

        viewModel.onClearCart()

        val state = viewModel.state.value
        assertThat(state.cartLines).isEmpty()
        assertThat(state.pricedCart).isNull()
        assertThat(state.isCartSheetVisible).isFalse()
    }

    @Test
    fun openingAndDismissingTheScanDialogTogglesTheState() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        assertThat(viewModel.state.value.isScanDialogVisible).isFalse()

        viewModel.onOpenScanDialog()
        assertThat(viewModel.state.value.isScanDialogVisible).isTrue()

        viewModel.onDismissScanDialog()
        assertThat(viewModel.state.value.isScanDialogVisible).isFalse()
    }

    @Test
    fun aCodeEnteredInTheScanDialogAddsTheProductAndClosesTheDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onOpenScanDialog()

            viewModel.onScannedCode("8991234567890")

            val state = viewModel.state.value
            assertThat(state.isScanDialogVisible).isFalse()
            assertThat(state.cartLines).hasSize(1)
            assertThat(state.cartLines.single().productName).isEqualTo("Kopi Susu")
        }

    @Test
    fun anUnknownCodeReportsNotFoundAndStillClosesTheDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onOpenScanDialog()

            viewModel.onScannedCode("0000000000000")

            val state = viewModel.state.value
            assertThat(state.isScanDialogVisible).isFalse()
            assertThat(state.cartLines).isEmpty()
            assertThat(state.message?.resolve(IdStrings)).isEqualTo("Tidak ada produk dengan kode 0000000000000")
        }

    @Test
    fun aHardwareScanClosesTheScanDialogAndAddsTheItem() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenScanDialog()

        scanner.emitScan("8991234567890")

        val state = viewModel.state.value
        assertThat(state.isScanDialogVisible).isFalse()
        assertThat(state.cartLines).hasSize(1)
    }

    @Test
    fun noShiftSetsShiftPromptVisibleAndConsumeClearsIt() = runTest(mainDispatcherRule.testDispatcher) {
        val shiftRepo = FakeCashierShiftRepository(initialShift = null)
        val viewModel = createViewModel(shiftRepository = shiftRepo)
        viewModel.onAddProduct(1L)
        viewModel.onMessageConsumed()

        viewModel.onOpenPaymentSheet()

        assertThat(viewModel.state.value.isShiftPromptVisible).isTrue()
        assertThat(viewModel.state.value.isPaymentSheetVisible).isFalse()
        assertThat(viewModel.state.value.message).isNull()

        viewModel.onShiftPromptConsumed()
        assertThat(viewModel.state.value.isShiftPromptVisible).isFalse()
    }

    @Test
    fun noShiftOnConfirmChargeSetsShiftPromptVisible() = runTest(mainDispatcherRule.testDispatcher) {
        val shiftRepo = FakeCashierShiftRepository(initialShift = null)
        val viewModel = createViewModel(shiftRepository = shiftRepo)
        viewModel.onAddProduct(1L)

        viewModel.onConfirmCharge()

        assertThat(viewModel.state.value.isShiftPromptVisible).isTrue()
        assertThat(viewModel.state.value.submitErrorMessage).isNull()
    }

    @Test
    fun afterShiftIsActiveOpenPaymentSheetOpensPayment() = runTest(mainDispatcherRule.testDispatcher) {
        val shift = CashierShift(
            id = 1L,
            cashierName = "Budi",
            openedAt = 1_000L,
            closedAt = null,
            startingCash = 50_000L,
            expectedEndingCash = null,
            actualEndingCash = null,
        )
        val shiftRepo = FakeCashierShiftRepository(initialShift = shift)
        val viewModel = createViewModel(shiftRepository = shiftRepo)
        viewModel.onAddProduct(1L)

        viewModel.onOpenPaymentSheet()

        assertThat(viewModel.state.value.isShiftPromptVisible).isFalse()
        assertThat(viewModel.state.value.isPaymentSheetVisible).isTrue()
    }

    @Test
    fun pendingPaymentAfterShiftAutoContinuesOnceWhenShiftEmits() = runTest(mainDispatcherRule.testDispatcher) {
        val shiftRepo = FakeCashierShiftRepository(initialShift = null)
        val viewModel = createViewModel(shiftRepository = shiftRepo)
        viewModel.onAddProduct(1L)

        // User taps payment without shift -> prompt shown
        viewModel.onOpenPaymentSheet()
        assertThat(viewModel.state.value.isShiftPromptVisible).isTrue()
        viewModel.onShiftPromptConsumed()

        // Shift sheet opened, but shift not yet emitted to repo
        viewModel.onShiftOpenedForPayment()
        assertThat(viewModel.state.value.isPaymentSheetVisible).isFalse()

        // Shift repo emits new active shift
        val newShift = CashierShift(
            id = 10L,
            cashierName = "Siti",
            openedAt = 2_000L,
            closedAt = null,
            startingCash = 100_000L,
            expectedEndingCash = null,
            actualEndingCash = null,
        )
        shiftRepo.activeShiftFlow.value = newShift
        testScheduler.advanceUntilIdle()

        // Pending payment should now have opened payment sheet automatically
        assertThat(viewModel.state.value.isPaymentSheetVisible).isTrue()

        // Close payment sheet and emit another shift update; should NOT auto-open again
        viewModel.onClosePaymentSheet()
        assertThat(viewModel.state.value.isPaymentSheetVisible).isFalse()
        shiftRepo.activeShiftFlow.value = newShift.copy(startingCash = 150_000L)
        testScheduler.advanceUntilIdle()
        assertThat(viewModel.state.value.isPaymentSheetVisible).isFalse()
    }

    @Test
    fun callingOnShiftOpenedForPaymentWhenShiftAlreadyActiveOpensPaymentImmediately() =
        runTest(mainDispatcherRule.testDispatcher) {
            val shift = CashierShift(
                id = 1L,
                cashierName = "Dini",
                openedAt = 1_000L,
                closedAt = null,
                startingCash = 50_000L,
                expectedEndingCash = null,
                actualEndingCash = null,
            )
            val shiftRepo = FakeCashierShiftRepository(initialShift = shift)
            val viewModel = createViewModel(shiftRepository = shiftRepo)
            viewModel.onAddProduct(1L)

            viewModel.onShiftOpenedForPayment()

            assertThat(viewModel.state.value.isPaymentSheetVisible).isTrue()
        }

    @Test
    fun shiftWithNoActiveStaffAllowsPaymentAndUsesShiftCashierName() = runTest(mainDispatcherRule.testDispatcher) {
        val shift = CashierShift(
            id = 5L,
            cashierName = "ShiftCashier",
            openedAt = 1_000L,
            closedAt = null,
            startingCash = 50_000L,
            expectedEndingCash = null,
            actualEndingCash = null,
        )
        val shiftRepo = FakeCashierShiftRepository(initialShift = shift)
        val staffRepo = FakeStaffRepository(emptyList())
        val viewModel = createViewModel(shiftRepository = shiftRepo, staffRepository = staffRepo)
        viewModel.onAddProduct(1L)

        viewModel.onOpenPaymentSheet()
        assertThat(viewModel.state.value.isPaymentSheetVisible).isTrue()

        viewModel.onAmountReceivedChange("2000")
        viewModel.onConfirmCharge()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.submitErrorMessage).isNull()
        assertThat(state.receipt).isNotNull()
        assertThat(transactions.savedTransactions()).hasSize(1)
        val record = transactions.savedTransactions().single()
        assertThat(record.cashierName).isEqualTo("ShiftCashier")
        assertThat(record.shiftId).isEqualTo(5L)
        assertThat(record.staffId).isNull()
    }

    private class FakeCashierShiftRepository(
        initialShift: CashierShift? = null,
    ) : ShiftRepository {
        val activeShiftFlow = MutableStateFlow<CashierShift?>(initialShift)

        override fun observeActiveShift(): Flow<CashierShift?> = activeShiftFlow

        override suspend fun getActiveShift(): CashierShift? = activeShiftFlow.value

        override suspend fun getRecentCashierNames(limit: Int): List<String> =
            listOfNotNull(activeShiftFlow.value?.cashierName)

        override suspend fun getShiftHistory(limit: Int): List<CashierShift> =
            listOfNotNull(activeShiftFlow.value)

        override suspend fun getShiftSummary(shiftId: Long): AppResult<ShiftCashSummary> =
            AppResult.Failure(AppError.NotFound("shift not found"))

        override suspend fun openShift(staffId: Long, startingCash: Long): AppResult<CashierShift> =
            AppResult.Failure(AppError.NotFound("not implemented"))

        override suspend fun closeShift(shiftId: Long, actualEndingCash: Long): AppResult<CashierShift> =
            AppResult.Failure(AppError.NotFound("not implemented"))

        override suspend fun switchShift(
            closeShiftId: Long,
            actualEndingCash: Long,
            nextStaffId: Long,
            nextStartingCash: Long,
        ): AppResult<ShiftSwitchResult> =
            AppResult.Failure(AppError.NotFound("not implemented"))

        override suspend fun switchOperator(staffId: Long): AppResult<Staff> =
            AppResult.Failure(AppError.NotFound("not implemented"))
    }
}
