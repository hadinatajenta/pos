package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.components.ChartBuckets
import id.umkm.pos.pro.ui.components.ChartPoint
import java.time.ZoneId

internal fun averageTransactionOf(summary: SalesSummary): Long =
    if (summary.transactionCount > 0) summary.totalSales / summary.transactionCount else 0L

internal fun chartPointsFor(
    range: ReportRange,
    dailySales: List<DailySales>,
    zoneId: ZoneId,
    strings: PosStrings,
): List<ChartPoint> {
    if (dailySales.none { day -> day.totalSales > 0L }) return emptyList()
    val buckets = ChartBuckets.dailyZeroFilled(range, dailySales, zoneId)
    val isWeekdayLabel = buckets.size <= WEEKDAY_LABEL_MAX_BUCKETS
    return buckets.map { day ->
        ChartPoint(
            label = if (isWeekdayLabel) {
                ChartBuckets.weekdayLabel(day.epochDay, strings)
            } else {
                ChartBuckets.dayOfMonthLabel(day.epochDay)
            },
            value = day.totalSales.toDouble(),
        )
    }
}

internal fun paymentShares(transactions: List<Transaction>): List<ReportsPaymentShare> =
    transactions.groupBy { transaction -> transaction.paymentMethod }
        .map { (method, group) ->
            ReportsPaymentShare(
                paymentMethod = method,
                amount = group.sumOf { transaction -> transaction.grandTotal },
                transactionCount = group.size,
            )
        }
        .sortedByDescending { share -> share.amount }

private const val WEEKDAY_LABEL_MAX_BUCKETS = 7
