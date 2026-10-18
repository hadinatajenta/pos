package id.umkm.pos.pro.feature.reports

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import id.umkm.pos.pro.ui.common.PosStrings

object ReportDates {

    const val MILLIS_PER_DAY: Long = 86_400_000L

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    fun formatEpochDay(epochDay: Long): String =
        newFormatter("yyyy-MM-dd", UTC).format(Date(epochDay * MILLIS_PER_DAY))

    fun formatEpochDayShort(epochDay: Long, strings: PosStrings): String =
        formatShortDate(Calendar.getInstance(UTC).apply { timeInMillis = epochDay * MILLIS_PER_DAY }, strings)

    fun formatLocalDateShort(millis: Long, timeZone: TimeZone, strings: PosStrings): String =
        formatShortDate(Calendar.getInstance(timeZone).apply { timeInMillis = millis }, strings)

    fun weekdayShortLabel(epochDay: Long, strings: PosStrings): String =
        strings.dateWeekdayAbbreviations[Math.floorMod(epochDay + 3L, 7L).toInt()]

    fun formatTimestamp(millis: Long, timeZone: TimeZone): String =
        newFormatter("yyyy-MM-dd HH:mm:ss", timeZone).format(Date(millis))

    fun formatLocalDateTimeShort(millis: Long, timeZone: TimeZone, strings: PosStrings): String {
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        return String.format(
            Locale.US,
            "%02d %s %d %02d:%02d",
            calendar.get(Calendar.DAY_OF_MONTH),
            strings.dateMonthAbbreviations[calendar.get(Calendar.MONTH)],
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
        )
    }

    fun epochDayForLocalMillis(millis: Long, timeZone: TimeZone): Long {
        val local = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val utc = Calendar.getInstance(UTC).apply {
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0,
            )
            set(Calendar.MILLISECOND, 0)
        }
        return utc.timeInMillis / MILLIS_PER_DAY
    }

    private fun newFormatter(pattern: String, timeZone: TimeZone): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply { this.timeZone = timeZone }

    private fun formatShortDate(calendar: Calendar, strings: PosStrings): String = String.format(
        Locale.US,
        "%02d %s %d",
        calendar.get(Calendar.DAY_OF_MONTH),
        strings.dateMonthAbbreviations[calendar.get(Calendar.MONTH)],
        calendar.get(Calendar.YEAR),
    )
}
