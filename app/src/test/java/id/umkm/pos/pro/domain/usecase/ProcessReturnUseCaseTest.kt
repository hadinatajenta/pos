package id.umkm.pos.pro.domain.usecase

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.pos.ReturnLineRequest
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProcessReturnUseCaseTest {

    private val clock = FakeClock(9_000L)
    private val products = FakeProductRepository().addProduct(sampleProduct(id = 1L))
    private val transactions = FakeTransactionRepository()
    private val returns = FakeReturnRepository(transactions)
    private val checkout = CheckoutUseCase(products, transactions, DefaultPriceEngine(), clock)
    private val useCase = ProcessReturnUseCase(transactions, returns, products, clock)

    private suspend fun seedSale(): Long {
        val result = checkout(
            CheckoutRequest(
                lines = listOf(CheckoutLine(1L, null, 3.0, null, 0L)),
                customerId = null,
                paymentMethod = PaymentMethod.CASH,
                paidAmount = 6_000L,
                dueDate = null,
                invoiceDiscount = 0L,
                taxRatePercent = 0,
                roundingIncrement = 0L,
                notes = null,
            ),
        )
        return (result as AppResult.Success<CheckoutResult>).value.transactionId
    }

    @Test
    fun partialReturnRestocksQuantityHppAndRefund() = runTest {
        val transactionId = seedSale()
        val itemId = transactions.getItems(transactionId).single().id

        val result = useCase(transactionId, listOf(ReturnLineRequest(itemId, 1.0, 1.0)), "damaged")

        val record = (result as AppResult.Success<ReturnRecord>).value
        assertThat(record.transactionId).isEqualTo(transactionId)
        assertThat(record.refundAmount).isEqualTo(2_000L)
        assertThat(record.totalHppReversed).isEqualTo(1_000L)

        val recorded = returns.recorded.single()
        assertThat(recorded.lines.single().baseQtyRestocked).isEqualTo(1.0)
        assertThat(recorded.lines.single().conversionFactor).isEqualTo(1.0)
        assertThat(transactions.getTransaction(transactionId)!!.status).isEqualTo(TransactionStatus.RETURNED)
    }

    @Test
    fun returnBeyondPurchasedQuantityFailsValidation() = runTest {
        val transactionId = seedSale()
        val itemId = transactions.getItems(transactionId).single().id

        val result = useCase(transactionId, listOf(ReturnLineRequest(itemId, 99.0, 1.0)), null)

        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.Validation::class.java)
        assertThat(returns.recorded).isEmpty()
    }

    @Test
    fun emptyReturnFailsValidation() = runTest {
        val transactionId = seedSale()

        val result = useCase(transactionId, emptyList(), null)

        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun unknownTransactionFailsNotFound() = runTest {
        val result = useCase(4_242L, listOf(ReturnLineRequest(1L, 1.0, 1.0)), null)

        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.NotFound::class.java)
    }
}
