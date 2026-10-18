package id.umkm.pos.pro.ui.components

import androidx.compose.ui.graphics.Color
import id.umkm.pos.pro.core.model.PaymentMethod

fun paymentSliceColor(method: PaymentMethod): Color = when (method) {
    PaymentMethod.CASH -> Color(0xFF0D5C46)
    PaymentMethod.QRIS -> Color(0xFF1D6FA5)
    PaymentMethod.TRANSFER -> Color(0xFF6C4AB6)
    PaymentMethod.SPLIT -> Color(0xFF3B82A6)
    PaymentMethod.KASBON -> Color(0xFFD97706)
}
