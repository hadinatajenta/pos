package id.umkm.pos.pro.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TIME_PATTERN = "HH.mm"

fun formatDate(epochMillis: Long, strings: PosStrings): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = strings.dateMonthAbbreviations[calendar.get(Calendar.MONTH)]
    val year = calendar.get(Calendar.YEAR)
    return "$day $month $year"
}

fun formatDateTime(epochMillis: Long, strings: PosStrings): String =
    formatDate(epochMillis, strings) + ", " + SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date(epochMillis))
