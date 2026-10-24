package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.model.PaymentMethod

data class PaymentAllocation(
    val paymentMethod: PaymentMethod,
    val paidAmount: Long,
    val changeAmount: Long,
)

fun allocatePayment(paymentMethod: PaymentMethod, paidAmount: Long, grandTotal: Long): PaymentAllocation {
    val payableTotal = grandTotal.coerceAtLeast(0L)
    val tenderedAmount = paidAmount.coerceAtLeast(0L)
    val isCash = paymentMethod == PaymentMethod.CASH
    return PaymentAllocation(
        paymentMethod = paymentMethod,
        paidAmount = if (isCash) tenderedAmount else payableTotal,
        changeAmount = if (isCash) (tenderedAmount - payableTotal).coerceAtLeast(0L) else 0L,
    )
}
