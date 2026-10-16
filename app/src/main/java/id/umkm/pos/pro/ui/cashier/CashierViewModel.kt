package id.umkm.pos.pro.ui.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReceiptAlignment
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptLine
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ScannerInput
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.UiState
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
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.mapList
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.common.toAppError
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

private sealed interface PaidAmountResolution {
    data class Resolved(val paidAmount: Long) : PaidAmountResolution

    data class Rejected(val message: String) : PaidAmountResolution
}

@OptIn(ExperimentalCoroutinesApi::class)
class CashierViewModel(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
    private val priceEngine: PriceEngine,
    private val checkoutUseCase: CheckoutUseCase,
    private val receiptPrinter: ReceiptPrinter,
    private val receiptRenderer: ReceiptRenderer,
    private val scannerInput: ScannerInput,
) : ViewModel() {

    private val _state = MutableStateFlow(CashierUiState())
    val state: StateFlow<CashierUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val reloadTrigger = MutableStateFlow(0)
    private val cartDrafts = MutableStateFlow<List<CartLineDraft>>(emptyList())
    private val productCache = mutableMapOf<Long, Product>()
    private val unitsCache = mutableMapOf<Long, List<ProductUnit>>()

    private var pricingContext = PricingContext()
    private var storeName = ""
    private var nextLineNumber = 1
    private var pendingReceiptDocument: ReceiptDocument? = null

    init {
        observeSettings()
        observeCatalog()
        observeCustomers()
        observeScanEvents()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun onRetryCatalog() {
        reloadTrigger.update { it + 1 }
    }

    fun onAddProduct(productId: Long) {
        viewModelScope.launch { addProductToCart(productId, null) }
    }

    fun onIncreaseQuantity(lineId: String) {
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
                cartLineEditor = CartLineEditorState(
                    lineId = draft.lineId,
                    productName = product.name,
                    unitOptions = unitsCache[draft.productId].orEmpty(),
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
        _state.update { current ->
            val editor = current.cartLineEditor ?: return@update current
            current.copy(cartLineEditor = editor.copy(selectedUnitId = unitId, validationMessage = null))
        }
    }

    fun onLineEditorPriceChange(value: String) {
        val digits = value.filter { it.isDigit() }
        _state.update { current ->
            val editor = current.cartLineEditor ?: return@update current
            current.copy(cartLineEditor = editor.copy(negotiatedUnitPriceInput = digits, validationMessage = null))
        }
    }

    fun onLineEditorDiscountChange(value: String) {
        val digits = value.filter { it.isDigit() }
        _state.update { current ->
            val editor = current.cartLineEditor ?: return@update current
            current.copy(cartLineEditor = editor.copy(itemDiscountInput = digits, validationMessage = null))
        }
    }

    fun onApplyLineEditor() {
        val editor = _state.value.cartLineEditor ?: return
        val priceInput = editor.negotiatedUnitPriceInput.trim()
        val negotiatedUnitPrice = if (priceInput.isEmpty()) null else priceInput.toLongOrNull()
        if (priceInput.isNotEmpty() && negotiatedUnitPrice == null) {
            updateLineEditorValidation("Enter a valid negotiated price.")
            return
        }
        val discountInput = editor.itemDiscountInput.trim()
        val itemDiscount = if (discountInput.isEmpty()) 0L else discountInput.toLongOrNull()
        if (itemDiscount == null) {
            updateLineEditorValidation("Enter a valid item discount.")
            return
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
        _state.update {
            it.copy(
                isPaymentSheetVisible = true,
                isCartSheetVisible = false,
                paymentDraft = PaymentDraft(),
                submitErrorMessage = null,
            )
        }
    }

    fun onClosePaymentSheet() {
        _state.update { it.copy(isPaymentSheetVisible = false, submitErrorMessage = null) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _state.update { current ->
            current.copy(
                paymentDraft = PaymentDraft(
                    method = method,
                    customerId = if (method == PaymentMethod.KASBON) current.paymentDraft.customerId else null,
                ),
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

    fun onOpenCustomerPicker() {
        _state.update { it.copy(isCustomerPickerVisible = true) }
    }

    fun onCloseCustomerPicker() {
        _state.update { it.copy(isCustomerPickerVisible = false) }
    }

    fun onCustomerSelected(customerId: Long?) {
        _state.update { current ->
            current.copy(
                paymentDraft = current.paymentDraft.copy(customerId = customerId),
                isCustomerPickerVisible = false,
                submitErrorMessage = null,
            )
        }
    }

    fun onOpenDueDatePicker() {
        _state.update { it.copy(isDueDatePickerVisible = true) }
    }

    fun onCloseDueDatePicker() {
        _state.update { it.copy(isDueDatePickerVisible = false) }
    }

    fun onDueDateChange(epochMillis: Long) {
        _state.update { current ->
            current.copy(
                paymentDraft = current.paymentDraft.copy(dueDateMillis = epochMillis),
                isDueDatePickerVisible = false,
            )
        }
    }

    fun onConfirmCharge() {
        val pricedCart = _state.value.pricedCart ?: return
        val drafts = cartDrafts.value
        if (drafts.isEmpty() || _state.value.isSubmitting) return

        val draft = _state.value.paymentDraft
        val paidAmount = when (val resolution = resolvePaidAmount(draft, pricedCart.grandTotal)) {
            is PaidAmountResolution.Rejected -> {
                _state.update { it.copy(submitErrorMessage = resolution.message) }
                return
            }

            is PaidAmountResolution.Resolved -> resolution.paidAmount
        }
        val remainingAfterPayment = (pricedCart.grandTotal - paidAmount).coerceAtLeast(0L)
        if (remainingAfterPayment > 0L && draft.customerId == null) {
            _state.update { it.copy(submitErrorMessage = "Choose a customer for the outstanding balance.") }
            return
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
            customerId = draft.customerId,
            paymentMethod = draft.method,
            paidAmount = paidAmount,
            dueDate = if (remainingAfterPayment > 0L) draft.dueDateMillis else null,
            invoiceDiscount = 0L,
            taxRatePercent = pricingContext.taxRatePercent,
            roundingIncrement = pricingContext.roundingIncrement,
            notes = null,
        )

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitErrorMessage = null) }
            when (val result = checkoutUseCase(request)) {
                is AppResult.Success -> onCheckoutCompleted(result.value)
                is AppResult.Failure -> _state.update { current ->
                    current.copy(isSubmitting = false, submitErrorMessage = result.error.message)
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
                    it.copy(isPrintingReceipt = false, receiptMessage = "Receipt sent to the printer.")
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isPrintingReceipt = false, receiptMessage = result.error.message)
                }
            }
        }
    }

    fun onNewSale() {
        pendingReceiptDocument = null
        _state.update { it.copy(receipt = null, isPrintingReceipt = false, receiptMessage = null) }
    }

    fun onMessageConsumed() {
        _state.update { it.copy(message = null) }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                storeName = settings.storeName
                pricingContext = PricingContext(
                    invoiceDiscount = 0L,
                    taxRatePercent = settings.taxRatePercent,
                    roundingIncrement = settings.roundingIncrement,
                )
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
                    _state.update { current ->
                        current.copy(
                            catalog = catalogState.mapList { product ->
                                CashierProductItem(product = product, stockStatus = resolveStockStatus(product))
                            },
                        )
                    }
                    refreshCart()
                }
        }
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            customerRepository.observeCustomers(null).collect { customers ->
                _state.update { it.copy(payableCustomers = customers) }
            }
        }
    }

    private fun observeScanEvents() {
        viewModelScope.launch {
            scannerInput.scanEvents.collect { scanEvent -> addScannedCode(scanEvent.code) }
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
        _state.update { it.copy(message = "No product matched code $code") }
    }

    private suspend fun addProductToCart(productId: Long, unitId: Long?) {
        val product = productCache[productId] ?: productRepository.getProductById(productId)
        if (product == null) {
            _state.update { it.copy(message = "Product was not found") }
            return
        }
        productCache[productId] = product
        if (!unitsCache.containsKey(productId)) {
            unitsCache[productId] = productRepository.getUnits(productId)
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
        _state.update { it.copy(message = "Added ${product.name}") }
        refreshCart()
    }

    private fun refreshCart() {
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
    )

    private fun resolvePaidAmount(draft: PaymentDraft, grandTotal: Long): PaidAmountResolution = when (draft.method) {
        PaymentMethod.CASH -> {
            val tendered = draft.amountReceivedInput.toLongOrNull()
            when {
                tendered == null -> PaidAmountResolution.Rejected("Enter the amount received.")
                tendered < grandTotal -> PaidAmountResolution.Rejected("Amount received is less than the total.")
                else -> PaidAmountResolution.Resolved(tendered)
            }
        }

        PaymentMethod.QRIS, PaymentMethod.TRANSFER -> PaidAmountResolution.Resolved(grandTotal)

        PaymentMethod.SPLIT -> {
            val tendered = draft.amountReceivedInput.toLongOrNull()
            if (tendered == null) {
                PaidAmountResolution.Rejected("Enter the amount received.")
            } else {
                PaidAmountResolution.Resolved(tendered)
            }
        }

        PaymentMethod.KASBON -> PaidAmountResolution.Resolved(draft.amountReceivedInput.toLongOrNull() ?: 0L)
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
            remainingDebt = result.allocation.remainingDebt,
            shareText = receiptRenderer.renderText(document, RECEIPT_WIDTH_CHARS),
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
        lines += ReceiptLine("Invoice ${result.invoiceNo}", alignment = ReceiptAlignment.CENTER)
        lines += ReceiptLine("")
        pricedCart.lines.forEach { pricedLine ->
            lines += ReceiptLine("${formatQuantity(pricedLine.line.qty)} x ${pricedLine.product.name}")
            lines += ReceiptLine(
                twoColumn("", Rupiah.format(pricedLine.itemSubtotal)),
                alignment = ReceiptAlignment.START,
            )
        }
        lines += ReceiptLine("")
        lines += ReceiptLine(twoColumn("Subtotal", Rupiah.format(pricedCart.subtotal)))
        if (pricedCart.invoiceDiscount > 0L) {
            lines += ReceiptLine(twoColumn("Discount", "-${Rupiah.format(pricedCart.invoiceDiscount)}"))
        }
        if (pricedCart.taxTotal > 0L) {
            lines += ReceiptLine(twoColumn("Tax", Rupiah.format(pricedCart.taxTotal)))
        }
        if (pricedCart.rounding != 0L) {
            lines += ReceiptLine(twoColumn("Rounding", Rupiah.format(pricedCart.rounding)))
        }
        lines += ReceiptLine(twoColumn("TOTAL", Rupiah.format(pricedCart.grandTotal)), isBold = true)
        lines += ReceiptLine(twoColumn("Paid", Rupiah.format(result.allocation.paidAmount)))
        if (result.allocation.changeAmount > 0L) {
            lines += ReceiptLine(twoColumn("Change", Rupiah.format(result.allocation.changeAmount)))
        }
        if (result.allocation.remainingDebt > 0L) {
            lines += ReceiptLine(twoColumn("Remaining", Rupiah.format(result.allocation.remainingDebt)))
        }
        return ReceiptDocument(storeName = storeName, lines = lines)
    }

    private fun twoColumn(label: String, value: String): String {
        val available = RECEIPT_WIDTH_CHARS - value.length
        if (available <= label.length) return "$label $value"
        return label + " ".repeat(available - label.length) + value
    }

    private fun updateLineEditorValidation(message: String) {
        _state.update { current ->
            val editor = current.cartLineEditor ?: return@update current
            current.copy(cartLineEditor = editor.copy(validationMessage = message))
        }
    }

    private companion object {
        const val QUANTITY_STEP = 1.0
        const val RECEIPT_WIDTH_CHARS = 32
    }
}
