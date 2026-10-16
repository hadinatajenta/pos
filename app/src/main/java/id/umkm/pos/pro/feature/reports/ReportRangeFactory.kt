package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.model.ReportRange
import java.util.Calendar
import java.util.TimeZone

class ReportRangeFactory(
    private val clock: Clock,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {

    fun today(): ReportRange {
        val epochDay = ReportDates.epochDayForLocalMillis(clock.nowMillis(), timeZone)
        return forEpochDayRange(epochDay, epochDay)
    }

    fun lastDays(dayCount: Int): ReportRange {
        val effectiveDays = dayCount.coerceAtLeast(1).toLong()
        val todayEpochDay = ReportDates.epochDayForLocalMillis(clock.nowMillis(), timeZone)
        return forEpochDayRange(todayEpochDay - (effectiveDays - 1), todayEpochDay)
    }

    fun thisMonth(): ReportRange {
        val now = clock.nowMillis()
        val start = localCalendar(now).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            resetTimeOfDay()
        }.timeInMillis
        val end = localCalendar(now).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            resetTimeOfDay()
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        return ReportRange(start, end)
    }

    fun thisYear(): ReportRange {
        val now = clock.nowMillis()
        val start = localCalendar(now).apply {
            set(Calendar.DAY_OF_YEAR, 1)
            resetTimeOfDay()
        }.timeInMillis
        val end = localCalendar(now).apply {
            set(Calendar.DAY_OF_YEAR, 1)
            resetTimeOfDay()
            add(Calendar.YEAR, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        return ReportRange(start, end)
    }

    fun allTime(): ReportRange = ReportRange(0L, clock.nowMillis())

    fun forEpochDay(epochDay: Long): ReportRange = forEpochDayRange(epochDay, epochDay)

    fun forEpochDayRange(startEpochDay: Long, endEpochDay: Long): ReportRange {
        val orderedStart = minOf(startEpochDay, endEpochDay)
        val orderedEnd = maxOf(startEpochDay, endEpochDay)
        return ReportRange(
            startMillis = startOfLocalDay(orderedStart),
            endMillis = startOfLocalDay(orderedEnd + 1) - 1,
        )
    }

    private fun startOfLocalDay(epochDay: Long): Long {
        val dateAtUtcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * ReportDates.MILLIS_PER_DAY
        }
        return localCalendar().apply {
            set(
                dateAtUtcMidnight.get(Calendar.YEAR),
                dateAtUtcMidnight.get(Calendar.MONTH),
                dateAtUtcMidnight.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0,
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun localCalendar(millis: Long? = null): Calendar =
        Calendar.getInstance(timeZone).apply { if (millis != null) timeInMillis = millis }

    private fun Calendar.resetTimeOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
