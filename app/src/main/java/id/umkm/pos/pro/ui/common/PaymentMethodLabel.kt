package id.umkm.pos.pro.ui.common

import id.umkm.pos.pro.core.model.PaymentMethod

fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.QRIS -> "QRIS"
    PaymentMethod.TRANSFER -> "Transfer"
    PaymentMethod.SPLIT -> "Split"
    PaymentMethod.KASBON -> "Kasbon"
}
