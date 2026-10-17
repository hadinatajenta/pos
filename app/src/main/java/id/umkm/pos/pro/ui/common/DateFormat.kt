package id.umkm.pos.pro.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MONTH_LABELS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

private const val TIME_PATTERN = "HH.mm"

fun formatDate(epochMillis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = MONTH_LABELS[calendar.get(Calendar.MONTH)]
    val year = calendar.get(Calendar.YEAR)
    return "$day $month $year"
}

fun formatDateTime(epochMillis: Long): String =
    formatDate(epochMillis) + ", " + SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date(epochMillis))
