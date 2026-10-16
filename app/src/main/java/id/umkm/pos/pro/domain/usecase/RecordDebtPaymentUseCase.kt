package id.umkm.pos.pro.domain.usecase

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.domain.pos.applyDebtPayment
import id.umkm.pos.pro.domain.pos.buildDebtPayment

class RecordDebtPaymentUseCase(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        transactionId: Long,
        amount: Long,
        paymentMethod: PaymentMethod,
        note: String?,
    ): AppResult<Long> {
        val transaction = transactionRepository.getTransaction(transactionId)
            ?: return AppResult.Failure(AppError.NotFound("transaction $transactionId was not found"))

        return when (val application = applyDebtPayment(transaction, amount)) {
            is AppResult.Failure -> application
            is AppResult.Success -> when (
                val payment = buildDebtPayment(
                    transaction = transaction,
                    paymentId = 0L,
                    amount = application.value.appliedAmount,
                    paymentMethod = paymentMethod,
                    paidAtMillis = clock.nowMillis(),
                    note = note,
                )
            ) {
                is AppResult.Failure -> payment
                is AppResult.Success -> transactionRepository.recordDebtPayment(payment.value)
            }
        }
    }
}
