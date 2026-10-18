package id.umkm.pos.pro.ui.common

import id.umkm.pos.pro.core.model.PaymentMethod

fun paymentMethodLabel(method: PaymentMethod, strings: PosStrings): String = when (method) {
    PaymentMethod.CASH -> strings.paymentCash
    PaymentMethod.QRIS -> strings.paymentQris
    PaymentMethod.TRANSFER -> strings.paymentTransfer
    PaymentMethod.SPLIT -> strings.paymentSplit
    PaymentMethod.KASBON -> strings.paymentKasbon
}
