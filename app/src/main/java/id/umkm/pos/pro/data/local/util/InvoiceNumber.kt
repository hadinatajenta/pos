package id.umkm.pos.pro.data.local.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object InvoiceNumber {

    private const val DAY_KEY_PATTERN = "yyyyMMdd"
    private const val SEQUENCE_WIDTH = 4

    fun dayKey(createdAtMillis: Long, timeZone: TimeZone): String =
        SimpleDateFormat(DAY_KEY_PATTERN, Locale.US)
            .apply { this.timeZone = timeZone }
            .format(Date(createdAtMillis))

    fun format(createdAtMillis: Long, sequence: Int, timeZone: TimeZone): String {
        val paddedSequence = sequence.toString().padStart(SEQUENCE_WIDTH, '0')
        return "INV-${dayKey(createdAtMillis, timeZone)}-$paddedSequence"
    }
}
