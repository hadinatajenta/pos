package id.umkm.pos.pro.ui.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReceiptAlignment
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptLine
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ScannerInput
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.contract.StaffRepository
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.AppLanguage
import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.PricedCartLine
import id.umkm.pos.pro.core.model.PricingContext
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.domain.usecase.CheckoutLine
import id.umkm.pos.pro.domain.usecase.CheckoutRequest
import id.umkm.pos.pro.domain.usecase.CheckoutResult
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.common.stringsFor
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.common.toAppError
import id.umkm.pos.pro.ui.common.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import id.umkm.pos.pro.feature.receipt.ReceiptShareData
import id.umkm.pos.pro.feature.receipt.ReceiptShareItem
import id.umkm.pos.pro.feature.receipt.ReceiptShareText
import id.umkm.pos.pro.feature.receipt.twoColumnLine
import id.umkm.pos.pro.ui.common.formatDateTime
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.app.SystemClock
import id.umkm.pos.pro.core.contract.Clock

private sealed interface PaidAmountResolution {
    data class Resolved(val paidAmount: Long) : PaidAmountResolution

    data class Rejected(val message: UiText) : PaidAmountResolution
}

@OptIn(ExperimentalCoroutinesApi::class)
class CashierViewModel(
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository,
    private val priceEngine: PriceEngine,
    private val checkoutUseCase: CheckoutUseCase,
    private val receiptPrinter: ReceiptPrinter,
    private val receiptRenderer: ReceiptRenderer,
    private val scannerInput: ScannerInput,
    private val shiftRepository: ShiftRepository? = null,
    private val staffRepository: StaffRepository? = null,
    private val clock: Clock = SystemClock,
) : ViewModel() {

    private val _state = MutableStateFlow(CashierUiState())
    val state: StateFlow<CashierUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val reloadTrigger = MutableStateFlow(0)
    private val catalogFilter = MutableStateFlow(CatalogFilter())
    private var catalogSource: UiState<List<Product>> = UiState.Loading
    private var catalogCategories: List<String> = emptyList()
    private val cartDrafts = MutableStateFlow<List<CartLineDraft>>(emptyList())
    private val productCache = mutableMapOf<Long, Product>()
    private val unitsCache = mutableMapOf<Long, List<ProductUnit>>()

    private var pricingContext = PricingContext()
    private var storeName = ""
    private var fallbackCashierName: String? = null
    private var currentStrings: PosStrings = IdStrings
    private var nextLineNumber = 1
    private var pendingReceiptDocument: ReceiptDocument? = null
    private var pendingPaymentAfterShift = false

    init {
        observeSettings()
        observeCatalog()
        observeScanEvents()
        observeActiveShift()
        observeActiveStaff()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun onCatalogCategoryChange(category: String?) {
        catalogFilter.update { filter -> filter.copy(category = category) }
        publishCatalog()
    }

    fun onCatalogAvailableOnlyChange(isAvailableOnly: Boolean) {
        catalogFilter.update { filter -> filter.copy(isAvailableOnly = isAvailableOnly) }
        publishCatalog()
    }

    fun onRetryCatalog() {
        reloadTrigger.update { it + 1 }
    }

    fun onAddProduct(productId: Long) {
        viewModelScope.launch { addProductToCart(productId, null) }
    }

    fun onDecreaseProductQuantity(productId: Long) {
        val draft = cartDrafts.value.firstOrNull { line ->
            line.productId == productId && line.unitId == null
        } ?: return
        onDecreaseQuantity(draft.lineId)
    }

    fun onIncreaseQuantity(lineId: String) {
        val target = cartDrafts.value.firstOrNull { it.lineId == lineId } ?: return
        val product = productCache[target.productId]
        if (product != null) {
            val addedBaseQty = QUANTITY_STEP * conversionFactorFor(target.productId, target.unitId)
            val projectedBaseQty = totalBaseQtyInCart(target.productId) + addedBaseQty
            if (projectedBaseQty > product.stockBaseQty) {
                setMessage(
                    UiText.from { strings ->
                        strings.cashierStockInsufficientNamed(
                            product.name,
                            formatQuantity(product.stockBaseQty),
                            product.baseUnit,
                        )
                    },
                )
                return
            }
        }
        cartDrafts.value = cartDrafts.value.map { draft ->
            if (draft.lineId == lineId) draft.copy(quantity = draft.quantity + QUANTITY_STEP) else draft
        }
        refreshCart()
    }

    fun onDecreaseQuantity(lineId: String) {
        val target = cartDrafts.value.firstOrNull { it.lineId == lineId } ?: return
        if (target.quantity - QUANTITY_STEP <= 0.0) {
            onRemoveLine(lineId)
            return
        }
        cartDrafts.value = cartDrafts.value.map { draft ->
            if (draft.lineId == lineId) draft.copy(quantity = draft.quantity - QUANTITY_STEP) else draft
        }
        refreshCart()
    }

    fun onRemoveLine(lineId: String) {
        cartDrafts.value = cartDrafts.value.filterNot { it.lineId == lineId }
        _state.update { current ->
            if (current.cartLineEditor?.lineId == lineId) current.copy(cartLineEditor = null) else current
        }
        refreshCart()
    }

    fun onClearCart() {
        cartDrafts.value = emptyList()
        _state.update { it.copy(isCartSheetVisible = false, cartLineEditor = null) }
        refreshCart()
    }

    fun onOpenScanDialog() {
        _state.update { it.copy(isScanDialogVisible = true) }
    }

    fun onDismissScanDialog() {
        _state.update { it.copy(isScanDialogVisible = false) }
    }

    fun onScannedCode(code: String) {
        _state.update { it.copy(isScanDialogVisible = false) }
        viewModelScope.launch { addScannedCode(code) }
    }

    fun onOpenCartSheet() {
        _state.update { it.copy(isCartSheetVisible = true) }
    }

    fun onCloseCartSheet() {
        _state.update { it.copy(isCartSheetVisible = false) }
    }

    fun onOpenLineEditor(lineId: String) {
        val draft = cartDrafts.value.firstOrNull { it.lineId == lineId } ?: return
        val product = productCache[draft.productId] ?: return
        _state.update { current ->
            current.copy(
                cartLineEditor = lineEditorState(
                    draft = draft,
                    product = product,
                    selectedUnitId = draft.unitId,
                    negotiatedUnitPriceInput = draft.negotiatedUnitPrice?.toString().orEmpty(),
                    itemDiscountInput = draft.itemDiscount.takeIf { it > 0L }?.toString().orEmpty(),
                    validationMessage = null,
                ),
            )
        }
    }

    fun onCloseLineEditor() {
        _state.update { it.copy(cartLineEditor = null) }
    }

    fun onLineEditorUnitChange(unitId: Long?) {
        updateLineEditor { editor -> editor.copy(selectedUnitId = unitId, validationMessage = null) }
    }

    fun onLineEditorPriceChange(value: String) {
        val digits = value.filter { it.isDigit() }
        updateLineEditor { editor ->
            editor.copy(negotiatedUnitPriceInput = digits, validationMessage = null)
        }
    }

    fun onLineEditorDiscountChange(value: String) {
        val digits = value.filter { it.isDigit() }
        updateLineEditor { editor ->
            editor.copy(itemDiscountInput = digits, validationMessage = null)
        }
    }

    fun onApplyLineEditor() {
        val editor = _state.value.cartLineEditor ?: return
        val priceInput = editor.negotiatedUnitPriceInput.trim()
        val negotiatedUnitPrice = if (priceInput.isEmpty()) null else priceInput.toLongOrNull()
        if (priceInput.isNotEmpty() && negotiatedUnitPrice == null) {
            updateLineEditorValidation(UiText.from { strings -> strings.cashierInvalidNegotiatedPrice })
            return
        }
        val discountInput = editor.itemDiscountInput.trim()
        val itemDiscount = if (discountInput.isEmpty()) 0L else discountInput.toLongOrNull()
        if (itemDiscount == null) {
            updateLineEditorValidation(UiText.from { strings -> strings.cashierInvalidItemDiscount })
            return
        }
        val target = cartDrafts.value.firstOrNull { it.lineId == editor.lineId } ?: return
        val product = productCache[target.productId]
        if (product != null) {
            val requiredBaseQty = totalBaseQtyInCart(target.productId, excludingLineId = target.lineId) +
                (target.quantity * conversionFactorFor(target.productId, editor.selectedUnitId))
            if (requiredBaseQty > product.stockBaseQty) {
                updateLineEditorValidation(
                    UiText.from { strings ->
                        strings.cashierStockInsufficientShort(
                            formatQuantity(product.stockBaseQty),
                            product.baseUnit,
                        )
                    },
                )
                return
            }
        }
        cartDrafts.value = cartDrafts.value.map { draft ->
            if (draft.lineId == editor.lineId) {
                draft.copy(
                    unitId = editor.selectedUnitId,
                    negotiatedUnitPrice = negotiatedUnitPrice,
                    itemDiscount = itemDiscount,
                )
            } else {
                draft
            }
        }
        _state.update { it.copy(cartLineEditor = null) }
        refreshCart()
    }

    fun onOpenPaymentSheet() {
        if (cartDrafts.value.isEmpty()) return
        if (shiftRepository != null && _state.value.activeShift == null) {
            _state.update { it.copy(isShiftPromptVisible = true, submitErrorMessage = null) }
            return
        }
        _state.update {
            it.copy(
                isPaymentSheetVisible = true,
                isCartSheetVisible = true,
                paymentDraft = PaymentDraft(),
                submitErrorMessage = null,
            )
        }
    }

    fun onShiftPromptConsumed() {
        _state.update { it.copy(isShiftPromptVisible = false) }
    }

    fun onShiftOpenedForPayment() {
        if (_state.value.activeShift != null) {
            onOpenPaymentSheet()
        } else {
            pendingPaymentAfterShift = true
        }
    }

    fun onClosePaymentSheet() {
        _state.update { it.copy(isPaymentSheetVisible = false, submitErrorMessage = null) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _state.update { current ->
            current.copy(
                paymentDraft = PaymentDraft(method = method),
                submitErrorMessage = null,
            )
        }
    }

    fun onAmountReceivedChange(value: String) {
        val digits = value.filter { it.isDigit() }
        _state.update { current ->
            current.copy(
                paymentDraft = current.paymentDraft.copy(amountReceivedInput = digits),
                submitErrorMessage = null,
            )
        }
    }

    fun onConfirmCharge() {
        val pricedCart = _state.value.pricedCart ?: return
        val drafts = cartDrafts.value
        if (drafts.isEmpty() || _state.value.isSubmitting) return
        if (shiftRepository != null && _state.value.activeShift == null) {
            _state.update { it.copy(isShiftPromptVisible = true, submitErrorMessage = null) }
            return
        }

        val draft = _state.value.paymentDraft
        val paidAmount = when (val resolution = resolvePaidAmount(draft, pricedCart.grandTotal)) {
            is PaidAmountResolution.Rejected -> {
                _state.update { it.copy(submitErrorMessage = resolution.message) }
                return
            }

            is PaidAmountResolution.Resolved -> resolution.paidAmount
        }
        val request = CheckoutRequest(
            lines = drafts.map { cartDraft ->
                CheckoutLine(
                    productId = cartDraft.productId,
                    unitId = cartDraft.unitId,
                    qty = cartDraft.quantity,
                    negotiatedUnitPrice = cartDraft.negotiatedUnitPrice,
                    itemDiscount = cartDraft.itemDiscount,
                )
            },
            paymentMethod = draft.method,
            paidAmount = paidAmount,
            invoiceDiscount = 0L,
            taxRatePercent = pricingContext.taxRatePercent,
            roundingIncrement = pricingContext.roundingIncrement,
            notes = null,
            cashierName = _state.value.activeStaff?.name
                ?: _state.value.activeShift?.cashierName
                ?: fallbackCashierName,
            shiftId = _state.value.activeShift?.id,
            staffId = _state.value.activeStaff?.id,
        )

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitErrorMessage = null) }
            when (val result = checkoutUseCase(request)) {
                is AppResult.Success -> onCheckoutCompleted(result.value)
                is AppResult.Failure -> _state.update { current ->
                    current.copy(isSubmitting = false, submitErrorMessage = result.error.toUiText())
                }
            }
        }
    }

    fun onPrintReceipt() {
        val document = pendingReceiptDocument ?: return
        if (_state.value.isPrintingReceipt) return
        viewModelScope.launch {
            _state.update { it.copy(isPrintingReceipt = true, receiptMessage = null) }
            when (val result = receiptPrinter.print(document)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        isPrintingReceipt = false,
                        receiptMessage = UiText.from { strings -> strings.cashierReceiptSent },
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isPrintingReceipt = false, receiptMessage = result.error.toUiText())
                }
            }
        }
    }

    fun onNewSale() {
        pendingReceiptDocument = null
        _state.update { it.copy(receipt = null, isPrintingReceipt = false, receiptMessage = null) }
    }

    fun onMessageConsumed() {
        _state.update { it.copy(message = null, shouldSuppressMessageSnackbar = false) }
    }

    private fun setMessage(message: UiText, shouldSuppressSnackbar: Boolean = false) {
        _state.update {
            it.copy(message = message, shouldSuppressMessageSnackbar = shouldSuppressSnackbar)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                storeName = settings.storeName
                fallbackCashierName = settings.cashierName
                currentStrings = stringsFor(AppLanguage.fromCode(settings.languageCode))
                pricingContext = PricingContext(
                    invoiceDiscount = 0L,
                    taxRatePercent = settings.taxRatePercent,
                    roundingIncrement = settings.roundingIncrement,
                )
                _state.update { current -> current.copy(qrisPayload = settings.qrisPayload) }
                refreshCart()
            }
        }
    }

    private fun observeCatalog() {
        viewModelScope.launch {
            combine(searchQuery, reloadTrigger) { query, _ -> query }
                .flatMapLatest { query ->
                    productRepository.observeProducts(query.ifBlank { null })
                        .map<List<Product>, UiState<List<Product>>> { products ->
                            if (products.isEmpty()) UiState.Empty else UiState.Success(products)
                        }
                        .catch { error -> emit(UiState.Error(error.toAppError())) }
                }
                .collect { catalogState ->
                    catalogState.successDataOrNull()?.forEach { product -> productCache[product.id] = product }
                    catalogSource = catalogState
                    catalogCategories = catalogState.successDataOrNull().orEmpty()
                        .mapNotNull { product -> product.category }
                        .distinct()
                        .sorted()
                    publishCatalog()
                    refreshCart()
                }
        }
    }

    private fun observeScanEvents() {
        viewModelScope.launch {
            scannerInput.scanEvents.collect { scanEvent -> onScannedCode(scanEvent.code) }
        }
    }

    private fun observeActiveShift() {
        val repository = shiftRepository ?: return
        viewModelScope.launch {
            repository.observeActiveShift().collect { shift ->
                _state.update { current -> current.copy(activeShift = shift) }
                if (shift != null && pendingPaymentAfterShift) {
                    pendingPaymentAfterShift = false
                    onOpenPaymentSheet()
                }
            }
        }
    }

    private fun observeActiveStaff() {
        val repository = staffRepository ?: return
        viewModelScope.launch {
            repository.observeActiveStaff().collect { staff ->
                _state.update { current -> current.copy(activeStaff = staff) }
            }
        }
    }

    private suspend fun addScannedCode(code: String) {
        val unit = productRepository.findUnitByBarcode(code)
        if (unit != null) {
            addProductToCart(unit.productId, unit.id)
            return
        }
        val product = productRepository.findProductByBarcode(code)
        if (product != null) {
            addProductToCart(product.id, null)
            return
        }
        _state.update { it.copy(message = UiText.from { strings -> strings.cashierBarcodeNotFound(code) }) }
    }

    private suspend fun addProductToCart(productId: Long, unitId: Long?) {
        val product = productCache[productId] ?: productRepository.getProductById(productId)
        if (product == null) {
            setMessage(UiText.from { strings -> strings.cashierProductNotFound })
            return
        }
        productCache[productId] = product
        if (!unitsCache.containsKey(productId)) {
            unitsCache[productId] = productRepository.getUnits(productId)
        }
        val addedBaseQty = QUANTITY_STEP * conversionFactorFor(productId, unitId)
        val projectedBaseQty = totalBaseQtyInCart(productId) + addedBaseQty
        if (product.stockBaseQty <= 0.0) {
            setMessage(UiText.from { strings -> strings.cashierStockEmptyNamed(product.name) })
            return
        }
        if (projectedBaseQty > product.stockBaseQty) {
            setMessage(
                UiText.from { strings ->
                    strings.cashierStockInsufficientNamed(
                        product.name,
                        formatQuantity(product.stockBaseQty),
                        product.baseUnit,
                    )
                },
            )
            return
        }
        val existingDraft = cartDrafts.value.firstOrNull { draft ->
            draft.productId == productId && draft.unitId == unitId && draft.negotiatedUnitPrice == null
        }
        cartDrafts.value = if (existingDraft != null) {
            cartDrafts.value.map { draft ->
                if (draft.lineId == existingDraft.lineId) {
                    draft.copy(quantity = draft.quantity + QUANTITY_STEP)
                } else {
                    draft
                }
            }
        } else {
            cartDrafts.value + CartLineDraft(
                lineId = "D${nextLineNumber++}",
                productId = productId,
                unitId = unitId,
                quantity = QUANTITY_STEP,
                negotiatedUnitPrice = null,
                itemDiscount = 0L,
            )
        }
        setMessage(
            UiText.from { strings -> strings.cashierProductAdded(product.name) },
            shouldSuppressSnackbar = true,
        )
        refreshCart()
    }
    private fun conversionFactorFor(productId: Long, unitId: Long?): Double {
        if (unitId == null) return 1.0
        return unitsCache[productId]?.firstOrNull { it.id == unitId }?.conversionFactor ?: 1.0
    }

    private fun totalBaseQtyInCart(productId: Long, excludingLineId: String? = null): Double =
        cartDrafts.value
            .filter { it.productId == productId && it.lineId != excludingLineId }
            .sumOf { it.quantity * conversionFactorFor(it.productId, it.unitId) }

    private fun publishCatalog() {
        val filter = catalogFilter.value
        val filteredItems = catalogSource.successDataOrNull().orEmpty()
            .map { product ->
                CashierProductItem(
                    product = product,
                    stockStatus = resolveStockStatus(product),
                    quantityInCart = quantityInCart(product.id),
                )
            }
            .filter { item -> filter.matches(item) }
            .sortedBy { item ->
                item.stockStatus == id.umkm.pos.pro.ui.common.StockStatus.OUT_OF_STOCK ||
                    item.stockStatus == id.umkm.pos.pro.ui.common.StockStatus.OVERSOLD
            }
        val publishedCatalog: UiState<List<CashierProductItem>> = when (val source = catalogSource) {
            is UiState.Success -> if (filteredItems.isEmpty()) UiState.Empty else UiState.Success(filteredItems)
            is UiState.Empty -> UiState.Empty
            is UiState.Error -> source
            is UiState.Loading -> UiState.Loading
        }
        _state.update { current ->
            current.copy(
                catalog = publishedCatalog,
                catalogFilter = filter,
                catalogCategories = catalogCategories,
            )
        }
    }

    private fun quantityInCart(productId: Long): Double =
        cartDrafts.value
            .filter { draft -> draft.productId == productId && draft.unitId == null }
            .sumOf { draft -> draft.quantity }

    private fun refreshCart() {
        updateCartState()
        publishCatalog()
    }

    private fun updateCartState() {
        val drafts = cartDrafts.value
        if (drafts.isEmpty()) {
            _state.update { it.copy(cartLines = emptyList(), pricedCart = null) }
            return
        }
        val resolvedProducts = LinkedHashMap<Long, Product>()
        for (draft in drafts) {
            val product = productCache[draft.productId]
            if (product == null) {
                return
            }
            resolvedProducts[draft.productId] = product
        }
        val cartLines = drafts.map { draft ->
            CartLine(
                lineId = draft.lineId,
                productId = draft.productId,
                unitId = draft.unitId,
                variantName = variantNameFor(draft),
                qty = draft.quantity,
                negotiatedUnitPrice = draft.negotiatedUnitPrice,
                itemDiscount = draft.itemDiscount,
            )
        }
        val pricedCart = priceEngine.priceCart(cartLines, resolvedProducts, unitsCache.toMap(), pricingContext)
        _state.update { current ->
            current.copy(
                cartLines = pricedCart.lines.map { pricedLine -> pricedLine.toCartLine() },
                pricedCart = pricedCart,
            )
        }
    }

    private fun variantNameFor(draft: CartLineDraft): String? = draft.unitId?.let { unitId ->
        unitsCache[draft.productId]?.firstOrNull { unit -> unit.id == unitId }?.variantName
    }

    private fun PricedCartLine.toCartLine(): CashierCartLine = CashierCartLine(
        lineId = line.lineId,
        productId = product.id,
        unitId = line.unitId,
        productName = product.name,
        variantName = line.variantName,
        unitLabel = line.variantName ?: product.baseUnit,
        quantity = line.qty,
        unitPrice = unitPrice,
        itemDiscount = line.itemDiscount,
        lineSubtotal = itemSubtotal,
        isNegotiated = isNegotiated,
        isWholesale = isWholesale,
        categoryName = product.category.orEmpty(),
        imagePath = product.imagePath,
        icon = product.icon,
        stock = product.stockBaseQty,
    )

    private fun resolvePaidAmount(draft: PaymentDraft, grandTotal: Long): PaidAmountResolution = when (draft.method) {
        PaymentMethod.CASH -> {
            val tendered = draft.amountReceivedInput.toLongOrNull()
            when {
                tendered == null -> PaidAmountResolution.Rejected(
                    UiText.from { strings -> strings.cashierEnterAmountReceived },
                )

                tendered < grandTotal -> PaidAmountResolution.Rejected(
                    UiText.from { strings -> strings.cashierAmountLessThanTotal },
                )

                else -> PaidAmountResolution.Resolved(tendered)
            }
        }

        PaymentMethod.QRIS, PaymentMethod.TRANSFER, PaymentMethod.DEBIT, PaymentMethod.CREDIT ->
            PaidAmountResolution.Resolved(grandTotal)
    }

    private fun onCheckoutCompleted(result: CheckoutResult) {
        cartDrafts.value = emptyList()
        val document = buildReceiptDocument(result)
        pendingReceiptDocument = document
        val receipt = CashierReceipt(
            invoiceNo = result.invoiceNo,
            grandTotal = result.pricedCart.grandTotal,
            paidAmount = result.allocation.paidAmount,
            changeAmount = result.allocation.changeAmount,
            shareText = ReceiptShareText.build(buildReceiptShareData(result), currentStrings),
            paymentMethod = result.allocation.paymentMethod,
            itemCount = result.pricedCart.lines.size,
        )
        _state.update { current ->
            current.copy(
                cartLines = emptyList(),
                pricedCart = null,
                isSubmitting = false,
                isPaymentSheetVisible = false,
                isCartSheetVisible = false,
                submitErrorMessage = null,
                receipt = receipt,
            )
        }
    }

    private fun buildReceiptDocument(result: CheckoutResult): ReceiptDocument {
        val pricedCart = result.pricedCart
        val lines = mutableListOf<ReceiptLine>()
        lines += ReceiptLine(currentStrings.receiptInvoiceHeader(result.invoiceNo), alignment = ReceiptAlignment.CENTER)
        lines += ReceiptLine("")
        pricedCart.lines.forEach { pricedLine ->
            lines += ReceiptLine("${formatQuantity(pricedLine.line.qty)} x ${pricedLine.product.name}")
            lines += ReceiptLine(
                twoColumn("", Rupiah.format(pricedLine.itemSubtotal)),
                alignment = ReceiptAlignment.START,
            )
        }
        lines += ReceiptLine("")
        lines += ReceiptLine(twoColumn(currentStrings.transactionsFieldSubtotal, Rupiah.format(pricedCart.subtotal)))
        if (pricedCart.invoiceDiscount > 0L) {
            lines += ReceiptLine(
                twoColumn(currentStrings.transactionsFieldDiscount, "-${Rupiah.format(pricedCart.invoiceDiscount)}"),
            )
        }
        if (pricedCart.taxTotal > 0L) {
            lines += ReceiptLine(twoColumn(currentStrings.transactionsFieldTax, Rupiah.format(pricedCart.taxTotal)))
        }
        if (pricedCart.rounding != 0L) {
            lines += ReceiptLine(
                twoColumn(currentStrings.transactionsFieldRounding, Rupiah.format(pricedCart.rounding)),
            )
        }
        lines += ReceiptLine(
            twoColumn(currentStrings.receiptTotalPlain, Rupiah.format(pricedCart.grandTotal)),
            isBold = true,
        )
        lines += ReceiptLine(
            twoColumn(currentStrings.transactionsFieldPaid, Rupiah.format(result.allocation.paidAmount)),
        )
        if (result.allocation.changeAmount > 0L) {
            lines += ReceiptLine(
                twoColumn(currentStrings.transactionsFieldChange, Rupiah.format(result.allocation.changeAmount)),
            )
        }
        return ReceiptDocument(storeName = storeName, lines = lines)
    }

    private fun buildReceiptShareData(result: CheckoutResult): ReceiptShareData {
        val pricedCart = result.pricedCart
        return ReceiptShareData(
            storeName = storeName,
            invoiceNo = result.invoiceNo,
            createdAtLabel = formatDateTime(clock.nowMillis(), currentStrings),
            cashierName = _state.value.activeStaff?.name
                ?: _state.value.activeShift?.cashierName
                ?: fallbackCashierName,
            paymentMethodLabel = paymentMethodLabel(result.allocation.paymentMethod, currentStrings),
            items = pricedCart.lines.map { pricedLine ->
                ReceiptShareItem(
                    name = pricedLine.product.name,
                    quantityLabel = formatQuantity(pricedLine.line.qty) + " " +
                        (pricedLine.line.variantName ?: pricedLine.product.baseUnit),
                    unitPrice = pricedLine.unitPrice,
                    lineTotal = pricedLine.itemSubtotal,
                    itemDiscount = pricedLine.line.itemDiscount,
                )
            },
            subtotal = pricedCart.subtotal,
            invoiceDiscount = pricedCart.invoiceDiscount,
            taxTotal = pricedCart.taxTotal,
            rounding = pricedCart.rounding,
            grandTotal = pricedCart.grandTotal,
            paidAmount = result.allocation.paidAmount,
            changeAmount = result.allocation.changeAmount,
        )
    }

    private fun twoColumn(label: String, value: String): String =
        twoColumnLine(label = label, value = value, widthChars = RECEIPT_WIDTH_CHARS)

    private fun updateLineEditorValidation(message: UiText) {
        _state.update { current ->
            val editor = current.cartLineEditor ?: return@update current
            current.copy(cartLineEditor = editor.copy(validationMessage = message))
        }
    }

    private fun lineEditorState(
        draft: CartLineDraft,
        product: Product,
        selectedUnitId: Long?,
        negotiatedUnitPriceInput: String,
        itemDiscountInput: String,
        validationMessage: UiText?,
    ): CartLineEditorState = CartLineEditorState(
        lineId = draft.lineId,
        productName = product.name,
        unitOptions = unitsCache[draft.productId].orEmpty(),
        selectedUnitId = selectedUnitId,
        negotiatedUnitPriceInput = negotiatedUnitPriceInput,
        itemDiscountInput = itemDiscountInput,
        validationMessage = validationMessage,
        pricing = lineEditorPricing(
            draft = draft,
            product = product,
            selectedUnitId = selectedUnitId,
            negotiatedUnitPriceInput = negotiatedUnitPriceInput,
            itemDiscountInput = itemDiscountInput,
        ),
    )

    private fun updateLineEditor(transform: (CartLineEditorState) -> CartLineEditorState) {
        _state.update { current ->
            val editor = current.cartLineEditor ?: return@update current
            val updated = transform(editor)
            val draft = cartDrafts.value.firstOrNull { it.lineId == updated.lineId }
            val product = draft?.let { productCache[it.productId] }
            if (draft == null || product == null) {
                current.copy(cartLineEditor = updated)
            } else {
                current.copy(
                    cartLineEditor = updated.copy(
                        pricing = lineEditorPricing(
                            draft = draft,
                            product = product,
                            selectedUnitId = updated.selectedUnitId,
                            negotiatedUnitPriceInput = updated.negotiatedUnitPriceInput,
                            itemDiscountInput = updated.itemDiscountInput,
                        ),
                    ),
                )
            }
        }
    }

    private fun lineEditorPricing(
        draft: CartLineDraft,
        product: Product,
        selectedUnitId: Long?,
        negotiatedUnitPriceInput: String,
        itemDiscountInput: String,
    ): CartLineEditorPricing = buildLineEditorPricing(
        quantity = draft.quantity,
        selectedUnitId = selectedUnitId,
        negotiatedUnitPrice = negotiatedUnitPriceInput.trim().toLongOrNull(),
        itemDiscount = itemDiscountInput.trim().toLongOrNull() ?: 0L,
        product = product,
        units = unitsCache[draft.productId].orEmpty(),
        priceEngine = priceEngine,
    )

    private companion object {
        const val QUANTITY_STEP = 1.0
        const val RECEIPT_WIDTH_CHARS = 32
    }
}
