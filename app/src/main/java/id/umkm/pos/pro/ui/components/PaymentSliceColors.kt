package id.umkm.pos.pro.ui.components

import androidx.compose.ui.graphics.Color
import id.umkm.pos.pro.core.model.PaymentMethod

fun paymentSliceColor(method: PaymentMethod): Color = when (method) {
    PaymentMethod.CASH -> Color(0xFF1E6B54)
    PaymentMethod.QRIS -> Color(0xFF21845D)
    PaymentMethod.TRANSFER -> Color(0xFFE7C46A)
    PaymentMethod.SPLIT -> Color(0xFF7C8A83)
    PaymentMethod.KASBON -> Color(0xFFB87917)
}
