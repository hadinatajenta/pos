package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus

internal fun sampleTransaction(
    id: Long = 1L,
    invoiceNo: String = "INV-1",
    createdAt: Long = 1_705_276_800_000L,
    paymentMethod: PaymentMethod = PaymentMethod.CASH,
    status: TransactionStatus = TransactionStatus.COMPLETED,
    subtotal: Long = 15_000L,
    grandTotal: Long = 20_000L,
    totalHpp: Long = 10_000L,
    netProfit: Long = 5_000L,
): Transaction = Transaction(
    id = id,
    invoiceNo = invoiceNo,
    createdAt = createdAt,
    cashierId = null,
    customerId = null,
    subtotal = subtotal,
    discountTotal = 0L,
    invoiceDiscount = 0L,
    taxTotal = 0L,
    rounding = 0L,
    grandTotal = grandTotal,
    totalHpp = totalHpp,
    netProfit = netProfit,
    paymentMethod = paymentMethod,
    paidAmount = grandTotal,
    changeAmount = 0L,
    remainingDebt = 0L,
    dueDate = null,
    status = status,
    notes = null,
)

internal class FakeReportRepository(
    private val transactions: List<Transaction> = emptyList(),
    private val summary: SalesSummary = SalesSummary(0L, 0L, 0L, 0),
    private val dailySales: List<DailySales> = emptyList(),
    private val failRead: Boolean = false,
) : ReportRepository {

    override suspend fun salesSummary(range: ReportRange): SalesSummary = summary

    override suspend fun salesByDay(range: ReportRange): List<DailySales> = dailySales

    override suspend fun transactionsInRange(range: ReportRange): List<Transaction> {
        if (failRead) throw IllegalStateException("report store unavailable")
        return transactions
    }
}

internal class RecordingCsvSink(private val failWrite: Boolean = false) : CsvSink {
    var writtenPath: String? = null
    var writtenContent: String? = null

    override suspend fun write(targetPath: String, content: String): Long {
        if (failWrite) throw IllegalStateException("disk full")
        writtenPath = targetPath
        writtenContent = content
        return content.toByteArray(Charsets.UTF_8).size.toLong()
    }
}

internal class FixedClock(private val now: Long) : Clock {
    override fun nowMillis(): Long = now
}
