package id.umkm.pos.pro.ui.customers

fun normalizeWhatsappNumber(rawNumber: String): String? {
    val digits = rawNumber.filter { character -> character.isDigit() }
    if (digits.isEmpty()) return null
    return when {
        digits.startsWith("62") -> digits
        digits.startsWith("0") -> "62" + digits.drop(1)
        digits.startsWith("8") -> "62" + digits
        else -> digits
    }
}
