package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.TransactionStatus
import org.junit.Test

class DebtApplicationTest {

    @Test
    fun partialPaymentReducesRemainingDebt() {
        val transaction = sampleTransaction(remainingDebt = 100_000L, status = TransactionStatus.PARTIAL_DEBT)

        val result = applyDebtPayment(transaction, 40_000L)

        val application = (result as AppResult.Success<DebtPaymentApplication>).value
        assertThat(application.previousRemainingDebt).isEqualTo(100_000L)
        assertThat(application.appliedAmount).isEqualTo(40_000L)
        assertThat(application.newRemainingDebt).isEqualTo(60_000L)
        assertThat(application.statusAfterPayment).isEqualTo(TransactionStatus.PARTIAL_DEBT)
    }

    @Test
    fun finalPaymentCompletesTransaction() {
        val transaction = sampleTransaction(remainingDebt = 60_000L, status = TransactionStatus.PARTIAL_DEBT)

        val result = applyDebtPayment(transaction, 60_000L)

        val application = (result as AppResult.Success<DebtPaymentApplication>).value
        assertThat(application.newRemainingDebt).isEqualTo(0L)
        assertThat(application.statusAfterPayment).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun paymentExceedingOutstandingIsRejected() {
        val transaction = sampleTransaction(remainingDebt = 60_000L, status = TransactionStatus.PARTIAL_DEBT)

        val result = applyDebtPayment(transaction, 70_000L)

        val failure = (result as AppResult.Failure)
        assertThat(failure.error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun zeroPaymentIsRejected() {
        val transaction = sampleTransaction(remainingDebt = 60_000L, status = TransactionStatus.PARTIAL_DEBT)

        val result = applyDebtPayment(transaction, 0L)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun settledTransactionIsRejected() {
        val transaction = sampleTransaction(remainingDebt = 0L, status = TransactionStatus.COMPLETED)

        val result = applyDebtPayment(transaction, 10_000L)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun buildDebtPaymentRequiresCustomer() {
        val transaction = sampleTransaction(customerId = null, remainingDebt = 60_000L, status = TransactionStatus.PARTIAL_DEBT)

        val result = buildDebtPayment(
            transaction = transaction,
            paymentId = 5L,
            amount = 20_000L,
            paymentMethod = PaymentMethod.CASH,
            paidAtMillis = 1_700_000_000_000L,
            note = null,
        )

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun buildDebtPaymentUsesTransactionCustomer() {
        val transaction = sampleTransaction(id = 9L, customerId = 7L, remainingDebt = 60_000L, status = TransactionStatus.PARTIAL_DEBT)

        val result = buildDebtPayment(
            transaction = transaction,
            paymentId = 5L,
            amount = 20_000L,
            paymentMethod = PaymentMethod.TRANSFER,
            paidAtMillis = 1_700_000_000_000L,
            note = "angsuran",
        )

        val payment = (result as AppResult.Success<DebtPayment>).value
        assertThat(payment.id).isEqualTo(5L)
        assertThat(payment.transactionId).isEqualTo(9L)
        assertThat(payment.customerId).isEqualTo(7L)
        assertThat(payment.amount).isEqualTo(20_000L)
        assertThat(payment.paymentMethod).isEqualTo(PaymentMethod.TRANSFER)
        assertThat(payment.note).isEqualTo("angsuran")
    }

    @Test
    fun buildDebtPaymentRejectsNonPositiveAmount() {
        val transaction = sampleTransaction(remainingDebt = 60_000L, status = TransactionStatus.PARTIAL_DEBT)

        val result = buildDebtPayment(
            transaction = transaction,
            paymentId = 5L,
            amount = 0L,
            paymentMethod = PaymentMethod.CASH,
            paidAtMillis = 1_700_000_000_000L,
            note = null,
        )

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }
}
