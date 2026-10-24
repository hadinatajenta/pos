package id.umkm.pos.pro.ui.cashier

import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.domain.security.PinPolicy
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.UiText

data class CashierProductItem(
    val product: Product,
    val stockStatus: StockStatus,
    val quantityInCart: Double = 0.0,
)

data class CatalogFilter(
    val category: String? = null,
    val isAvailableOnly: Boolean = false,
) {
    val isActive: Boolean get() = category != null || isAvailableOnly

    fun matches(item: CashierProductItem): Boolean {
        if (category != null && item.product.category != category) return false
        if (!isAvailableOnly) return true
        return item.stockStatus == StockStatus.IN_STOCK || item.stockStatus == StockStatus.LOW_STOCK
    }
}

data class CashierCartLine(
    val lineId: String,
    val productId: Long,
    val unitId: Long?,
    val productName: String,
    val variantName: String?,
    val unitLabel: String,
    val quantity: Double,
    val unitPrice: Long,
    val itemDiscount: Long,
    val lineSubtotal: Long,
    val isNegotiated: Boolean,
    val isWholesale: Boolean,
    val categoryName: String,
    val imagePath: String? = null,
    val icon: String? = null,
    val stock: Double = 0.0,
)

internal data class CartLineDraft(
    val lineId: String,
    val productId: Long,
    val unitId: Long?,
    val quantity: Double,
    val negotiatedUnitPrice: Long?,
    val itemDiscount: Long,
)

data class PaymentDraft(
    val method: PaymentMethod = PaymentMethod.CASH,
    val amountReceivedInput: String = "",
)

data class CartLineEditorPricing(
    val unitPrice: Long,
    val lineSubtotal: Long,
    val itemDiscount: Long,
    val unitHpp: Long,
    val negotiatedUnitPrice: Long?,
) {
    fun isBelowCost(policy: PinPolicy): Boolean =
        negotiatedUnitPrice != null && policy.isBelowCost(negotiatedUnitPrice, unitHpp)

    fun exceedsDiscountThreshold(policy: PinPolicy): Boolean =
        policy.exceedsDiscountThreshold(lineSubtotal, itemDiscount)
}

data class CartLineEditorState(
    val lineId: String,
    val productName: String,
    val unitOptions: List<ProductUnit>,
    val selectedUnitId: Long?,
    val negotiatedUnitPriceInput: String,
    val itemDiscountInput: String,
    val validationMessage: UiText?,
    val pricing: CartLineEditorPricing,
)

data class CashierReceipt(
    val invoiceNo: String,
    val grandTotal: Long,
    val paidAmount: Long,
    val changeAmount: Long,
    val shareText: String,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val itemCount: Int = 0,
)

data class CashierUiState(
    val searchQuery: String = "",
    val catalog: UiState<List<CashierProductItem>> = UiState.Loading,
    val catalogFilter: CatalogFilter = CatalogFilter(),
    val catalogCategories: List<String> = emptyList(),
    val cartLines: List<CashierCartLine> = emptyList(),
    val pricedCart: PricedCart? = null,
    val isCartSheetVisible: Boolean = false,
    val cartLineEditor: CartLineEditorState? = null,
    val isPaymentSheetVisible: Boolean = false,
    val paymentDraft: PaymentDraft = PaymentDraft(),
    val isScanDialogVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitErrorMessage: UiText? = null,
    val receipt: CashierReceipt? = null,
    val isPrintingReceipt: Boolean = false,
    val receiptMessage: UiText? = null,
    val message: UiText? = null,
    val shouldSuppressMessageSnackbar: Boolean = false,
    val qrisPayload: String? = null,
    val activeShift: CashierShift? = null,
    val activeStaff: Staff? = null,
    val isShiftPromptVisible: Boolean = false,
) {
    val lineCount: Int get() = cartLines.size

    val totalQuantity: Double get() = cartLines.sumOf { it.quantity }

    val grandTotal: Long get() = pricedCart?.grandTotal ?: 0L
}
