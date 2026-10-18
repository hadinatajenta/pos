package id.umkm.pos.pro.ui.components

import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.ui.common.PosStrings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ChartBuckets {

    val DEFAULT_ZONE: ZoneId = ZoneId.systemDefault()

    fun dailyZeroFilled(
        range: ReportRange,
        dailySales: List<DailySales>,
        zone: ZoneId = DEFAULT_ZONE,
        maxBuckets: Int = 62,
    ): List<DailySales> {
        val salesByDay = dailySales.associateBy { it.epochDay }
        val firstDay = LocalDate.ofInstant(Instant.ofEpochMilli(range.startMillis), zone).toEpochDay()
        val lastDay = LocalDate.ofInstant(Instant.ofEpochMilli(range.endMillis), zone).toEpochDay()
        if (lastDay < firstDay) return emptyList()
        val startDay = maxOf(firstDay, lastDay - (maxBuckets - 1))
        return (startDay..lastDay).map { epochDay ->
            salesByDay[epochDay] ?: DailySales(
                epochDay = epochDay,
                totalSales = 0L,
                netProfit = 0L,
                transactionCount = 0,
            )
        }
    }

    fun weekdayLabel(epochDay: Long, strings: PosStrings): String =
        strings.dateWeekdayAbbreviations[LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1]

    fun dayOfMonthLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).dayOfMonth.toString()
}
