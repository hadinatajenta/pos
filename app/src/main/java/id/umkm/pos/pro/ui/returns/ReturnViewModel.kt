package id.umkm.pos.pro.ui.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.domain.usecase.ProcessReturnUseCase
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.sanitizeQuantityInput
import id.umkm.pos.pro.ui.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReturnViewModel(
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository,
    private val processReturnUseCase: ProcessReturnUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReturnUiState())
    val state: StateFlow<ReturnUiState> = _state.asStateFlow()

    private var activeTransactionId: Long? = null

    fun onScreenShown(transactionId: Long) {
        if (activeTransactionId == transactionId) return
        activeTransactionId = transactionId
        _state.value = ReturnUiState()
        load()
    }

    fun onReturnQtyChange(transactionItemId: Long, value: String) {
        _state.update { current ->
            current.copy(
                lines = current.lines.map { line ->
                    if (line.transactionItemId == transactionItemId) {
                        line.copy(returnQty = sanitizeQuantityInput(value))
                    } else {
                        line
                    }
                },
                lineErrors = current.lineErrors - transactionItemId,
                formError = null,
                submitError = null,
            )
        }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun onRetry() {
        load()
    }

    fun onProcessed() {
        activeTransactionId = null
    }

    fun onSubmit() {
        val transactionId = activeTransactionId ?: return
        val current = _state.value
        if (current.isSubmitting || current.isProcessed) return
        val validation = validateReturn(current.lines)
        if (!validation.isValid) {
            _state.update { it.copy(lineErrors = validation.lineErrors, formError = validation.formError) }
            return
        }
        val requests = toReturnLineRequests(current.lines)
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitError = null) }
            when (val result = processReturnUseCase(transactionId, requests, current.note.trim().ifBlank { null })) {
                is AppResult.Success -> _state.update { it.copy(isSubmitting = false, isProcessed = true) }
                is AppResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, submitError = result.error.toUiText())
                }
            }
        }
    }

    private fun load() {
        val transactionId = activeTransactionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            val transaction = transactionRepository.getTransaction(transactionId)
            if (transaction == null) {
                _state.update { it.copy(isLoading = false, loadError = AppError.NotFound("This sale could not be found.")) }
                return@launch
            }
            val items = transactionRepository.getItems(transactionId)
            if (items.isEmpty()) {
                _state.update { it.copy(isLoading = false, loadError = AppError.NotFound("This sale has no items to return.")) }
                return@launch
            }
            val lines = items.map { item ->
                val productName = productRepository.getProductById(item.productId)?.name
                ReturnLineInput(
                    transactionItemId = item.id,
                    itemName = productName?.let { name -> UiText.literal(name) }
                        ?: UiText.from { strings -> strings.returnItemFallbackName(item.productId) },
                    variantName = item.variantName,
                    purchasedQty = item.qty,
                    returnQty = formatQuantity(item.qty),
                )
            }
            _state.update { it.copy(isLoading = false, invoiceNo = transaction.invoiceNo, lines = lines) }
        }
    }
}
