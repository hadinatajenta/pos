package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.data.local.entity.DebtPaymentEntity

object DebtPaymentMapper {

    fun toEntity(payment: DebtPayment): DebtPaymentEntity = DebtPaymentEntity(
        id = payment.id,
        transactionId = payment.transactionId,
        customerId = payment.customerId,
        paidAt = payment.paidAt,
        amount = payment.amount,
        paymentMethod = payment.paymentMethod,
        note = payment.note,
    )

    fun toModel(entity: DebtPaymentEntity): DebtPayment = DebtPayment(
        id = entity.id,
        transactionId = entity.transactionId,
        customerId = entity.customerId,
        paidAt = entity.paidAt,
        amount = entity.amount,
        paymentMethod = entity.paymentMethod,
        note = entity.note,
    )
}
