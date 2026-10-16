package id.umkm.pos.pro.data.local.repository

import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.data.local.dao.ReportDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.mapper.TransactionMapper

class ReportRepositoryImpl(
    private val reportDao: ReportDao,
    private val transactionDao: TransactionDao,
) : ReportRepository {

    override suspend fun salesSummary(range: ReportRange): SalesSummary {
        val aggregate = reportDao.salesSummary(range.startMillis, range.endMillis)
        return SalesSummary(
            totalSales = aggregate.totalSales,
            totalHpp = aggregate.totalHpp,
            netProfit = aggregate.netProfit,
            transactionCount = aggregate.transactionCount,
        )
    }

    override suspend fun salesByDay(range: ReportRange): List<DailySales> =
        reportDao.salesByDay(range.startMillis, range.endMillis).map { aggregate ->
            DailySales(
                epochDay = aggregate.epochDay,
                totalSales = aggregate.totalSales,
                netProfit = aggregate.netProfit,
                transactionCount = aggregate.transactionCount,
            )
        }

    override suspend fun transactionsInRange(range: ReportRange): List<Transaction> =
        transactionDao.transactionsInRange(range.startMillis, range.endMillis)
            .map(TransactionMapper::toModel)
}
