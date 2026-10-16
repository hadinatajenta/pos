package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.TransactionStatus

data class PaymentAllocation(
    val paymentMethod: PaymentMethod,
    val paidAmount: Long,
    val changeAmount: Long,
    val remainingDebt: Long,
    val status: TransactionStatus,
)

fun allocatePayment(paymentMethod: PaymentMethod, paidAmount: Long, grandTotal: Long): PaymentAllocation {
    val payableTotal = grandTotal.coerceAtLeast(0L)
    val tenderedAmount = paidAmount.coerceAtLeast(0L)

    if (tenderedAmount >= payableTotal) {
        val isCash = paymentMethod == PaymentMethod.CASH
        return PaymentAllocation(
            paymentMethod = paymentMethod,
            paidAmount = if (isCash) tenderedAmount else payableTotal,
            changeAmount = if (isCash) tenderedAmount - payableTotal else 0L,
            remainingDebt = 0L,
            status = TransactionStatus.COMPLETED,
        )
    }

    return PaymentAllocation(
        paymentMethod = paymentMethod,
        paidAmount = tenderedAmount,
        changeAmount = 0L,
        remainingDebt = payableTotal - tenderedAmount,
        status = TransactionStatus.PARTIAL_DEBT,
    )
}
