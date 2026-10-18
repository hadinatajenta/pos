package id.umkm.pos.pro.ui.customers

import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.ui.common.UiText

data class CustomerDebtItem(
    val customer: Customer,
    val outstandingDebt: Long,
    val nextDueDateMillis: Long?,
    val status: DebtStatus,
)

data class OutstandingTransactionItem(
    val transactionId: Long,
    val invoiceNo: String,
    val remainingDebt: Long,
    val dueDateMillis: Long?,
    val isOverdue: Boolean,
)

data class RecordPaymentState(
    val customerId: Long,
    val customerName: String,
    val outstandingDebt: Long,
    val transactions: List<OutstandingTransactionItem>,
    val selectedTransactionId: Long?,
    val amountInput: String,
    val method: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val validationMessage: UiText? = null,
)

data class CustomerFormState(
    val name: String = "",
    val whatsappNumber: String = "",
    val notes: String = "",
    val validationMessage: UiText? = null,
)

data class CustomersUiState(
    val searchQuery: String = "",
    val customers: UiState<List<CustomerDebtItem>> = UiState.Loading,
    val recordPayment: RecordPaymentState? = null,
    val customerForm: CustomerFormState? = null,
    val isSubmitting: Boolean = false,
    val message: UiText? = null,
)
