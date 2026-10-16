package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.TimeZone

class ReportsPresenterExportTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val utc = TimeZone.getTimeZone("UTC")
    private val clock = FixedClock(1_705_320_000_000L)
    private val exportDirectory = File("/tmp/umkm-pos-reports-export-tests")

    @Test
    fun csvExportWritesTransactionsAndProductSummary() = runTest(mainDispatcherRule.testDispatcher) {
        val csvSink = RecordingCsvSink()
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction(id = 1L))),
            transactionRepository = FakeTransactionRepository(mapOf(1L to listOf(sampleTransactionItem()))),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct())),
            csvSink = csvSink,
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.exportReport(ReportExportFormat.CSV)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = presenter.uiState.value.exportState
        assertThat(state).isInstanceOf(ReportsExportState.Succeeded::class.java)
        assertThat((state as ReportsExportState.Succeeded).artifactPath).endsWith(".csv")
        assertThat(csvSink.writtenContent).contains("Kopi Susu")
        assertThat(csvSink.writtenContent).contains("INV-1")
    }

    @Test
    fun xlsxExportWritesBinaryArtifact() = runTest(mainDispatcherRule.testDispatcher) {
        val binarySink = RecordingBinarySink()
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction(id = 1L))),
            transactionRepository = FakeTransactionRepository(mapOf(1L to listOf(sampleTransactionItem()))),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct())),
            binarySink = binarySink,
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.exportReport(ReportExportFormat.XLSX)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = presenter.uiState.value.exportState
        assertThat(state).isInstanceOf(ReportsExportState.Succeeded::class.java)
        assertThat((state as ReportsExportState.Succeeded).artifactPath).endsWith(".xlsx")
        assertThat(binarySink.writtenBytes).isNotNull()
        assertThat(binarySink.writtenBytes!!.size).isGreaterThan(0)
    }

    @Test
    fun rewardedAdFailureDoesNotBlockExport() = runTest(mainDispatcherRule.testDispatcher) {
        val adController = RecordingAdController(AppResult.Failure(AppError.Network("ads unavailable")))
        val csvSink = RecordingCsvSink()
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction(id = 1L))),
            transactionRepository = FakeTransactionRepository(mapOf(1L to listOf(sampleTransactionItem()))),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct())),
            csvSink = csvSink,
            adController = adController,
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.exportReport(ReportExportFormat.CSV)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(adController.callCount).isEqualTo(1)
        assertThat(presenter.uiState.value.exportState).isInstanceOf(ReportsExportState.Succeeded::class.java)
        assertThat(csvSink.writtenContent).isNotNull()
    }

    @Test
    fun rewardedAdExceptionDoesNotBlockExport() = runTest(mainDispatcherRule.testDispatcher) {
        val adController = RecordingAdController(shouldThrow = true)
        val csvSink = RecordingCsvSink()
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction(id = 1L))),
            transactionRepository = FakeTransactionRepository(mapOf(1L to listOf(sampleTransactionItem()))),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct())),
            csvSink = csvSink,
            adController = adController,
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.exportReport(ReportExportFormat.CSV)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(adController.callCount).isEqualTo(1)
        assertThat(presenter.uiState.value.exportState).isInstanceOf(ReportsExportState.Succeeded::class.java)
    }

    @Test
    fun exportRequestsTheExcelExportPlacement() = runTest(mainDispatcherRule.testDispatcher) {
        val adController = RecordingAdController()
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction(id = 1L))),
            transactionRepository = FakeTransactionRepository(mapOf(1L to listOf(sampleTransactionItem()))),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct())),
            adController = adController,
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.exportReport(ReportExportFormat.CSV)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(adController.requestedPlacement).isEqualTo(AdPlacement.EXCEL_EXPORT)
    }

    @Test
    fun exportFailureIsReportedAsStorageError() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(failRead = true),
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.exportReport(ReportExportFormat.CSV)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = presenter.uiState.value.exportState
        assertThat(state).isInstanceOf(ReportsExportState.Failed::class.java)
        assertThat((state as ReportsExportState.Failed).error).isInstanceOf(AppError.Storage::class.java)
    }

    private fun createPresenter(
        reportRepository: ReportRepository,
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        productRepository: FakeProductRepository = FakeProductRepository(),
        csvSink: RecordingCsvSink = RecordingCsvSink(),
        binarySink: RecordingBinarySink = RecordingBinarySink(),
        adController: RecordingAdController = RecordingAdController(),
        scope: CoroutineScope,
    ): ReportsPresenter = ReportsPresenter(
        reportRepository = reportRepository,
        transactionRepository = transactionRepository,
        productRepository = productRepository,
        csvExporter = SalesCsvExporter(reportRepository, csvSink, utc),
        xlsxExporter = XlsxReportExporter(binarySink, utc),
        adController = adController,
        exportDirectory = exportDirectory,
        coroutineScope = scope,
        clock = clock,
        timeZone = utc,
    )
}
