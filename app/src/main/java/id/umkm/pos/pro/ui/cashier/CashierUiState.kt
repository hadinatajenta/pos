package id.umkm.pos.pro.ui.cashier

import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.ui.common.StockStatus

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
    val customerId: Long? = null,
    val dueDateMillis: Long? = null,
)

data class CartLineEditorState(
    val lineId: String,
    val productName: String,
    val unitOptions: List<ProductUnit>,
    val selectedUnitId: Long?,
    val negotiatedUnitPriceInput: String,
    val itemDiscountInput: String,
    val validationMessage: String?,
)

data class CashierReceipt(
    val invoiceNo: String,
    val grandTotal: Long,
    val paidAmount: Long,
    val changeAmount: Long,
    val remainingDebt: Long,
    val shareText: String,
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
    val payableCustomers: List<Customer> = emptyList(),
    val isCustomerPickerVisible: Boolean = false,
    val isDueDatePickerVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,
    val receipt: CashierReceipt? = null,
    val isPrintingReceipt: Boolean = false,
    val receiptMessage: String? = null,
    val message: String? = null,
) {
    val lineCount: Int get() = cartLines.size

    val grandTotal: Long get() = pricedCart?.grandTotal ?: 0L

    val selectedCustomerName: String?
        get() = payableCustomers.firstOrNull { it.id == paymentDraft.customerId }?.name
}
