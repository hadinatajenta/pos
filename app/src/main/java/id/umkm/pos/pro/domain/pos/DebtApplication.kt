package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus

data class DebtPaymentApplication(
    val previousRemainingDebt: Long,
    val appliedAmount: Long,
    val newRemainingDebt: Long,
    val statusAfterPayment: TransactionStatus,
)

fun applyDebtPayment(transaction: Transaction, paymentAmount: Long): AppResult<DebtPaymentApplication> {
    val outstandingDebt = transaction.remainingDebt
    if (outstandingDebt <= 0L) {
        return AppResult.Failure(
            AppError.Validation("transaction ${transaction.invoiceNo} has no outstanding balance"),
        )
    }
    if (paymentAmount <= 0L) {
        return AppResult.Failure(AppError.Validation("debt payment amount must be positive"))
    }
    if (paymentAmount > outstandingDebt) {
        return AppResult.Failure(AppError.Validation("debt payment exceeds outstanding balance"))
    }

    val newRemainingDebt = outstandingDebt - paymentAmount
    val statusAfterPayment = if (newRemainingDebt == 0L) TransactionStatus.COMPLETED else TransactionStatus.PARTIAL_DEBT

    return AppResult.Success(
        DebtPaymentApplication(
            previousRemainingDebt = outstandingDebt,
            appliedAmount = paymentAmount,
            newRemainingDebt = newRemainingDebt,
            statusAfterPayment = statusAfterPayment,
        ),
    )
}

fun buildDebtPayment(
    transaction: Transaction,
    paymentId: Long,
    amount: Long,
    paymentMethod: PaymentMethod,
    paidAtMillis: Long,
    note: String?,
): AppResult<DebtPayment> {
    val customerId = transaction.customerId
        ?: return AppResult.Failure(
            AppError.Validation("debt payment requires a customer on transaction ${transaction.invoiceNo}"),
        )
    if (amount <= 0L) {
        return AppResult.Failure(AppError.Validation("debt payment amount must be positive"))
    }

    return AppResult.Success(
        DebtPayment(
            id = paymentId,
            transactionId = transaction.id,
            customerId = customerId,
            paidAt = paidAtMillis,
            amount = amount,
            paymentMethod = paymentMethod,
            note = note,
        ),
    )
}
