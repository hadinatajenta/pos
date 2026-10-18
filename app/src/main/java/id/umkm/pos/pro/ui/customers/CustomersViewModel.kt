package id.umkm.pos.pro.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.domain.usecase.RecordDebtPaymentUseCase
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.mapList
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

@OptIn(ExperimentalCoroutinesApi::class)
class CustomersViewModel(
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val recordDebtPaymentUseCase: RecordDebtPaymentUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomersUiState())
    val state: StateFlow<CustomersUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val reloadTrigger = MutableStateFlow(0)
    private val customerList = MutableStateFlow<UiState<List<Customer>>>(UiState.Loading)
    private val debtTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    init {
        observeCustomerList()
        observeTransactions()
        combineCustomerState()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun onRetry() {
        reloadTrigger.update { it + 1 }
    }

    fun onOpenRecordPayment(customerId: Long) {
        val item = _state.value.customers
            .successDataOrNull()
            ?.firstOrNull { it.customer.id == customerId } ?: return
        val nowMillis = clock.nowMillis()
        val unpaidTransactions = debtTransactions.value
            .filter { it.customerId == customerId && it.remainingDebt > 0L }
            .sortedBy { it.dueDate ?: Long.MAX_VALUE }
        _state.update { current ->
            current.copy(
                recordPayment = RecordPaymentState(
                    customerId = customerId,
                    customerName = item.customer.name,
                    outstandingDebt = item.outstandingDebt,
                    transactions = unpaidTransactions.map { transaction ->
                        OutstandingTransactionItem(
                            transactionId = transaction.id,
                            invoiceNo = transaction.invoiceNo,
                            remainingDebt = transaction.remainingDebt,
                            dueDateMillis = transaction.dueDate,
                            isOverdue = isOverdue(transaction, nowMillis),
                        )
                    },
                    selectedTransactionId = unpaidTransactions.firstOrNull()?.id,
                    amountInput = "",
                    method = PaymentMethod.CASH,
                    note = "",
                    validationMessage = null,
                ),
            )
        }
    }

    fun onCloseRecordPayment() {
        _state.update { it.copy(recordPayment = null) }
    }

    fun onSelectTransaction(transactionId: Long) {
        _state.update { current ->
            val draft = current.recordPayment ?: return@update current
            current.copy(recordPayment = draft.copy(selectedTransactionId = transactionId, validationMessage = null))
        }
    }

    fun onAmountChange(value: String) {
        val digits = value.filter { it.isDigit() }
        _state.update { current ->
            val draft = current.recordPayment ?: return@update current
            current.copy(recordPayment = draft.copy(amountInput = digits, validationMessage = null))
        }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _state.update { current ->
            val draft = current.recordPayment ?: return@update current
            current.copy(recordPayment = draft.copy(method = method))
        }
    }

    fun onNoteChange(note: String) {
        _state.update { current ->
            val draft = current.recordPayment ?: return@update current
            current.copy(recordPayment = draft.copy(note = note))
        }
    }

    fun onConfirmPayment() {
        val draft = _state.value.recordPayment ?: return
        if (_state.value.isSubmitting) return
        val transactionId = draft.selectedTransactionId
        if (transactionId == null) {
            updatePaymentValidation(UiText.from { strings -> strings.customersValidationSelectDebt })
            return
        }
        val amount = draft.amountInput.toLongOrNull()
        if (amount == null || amount <= 0L) {
            updatePaymentValidation(UiText.from { strings -> strings.customersValidationAmountPositive })
            return
        }
        val remainingDebt = draft.transactions.firstOrNull { it.transactionId == transactionId }?.remainingDebt ?: 0L
        if (amount > remainingDebt) {
            updatePaymentValidation(UiText.from { strings -> strings.customersValidationAmountExceeds })
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            when (
                val result = recordDebtPaymentUseCase(
                    transactionId = transactionId,
                    amount = amount,
                    paymentMethod = draft.method,
                    note = draft.note.trim().ifBlank { null },
                )
            ) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        recordPayment = null,
                        message = UiText.from { strings -> strings.customersPaymentRecorded },
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        recordPayment = draft.copy(validationMessage = result.error.toUiText()),
                    )
                }
            }
        }
    }

    fun onOpenCreateCustomer() {
        _state.update { it.copy(customerForm = CustomerFormState()) }
    }

    fun onCloseCustomerForm() {
        _state.update { it.copy(customerForm = null) }
    }

    fun onCustomerNameChange(name: String) {
        _state.update { current ->
            val form = current.customerForm ?: return@update current
            current.copy(customerForm = form.copy(name = name, validationMessage = null))
        }
    }

    fun onCustomerWhatsappChange(whatsappNumber: String) {
        _state.update { current ->
            val form = current.customerForm ?: return@update current
            current.copy(customerForm = form.copy(whatsappNumber = whatsappNumber))
        }
    }

    fun onCustomerNotesChange(notes: String) {
        _state.update { current ->
            val form = current.customerForm ?: return@update current
            current.copy(customerForm = form.copy(notes = notes))
        }
    }

    fun onSubmitCustomerForm() {
        val form = _state.value.customerForm ?: return
        if (_state.value.isSubmitting) return
        if (form.name.isBlank()) {
            _state.update { current ->
                current.copy(
                    customerForm = form.copy(
                        validationMessage = UiText.from { strings -> strings.customersValidationName },
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            customerRepository.upsertCustomer(
                Customer(
                    id = 0L,
                    name = form.name.trim(),
                    whatsappNumber = form.whatsappNumber.trim().ifBlank { null },
                    totalDebt = 0L,
                    notes = form.notes.trim().ifBlank { null },
                ),
            )
            _state.update {
                it.copy(
                    isSubmitting = false,
                    customerForm = null,
                    message = UiText.from { strings -> strings.customersSaved },
                )
            }
        }
    }

    fun onMessageConsumed() {
        _state.update { it.copy(message = null) }
    }

    private fun updatePaymentValidation(message: UiText) {
        _state.update { current ->
            val draft = current.recordPayment ?: return@update current
            current.copy(recordPayment = draft.copy(validationMessage = message))
        }
    }

    private fun observeCustomerList() {
        viewModelScope.launch {
            combine(searchQuery, reloadTrigger) { query, _ -> query }
                .flatMapLatest { query ->
                    customerRepository.observeCustomers(query.ifBlank { null })
                        .map<List<Customer>, UiState<List<Customer>>> { customers ->
                            if (customers.isEmpty()) UiState.Empty else UiState.Success(customers)
                        }
                        .catch { error -> emit(UiState.Error(error.toAppError())) }
                }
                .collect { listState -> customerList.value = listState }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.observeTransactions(ReportRange(0L, Long.MAX_VALUE)).collect { transactions ->
                debtTransactions.value = transactions
            }
        }
    }

    private fun combineCustomerState() {
        viewModelScope.launch {
            combine(customerList, debtTransactions) { customers, transactions -> customers to transactions }
                .collect { (customers, transactions) ->
                    val nowMillis = clock.nowMillis()
                    _state.update { current ->
                        current.copy(
                            customers = customers.mapList { customer ->
                                customer.toDebtItem(transactions = transactions, nowMillis = nowMillis)
                            },
                        )
                    }
                }
        }
    }

    private fun Customer.toDebtItem(
        transactions: List<Transaction>,
        nowMillis: Long,
    ): CustomerDebtItem {
        val customerTransactions = transactions.filter { it.customerId == id }
        return CustomerDebtItem(
            customer = this,
            outstandingDebt = totalDebt,
            nextDueDateMillis = nextDueDateMillis(customerTransactions),
            status = resolveDebtStatus(customer = this, transactions = customerTransactions, nowMillis = nowMillis),
        )
    }
}
