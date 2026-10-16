package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.ui.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.TimeZone

class ReportsPresenterReturnTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val utc = TimeZone.getTimeZone("UTC")
    private val clock = FixedClock(1_705_320_000_000L)
    private val exportDirectory = File("/tmp/umkm-pos-reports-tests")

    @Test
    fun drillDownExposesTransactionIdForReturn() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = RecordingReportRepository(
            transactions = listOf(sampleTransaction(id = 42L, invoiceNo = "INV-42")),
        )
        val presenter = createPresenter(repository, this)

        presenter.start()
        presenter.openSummaryDrillDown()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(presenter.uiState.value.drillDown?.transactions?.map { transaction -> transaction.id })
            .containsExactly(42L)

        presenter.requestReturn(42L)

        assertThat(presenter.uiState.value.pendingReturnTransactionId).isEqualTo(42L)
    }

    @Test
    fun dismissReturnClearsPendingReturn() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(RecordingReportRepository(), this)

        presenter.start()
        presenter.requestReturn(7L)
        presenter.dismissReturn()

        assertThat(presenter.uiState.value.pendingReturnTransactionId).isNull()
    }

    @Test
    fun processingAReturnClearsPendingAndReloadsNettedTotals() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = RecordingReportRepository(
            summary = SalesSummary(
                totalSales = 20_000L,
                totalHpp = 10_000L,
                netProfit = 5_000L,
                transactionCount = 1,
            ),
            transactions = listOf(sampleTransaction(id = 42L)),
        )
        val presenter = createPresenter(repository, this)

        presenter.start()
        presenter.openSummaryDrillDown()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.requestReturn(42L)

        repository.summary = SalesSummary(
            totalSales = 12_000L,
            totalHpp = 6_000L,
            netProfit = 3_000L,
            transactionCount = 1,
        )
        repository.dailySales = listOf(
            DailySales(epochDay = 19_718L, totalSales = 12_000L, netProfit = 3_000L, transactionCount = 1),
        )

        presenter.onReturnProcessed()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = presenter.uiState.value
        assertThat(state.pendingReturnTransactionId).isNull()
        assertThat(state.drillDown).isNull()
        assertThat(state.summary).isEqualTo(repository.summary)
        assertThat(state.dailySales).isEqualTo(repository.dailySales)
        assertThat(repository.summaryReadCount).isEqualTo(2)
        assertThat(repository.dailySalesReadCount).isEqualTo(2)
        assertThat(repository.transactionsReadCount).isEqualTo(3)
    }

    @Test
    fun loadsPaymentSharesFromNettedTransactions() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = RecordingReportRepository(
            summary = SalesSummary(
                totalSales = 62_000L,
                totalHpp = 0L,
                netProfit = 0L,
                transactionCount = 3,
            ),
            transactions = listOf(
                sampleTransaction(id = 1L, paymentMethod = PaymentMethod.CASH, grandTotal = 30_000L),
                sampleTransaction(id = 2L, paymentMethod = PaymentMethod.CASH, grandTotal = 20_000L),
                sampleTransaction(id = 3L, paymentMethod = PaymentMethod.QRIS, grandTotal = 12_000L),
            ),
        )
        val presenter = createPresenter(repository, this)

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val shares = presenter.uiState.value.paymentShares
        assertThat(shares).hasSize(2)
        assertThat(shares[0].paymentMethod).isEqualTo(PaymentMethod.CASH)
        assertThat(shares[0].amount).isEqualTo(50_000L)
        assertThat(shares[0].transactionCount).isEqualTo(2)
        assertThat(shares[1].paymentMethod).isEqualTo(PaymentMethod.QRIS)
        assertThat(shares[1].amount).isEqualTo(12_000L)
        assertThat(shares[1].transactionCount).isEqualTo(1)
        assertThat(presenter.uiState.value.averageTransaction).isEqualTo(20_666L)
    }

    @Test
    fun hasNoPaymentSharesWhenRangeHasNoTransactions() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(RecordingReportRepository(), this)

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(presenter.uiState.value.paymentShares).isEmpty()
        assertThat(presenter.uiState.value.averageTransaction).isEqualTo(0L)
    }

    private fun createPresenter(
        repository: ReportRepository,
        coroutineScope: CoroutineScope,
    ): ReportsPresenter = ReportsPresenter(
        reportRepository = repository,
        csvExporter = SalesCsvExporter(repository, RecordingCsvSink(), utc),
        exportDirectory = exportDirectory,
        coroutineScope = coroutineScope,
        clock = clock,
        timeZone = utc,
    )
}

private class RecordingReportRepository(
    var summary: SalesSummary = SalesSummary(0L, 0L, 0L, 0),
    var dailySales: List<DailySales> = emptyList(),
    var transactions: List<Transaction> = emptyList(),
) : ReportRepository {

    var summaryReadCount = 0
    var dailySalesReadCount = 0
    var transactionsReadCount = 0

    override suspend fun salesSummary(range: ReportRange): SalesSummary {
        summaryReadCount++
        return summary
    }

    override suspend fun salesByDay(range: ReportRange): List<DailySales> {
        dailySalesReadCount++
        return dailySales
    }

    override suspend fun transactionsInRange(range: ReportRange): List<Transaction> {
        transactionsReadCount++
        return transactions
    }
}
