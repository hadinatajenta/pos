package id.umkm.pos.pro.ui.returns

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.usecase.CheckoutLine
import id.umkm.pos.pro.domain.usecase.CheckoutRequest
import id.umkm.pos.pro.domain.usecase.CheckoutResult
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.domain.usecase.FakeClock
import id.umkm.pos.pro.domain.usecase.FakeProductRepository
import id.umkm.pos.pro.domain.usecase.FakeReturnRepository
import id.umkm.pos.pro.domain.usecase.FakeTransactionRepository
import id.umkm.pos.pro.domain.usecase.ProcessReturnUseCase
import id.umkm.pos.pro.domain.usecase.sampleProduct
import id.umkm.pos.pro.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ReturnViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock(9_000L)
    private val products = FakeProductRepository()
        .addProduct(sampleProduct(1L))
        .addProduct(sampleProduct(2L))
    private val transactions = FakeTransactionRepository()
    private val returns = FakeReturnRepository(transactions)
    private val checkout = CheckoutUseCase(products, transactions, DefaultPriceEngine(), clock)
    private val processReturn = ProcessReturnUseCase(transactions, returns, products, clock)

    private suspend fun seedSale(): Long {
        val result = checkout(
            CheckoutRequest(
                lines = listOf(
                    CheckoutLine(productId = 1L, unitId = null, qty = 2.0, negotiatedUnitPrice = null, itemDiscount = 0L),
                    CheckoutLine(productId = 2L, unitId = null, qty = 3.0, negotiatedUnitPrice = null, itemDiscount = 0L),
                ),
                customerId = null,
                paymentMethod = PaymentMethod.CASH,
                paidAmount = 1_000_000L,
                dueDate = null,
                invoiceDiscount = 0L,
                taxRatePercent = 0,
                roundingIncrement = 0L,
                notes = null,
            ),
        )
        return (result as AppResult.Success<CheckoutResult>).value.transactionId
    }

    private fun createViewModel(transactionId: Long): ReturnViewModel =
        ReturnViewModel(
            transactionRepository = transactions,
            productRepository = products,
            processReturnUseCase = processReturn,
        ).apply { onScreenShown(transactionId) }

    @Test
    fun loadsItemsWithFullDefaultQuantities() = runTest(mainDispatcherRule.testDispatcher) {
        val transactionId = seedSale()

        val state = createViewModel(transactionId).state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.loadError).isNull()
        assertThat(state.lines.map { it.returnQty }).containsExactly("2", "3")
    }

    @Test
    fun missingTransactionReportsLoadError() = runTest(mainDispatcherRule.testDispatcher) {
        val state = createViewModel(4_242L).state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.loadError).isInstanceOf(AppError.NotFound::class.java)
    }

    @Test
    fun submittingAValidReturnRecordsItAndMarksProcessed() = runTest(mainDispatcherRule.testDispatcher) {
        val transactionId = seedSale()
        val viewModel = createViewModel(transactionId)

        viewModel.onSubmit()

        val state = viewModel.state.value
        assertThat(state.isProcessed).isTrue()
        assertThat(state.submitError).isNull()
        assertThat(returns.recorded.single().record.transactionId).isEqualTo(transactionId)
        assertThat(returns.recorded.single().lines.map { it.qty }).containsExactly(2.0, 3.0)
    }

    @Test
    fun submittingAgainAfterSuccessDoesNotRecordASecondReturn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(seedSale())

        viewModel.onSubmit()
        viewModel.onSubmit()

        assertThat(returns.recorded).hasSize(1)
    }

    @Test
    fun quantityAbovePurchasedIsRejectedBeforeRecording() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(seedSale())
        val firstItemId = viewModel.state.value.lines.first().transactionItemId

        viewModel.onReturnQtyChange(firstItemId, "99")
        viewModel.onSubmit()

        val state = viewModel.state.value
        assertThat(state.lineErrors).containsKey(firstItemId)
        assertThat(state.isProcessed).isFalse()
        assertThat(returns.recorded).isEmpty()
    }

    @Test
    fun blankQuantitySkipsThatItem() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(seedSale())
        val firstItemId = viewModel.state.value.lines.first().transactionItemId

        viewModel.onReturnQtyChange(firstItemId, "")
        viewModel.onSubmit()

        assertThat(viewModel.state.value.isProcessed).isTrue()
        assertThat(returns.recorded.single().lines).hasSize(1)
    }

    @Test
    fun clearingEveryQuantityBlocksSubmission() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(seedSale())

        viewModel.state.value.lines.forEach { line -> viewModel.onReturnQtyChange(line.transactionItemId, "") }
        viewModel.onSubmit()

        val state = viewModel.state.value
        assertThat(state.formError).isNotNull()
        assertThat(state.isProcessed).isFalse()
        assertThat(returns.recorded).isEmpty()
    }

    @Test
    fun reshowingTheSameTransactionKeepsAnInProgressForm() = runTest(mainDispatcherRule.testDispatcher) {
        val transactionId = seedSale()
        val viewModel = createViewModel(transactionId)
        val firstItemId = viewModel.state.value.lines.first().transactionItemId
        viewModel.onReturnQtyChange(firstItemId, "1")
        viewModel.onNoteChange("damaged")

        viewModel.onScreenShown(transactionId)

        assertThat(viewModel.state.value.lines.first().returnQty).isEqualTo("1")
        assertThat(viewModel.state.value.note).isEqualTo("damaged")
    }

    @Test
    fun showingADifferentTransactionReloadsTheForm() = runTest(mainDispatcherRule.testDispatcher) {
        val firstTransactionId = seedSale()
        val viewModel = createViewModel(firstTransactionId)
        val firstItemId = viewModel.state.value.lines.first().transactionItemId
        viewModel.onReturnQtyChange(firstItemId, "1")

        val secondTransactionId = seedSale()
        viewModel.onScreenShown(secondTransactionId)

        assertThat(viewModel.state.value.lines.first().returnQty).isEqualTo("2")
    }

    @Test
    fun reopeningAfterAProcessedReturnStartsFresh() = runTest(mainDispatcherRule.testDispatcher) {
        val transactionId = seedSale()
        val viewModel = createViewModel(transactionId)

        viewModel.onSubmit()
        assertThat(viewModel.state.value.isProcessed).isTrue()

        viewModel.onProcessed()
        viewModel.onScreenShown(transactionId)

        val state = viewModel.state.value
        assertThat(state.isProcessed).isFalse()
        assertThat(state.lines.map { it.returnQty }).containsExactly("2", "3")
    }
}
