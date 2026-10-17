package id.umkm.pos.pro.feature.reports

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ReportDates {

    const val MILLIS_PER_DAY: Long = 86_400_000L

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    private val MONTH_LABELS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
    )

    private val WEEKDAY_LABELS = arrayOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    fun formatEpochDay(epochDay: Long): String =
        newFormatter("yyyy-MM-dd", UTC).format(Date(epochDay * MILLIS_PER_DAY))

    fun formatEpochDayShort(epochDay: Long): String =
        formatShortDate(Calendar.getInstance(UTC).apply { timeInMillis = epochDay * MILLIS_PER_DAY })

    fun formatLocalDateShort(millis: Long, timeZone: TimeZone): String =
        formatShortDate(Calendar.getInstance(timeZone).apply { timeInMillis = millis })

    fun weekdayShortLabel(epochDay: Long): String =
        WEEKDAY_LABELS[Math.floorMod(epochDay + 3L, 7L).toInt()]

    fun formatTimestamp(millis: Long, timeZone: TimeZone): String =
        newFormatter("yyyy-MM-dd HH:mm:ss", timeZone).format(Date(millis))

    fun formatLocalDateTimeShort(millis: Long, timeZone: TimeZone): String =
        newFormatter("dd MMM yyyy HH:mm", timeZone).format(Date(millis))

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

    private fun formatShortDate(calendar: Calendar): String = String.format(
        Locale.US,
        "%02d %s %d",
        calendar.get(Calendar.DAY_OF_MONTH),
        MONTH_LABELS[calendar.get(Calendar.MONTH)],
        calendar.get(Calendar.YEAR),
    )
}
