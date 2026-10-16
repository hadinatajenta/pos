package id.umkm.pos.pro.ui.customers

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.domain.usecase.FakeClock
import id.umkm.pos.pro.domain.usecase.RecordDebtPaymentUseCase
import id.umkm.pos.pro.ui.FakeCustomerStore
import id.umkm.pos.pro.ui.FakeTransactionStore
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.uiSampleCustomer
import id.umkm.pos.pro.ui.uiSampleDebtTransaction
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CustomersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock(currentMillis = 10_000L)
    private val customers = FakeCustomerStore()
    private val transactions = FakeTransactionStore()

    private fun createViewModel(): CustomersViewModel = CustomersViewModel(
        customerRepository = customers,
        transactionRepository = transactions,
        recordDebtPaymentUseCase = RecordDebtPaymentUseCase(transactions, clock),
        clock = clock,
    )

    private fun itemsOf(state: CustomersUiState): List<CustomerDebtItem> =
        state.customers.successDataOrNull().orEmpty()

    @Test
    fun loadingWithoutCustomersReportsEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        assertThat(viewModel.state.value.customers).isEqualTo(UiState.Empty)
    }

    @Test
    fun overdueDebtIsReportedAsOverdueWithTheEarliestDueDate() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(listOf(uiSampleCustomer(id = 1L, name = "Pak Budi", totalDebt = 150_000L)))
        transactions.setTransactions(
            listOf(
                uiSampleDebtTransaction(id = 10L, customerId = 1L, remainingDebt = 150_000L, dueDate = 5_000L),
                uiSampleDebtTransaction(id = 11L, customerId = 1L, remainingDebt = 0L, paidAmount = 5_000L),
            ),
        )
        val viewModel = createViewModel()

        val item = itemsOf(viewModel.state.value).single()

        assertThat(item.status).isEqualTo(DebtStatus.OVERDUE)
        assertThat(item.outstandingDebt).isEqualTo(150_000L)
        assertThat(item.nextDueDateMillis).isEqualTo(5_000L)
    }

    @Test
    fun customerWithoutDebtIsReportedAsPaid() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(listOf(uiSampleCustomer(id = 1L, name = "Ibu Ani", totalDebt = 0L)))
        val viewModel = createViewModel()

        val item = itemsOf(viewModel.state.value).single()

        assertThat(item.status).isEqualTo(DebtStatus.PAID)
        assertThat(item.nextDueDateMillis).isNull()
    }

    @Test
    fun recordingAPaymentClosesTheSheetAndStoresThePayment() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(listOf(uiSampleCustomer(id = 7L, name = "Pak Budi", totalDebt = 100_000L)))
        transactions.setTransactions(
            listOf(uiSampleDebtTransaction(id = 20L, customerId = 7L, remainingDebt = 100_000L)),
        )
        val viewModel = createViewModel()
        viewModel.onOpenRecordPayment(7L)
        assertThat(viewModel.state.value.recordPayment).isNotNull()

        viewModel.onSelectTransaction(20L)
        viewModel.onAmountChange("40000")
        viewModel.onPaymentMethodChange(PaymentMethod.CASH)
        viewModel.onConfirmPayment()

        val state = viewModel.state.value
        assertThat(state.recordPayment).isNull()
        assertThat(state.message).isEqualTo("Payment recorded")
        val recorded = transactions.recordedPayments.single()
        assertThat(recorded.transactionId).isEqualTo(20L)
        assertThat(recorded.customerId).isEqualTo(7L)
        assertThat(recorded.amount).isEqualTo(40_000L)
    }

    @Test
    fun paymentAboveTheOutstandingBalanceIsRejected() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(listOf(uiSampleCustomer(id = 7L, name = "Pak Budi", totalDebt = 100_000L)))
        transactions.setTransactions(
            listOf(uiSampleDebtTransaction(id = 20L, customerId = 7L, remainingDebt = 100_000L)),
        )
        val viewModel = createViewModel()
        viewModel.onOpenRecordPayment(7L)

        viewModel.onAmountChange("200000")
        viewModel.onConfirmPayment()

        assertThat(viewModel.state.value.recordPayment?.validationMessage)
            .isEqualTo("The payment exceeds the outstanding balance.")
        assertThat(transactions.recordedPayments).isEmpty()
    }

    @Test
    fun zeroAmountIsRejected() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(listOf(uiSampleCustomer(id = 7L, name = "Pak Budi", totalDebt = 100_000L)))
        transactions.setTransactions(
            listOf(uiSampleDebtTransaction(id = 20L, customerId = 7L, remainingDebt = 100_000L)),
        )
        val viewModel = createViewModel()
        viewModel.onOpenRecordPayment(7L)

        viewModel.onAmountChange("0")
        viewModel.onConfirmPayment()

        assertThat(viewModel.state.value.recordPayment?.validationMessage)
            .isEqualTo("Enter a payment amount greater than zero.")
        assertThat(transactions.recordedPayments).isEmpty()
    }

    @Test
    fun customerWithoutOutstandingDebtHasNoPayableTransactions() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(listOf(uiSampleCustomer(id = 7L, name = "Pak Budi", totalDebt = 0L)))
        transactions.setTransactions(
            listOf(uiSampleDebtTransaction(id = 20L, customerId = 7L, remainingDebt = 0L, paidAmount = 50_000L)),
        )
        val viewModel = createViewModel()

        viewModel.onOpenRecordPayment(7L)

        assertThat(viewModel.state.value.recordPayment?.transactions).isEmpty()
    }

    @Test
    fun creatingACustomerRequiresAName() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onOpenCreateCustomer()
        viewModel.onSubmitCustomerForm()

        assertThat(viewModel.state.value.customerForm?.validationMessage).isEqualTo("Enter a customer name.")
        assertThat(customers.currentCustomers()).isEmpty()
    }

    @Test
    fun creatingACustomerStoresIt() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateCustomer()
        viewModel.onCustomerNameChange("Ibu Ani")
        viewModel.onCustomerWhatsappChange("0812345678")

        viewModel.onSubmitCustomerForm()

        val saved = customers.currentCustomers().single()
        assertThat(saved.name).isEqualTo("Ibu Ani")
        assertThat(saved.whatsappNumber).isEqualTo("0812345678")
        assertThat(viewModel.state.value.customerForm).isNull()
        assertThat(viewModel.state.value.message).isEqualTo("Customer saved")
    }

    @Test
    fun searchQueryFiltersCustomers() = runTest(mainDispatcherRule.testDispatcher) {
        customers.setCustomers(
            listOf(
                uiSampleCustomer(id = 1L, name = "Pak Budi", totalDebt = 0L),
                uiSampleCustomer(id = 2L, name = "Ibu Ani", totalDebt = 0L),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Ani")

        assertThat(itemsOf(viewModel.state.value).map { it.customer.name }).containsExactly("Ibu Ani")
    }
}
