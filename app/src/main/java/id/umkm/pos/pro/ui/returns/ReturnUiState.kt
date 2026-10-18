package id.umkm.pos.pro.ui.returns

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.ui.common.UiText

data class ReturnLineInput(
    val transactionItemId: Long,
    val itemName: UiText,
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
    val lineErrors: Map<Long, UiText> = emptyMap(),
    val formError: UiText? = null,
    val isSubmitting: Boolean = false,
    val submitError: UiText? = null,
    val isProcessed: Boolean = false,
)
