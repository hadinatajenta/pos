package id.umkm.pos.pro.feature.reports

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ReportDates {

    const val MILLIS_PER_DAY: Long = 86_400_000L

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    fun formatEpochDay(epochDay: Long): String =
        newFormatter("yyyy-MM-dd", UTC).format(Date(epochDay * MILLIS_PER_DAY))

    fun formatTimestamp(millis: Long, timeZone: TimeZone): String =
        newFormatter("yyyy-MM-dd HH:mm:ss", timeZone).format(Date(millis))

    fun formatLocalDate(millis: Long, timeZone: TimeZone): String =
        newFormatter("yyyy-MM-dd", timeZone).format(Date(millis))

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
}
