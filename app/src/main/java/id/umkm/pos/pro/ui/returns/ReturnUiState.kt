package id.umkm.pos.pro.ui.returns

import id.umkm.pos.pro.core.contract.AppError

data class ReturnLineInput(
    val transactionItemId: Long,
    val itemName: String,
    val variantName: String?,
    val purchasedQty: Double,
    val returnQty: String,
)

data class ReturnUiState(
    val invoiceNo: String? = null,
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val lines: List<ReturnLineInput> = emptyList(),
    val note: String = "",
    val lineErrors: Map<Long, String> = emptyMap(),
    val formError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val isProcessed: Boolean = false,
)
