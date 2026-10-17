package id.umkm.pos.pro.domain.qris

data class QrisField(
    val tag: String,
    val value: String,
)

object QrisPayload {

    private const val PAYLOAD_FORMAT_TAG = "00"
    private const val CURRENCY_TAG = "53"
    private const val AMOUNT_TAG = "54"
    private const val CRC_TAG = "63"
    private const val RUPIAH_CURRENCY = "360"
    private const val CRC_LENGTH = 4
    private const val STATIC_MARKER = "000201"

    fun isValid(payload: String): Boolean {
        if (!payload.startsWith(STATIC_MARKER)) return false
        val fields = parse(payload) ?: return false
        val currency = fields.firstOrNull { field -> field.tag == CURRENCY_TAG }?.value ?: return false
        if (currency != RUPIAH_CURRENCY) return false
        val crcField = fields.lastOrNull() ?: return false
        if (crcField.tag != CRC_TAG || crcField.value.length != CRC_LENGTH) return false
        val body = payload.dropLast(CRC_LENGTH)
        return crc16(body).toString(16).uppercase().padStart(CRC_LENGTH, '0') == crcField.value.uppercase()
    }

    fun withAmount(payload: String, amountRupiah: Long): String? {
        if (amountRupiah <= 0L) return null
        val fields = parse(payload) ?: return null
        if (fields.lastOrNull()?.tag != CRC_TAG) return null
        val contentFields = fields.filter { field -> field.tag != CRC_TAG && field.tag != AMOUNT_TAG }
        val amountField = QrisField(AMOUNT_TAG, amountRupiah.toString())
        val merged = (contentFields + amountField).sortedBy { field -> field.tag }
        val body = merged.joinToString(separator = "") { field -> encode(field) } + CRC_TAG + "04"
        val checksum = crc16(body).toString(16).uppercase().padStart(CRC_LENGTH, '0')
        return body + checksum
    }

    fun amountOf(payload: String): Long? {
        val fields = parse(payload) ?: return null
        val raw = fields.firstOrNull { field -> field.tag == AMOUNT_TAG }?.value ?: return null
        val normalized = raw.trim()
        if (normalized.isEmpty()) return null
        val parts = normalized.split('.')
        if (parts.size > 2) return null
        val whole = parts[0].toLongOrNull() ?: return null
        val fraction = parts.getOrNull(1).orEmpty()
        if (fraction.any { character -> !character.isDigit() }) return null
        if (fraction.take(2).toLongOrNull()?.let { value -> value != 0L } == true) return null
        return whole
    }

    internal fun crc16(payload: String): Int {
        var crc = 0xFFFF
        for (byte in payload.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }

    private fun parse(payload: String): List<QrisField>? {
        if (payload.length < 4) return null
        val fields = mutableListOf<QrisField>()
        var index = 0
        while (index < payload.length) {
            if (index + 4 > payload.length) return null
            val tag = payload.substring(index, index + 2)
            val length = payload.substring(index + 2, index + 4).toIntOrNull() ?: return null
            val valueStart = index + 4
            val valueEnd = valueStart + length
            if (length < 0 || valueEnd > payload.length) return null
            fields.add(QrisField(tag = tag, value = payload.substring(valueStart, valueEnd)))
            index = valueEnd
        }
        return fields
    }

    private fun encode(field: QrisField): String =
        field.tag + field.value.length.toString().padStart(2, '0') + field.value
}
