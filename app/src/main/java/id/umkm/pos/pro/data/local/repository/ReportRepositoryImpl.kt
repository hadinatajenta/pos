package id.umkm.pos.pro.data.local.repository

import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.data.local.dao.DailySalesAggregate
import id.umkm.pos.pro.data.local.dao.ReportDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.mapper.TransactionMapper

class ReportRepositoryImpl(
    private val reportDao: ReportDao,
    private val transactionDao: TransactionDao,
) : ReportRepository {

    override suspend fun salesSummary(range: ReportRange): SalesSummary {
        val transactions = reportDao.transactionTotals(range.startMillis, range.endMillis)
        val returns = reportDao.returnsTotals(range.startMillis, range.endMillis)
        return SalesSummary(
            totalSales = transactions.totalSales - returns.refundTotal,
            totalHpp = transactions.totalHpp - returns.hppReversedTotal,
            netProfit = transactions.netProfit - (returns.refundTotal - returns.hppReversedTotal),
            transactionCount = transactions.transactionCount,
        )
    }

    override suspend fun salesByDay(range: ReportRange): List<DailySales> {
        val transactionsByDay = reportDao.transactionTotalsByDay(range.startMillis, range.endMillis)
            .associateBy(DailySalesAggregate::epochDay)
        val returnsByDay = reportDao.returnsTotalsByDay(range.startMillis, range.endMillis)
            .associateBy { aggregate -> aggregate.epochDay }
        val epochDays = (transactionsByDay.keys + returnsByDay.keys).sorted()
        return epochDays.map { epochDay ->
            val transactions = transactionsByDay[epochDay]
            val returns = returnsByDay[epochDay]
            DailySales(
                epochDay = epochDay,
                totalSales = (transactions?.totalSales ?: 0L) - (returns?.refundTotal ?: 0L),
                netProfit = (transactions?.netProfit ?: 0L) - (returns?.netRefund ?: 0L),
                transactionCount = transactions?.transactionCount ?: 0,
            )
        }
    }

    override suspend fun transactionsInRange(range: ReportRange): List<Transaction> =
        transactionDao.transactionsInRange(range.startMillis, range.endMillis)
            .map(TransactionMapper::toModel)
}
