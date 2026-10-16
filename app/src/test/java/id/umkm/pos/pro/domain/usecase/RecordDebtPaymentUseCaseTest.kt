package id.umkm.pos.pro.domain.usecase

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecordDebtPaymentUseCaseTest {

    private val clock = FakeClock(12_000L)
    private val transactions = FakeTransactionRepository()
    private val useCase = RecordDebtPaymentUseCase(transactions, clock)

    private suspend fun seedDebtSale(): Long {
        val transaction = Transaction(
            id = 0L,
            invoiceNo = "INV-1",
            createdAt = 1L,
            cashierId = null,
            customerId = 3L,
            subtotal = 6_000L,
            discountTotal = 0L,
            invoiceDiscount = 0L,
            taxTotal = 0L,
            rounding = 0L,
            grandTotal = 6_000L,
            totalHpp = 3_000L,
            netProfit = 3_000L,
            paymentMethod = PaymentMethod.KASBON,
            paidAmount = 1_000L,
            changeAmount = 0L,
            remainingDebt = 5_000L,
            dueDate = null,
            status = TransactionStatus.PARTIAL_DEBT,
            notes = null,
        )
        val item = TransactionItem(0L, 0L, 1L, null, 3.0, 1_000L, 2_000L, false, false, 0L, 6_000L)
        return (transactions.saveTransaction(transaction, listOf(item)) as AppResult.Success<Long>).value
    }

    @Test
    fun fullPaymentClearsDebtAndCompletes() = runTest {
        val transactionId = seedDebtSale()

        val result = useCase(transactionId, 5_000L, PaymentMethod.CASH, null)

        assertThat((result as AppResult.Success<Long>).value).isEqualTo(1L)
        val updated = transactions.getTransaction(transactionId)!!
        assertThat(updated.remainingDebt).isEqualTo(0L)
        assertThat(updated.status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun partialPaymentKeepsPartialDebt() = runTest {
        val transactionId = seedDebtSale()

        useCase(transactionId, 2_000L, PaymentMethod.QRIS, "installment")

        val updated = transactions.getTransaction(transactionId)!!
        assertThat(updated.remainingDebt).isEqualTo(3_000L)
        assertThat(updated.status).isEqualTo(TransactionStatus.PARTIAL_DEBT)
    }

    @Test
    fun overpaymentFailsValidation() = runTest {
        val transactionId = seedDebtSale()

        val result = useCase(transactionId, 9_999L, PaymentMethod.CASH, null)

        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.Validation::class.java)
        assertThat(transactions.getTransaction(transactionId)!!.remainingDebt).isEqualTo(5_000L)
    }

    @Test
    fun unknownTransactionFailsNotFound() = runTest {
        val result = useCase(999L, 1_000L, PaymentMethod.CASH, null)

        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.NotFound::class.java)
    }
}
