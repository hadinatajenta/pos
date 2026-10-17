package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.ui.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.TimeZone

class ReportsPresenterTransactionsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val utc = TimeZone.getTimeZone("UTC")
    private val clock = FixedClock(1_705_320_000_000L)

    @Test
    fun statusFilterTogglesBetweenCompletedRefundAndAll() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(
                transactions = listOf(
                    sampleTransaction(id = 1L, status = TransactionStatus.COMPLETED),
                    sampleTransaction(id = 2L, status = TransactionStatus.PARTIAL_DEBT),
                    sampleTransaction(id = 3L, status = TransactionStatus.RETURNED),
                ),
            ),
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(presenter.uiState.value.visibleTransactions.map { transaction -> transaction.id })
            .containsExactly(1L, 2L, 3L)
            .inOrder()

        presenter.toggleStatusFilter(TransactionStatusFilter.COMPLETED)

        assertThat(presenter.uiState.value.statusFilter).isEqualTo(TransactionStatusFilter.COMPLETED)
        assertThat(presenter.uiState.value.visibleTransactions.map { transaction -> transaction.id })
            .containsExactly(1L, 2L)
            .inOrder()

        presenter.toggleStatusFilter(TransactionStatusFilter.REFUND)

        assertThat(presenter.uiState.value.visibleTransactions.map { transaction -> transaction.id })
            .containsExactly(3L)

        presenter.toggleStatusFilter(TransactionStatusFilter.REFUND)

        assertThat(presenter.uiState.value.statusFilter).isNull()
        assertThat(presenter.uiState.value.visibleTransactions.map { transaction -> transaction.id })
            .containsExactly(1L, 2L, 3L)
            .inOrder()
    }

    @Test
    fun selectedStatusFilterSurvivesPeriodReload() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            reportRepository = FakeReportRepository(
                transactions = listOf(sampleTransaction(id = 3L, status = TransactionStatus.RETURNED)),
            ),
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.toggleStatusFilter(TransactionStatusFilter.REFUND)
        presenter.selectPreset(ReportsRangePreset.LAST_7_DAYS)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = presenter.uiState.value
        assertThat(state.statusFilter).isEqualTo(TransactionStatusFilter.REFUND)
        assertThat(state.visibleTransactions.map { transaction -> transaction.id }).containsExactly(3L)
    }

    @Test
    fun openDetailLoadsItemLinesAndCounterparties() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            transactionRepository = FakeTransactionRepository(
                itemsByTransaction = mapOf(
                    5L to listOf(
                        sampleTransactionItem(
                            id = 1L,
                            transactionId = 5L,
                            productId = 10L,
                            qty = 3.0,
                            subtotal = 45_000L,
                        ),
                    ),
                ),
                transactionsById = mapOf(5L to sampleTransaction(id = 5L, invoiceNo = "INV-5", customerId = 7L)),
            ),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct(name = "Kopi Susu"))),
            customerRepository = FakeCustomerRepository(
                mapOf(
                    7L to Customer(
                        id = 7L,
                        name = "Bu Rina",
                        whatsappNumber = null,
                        totalDebt = 0L,
                        notes = null,
                    ),
                ),
            ),
            settingsRepository = FakeSettingsRepository(sampleSettings(storeName = "Toko Uji", cashierName = "Sari")),
            scope = this,
        )

        presenter.start()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        presenter.openDetail(5L)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val detail = presenter.uiState.value.detail
        assertThat(detail).isNotNull()
        val loadedDetail = requireNotNull(detail)
        assertThat(loadedDetail.transaction.invoiceNo).isEqualTo("INV-5")
        assertThat(loadedDetail.cashierName).isEqualTo("Sari")
        assertThat(loadedDetail.customerName).isEqualTo("Bu Rina")
        assertThat(loadedDetail.lines).hasSize(1)
        assertThat(loadedDetail.lines.single().name).isEqualTo("Kopi Susu")
        assertThat(loadedDetail.lines.single().quantityLabel).isEqualTo("3 pcs")
        assertThat(loadedDetail.lines.single().lineTotal).isEqualTo(45_000L)

        presenter.closeDetail()

        assertThat(presenter.uiState.value.detail).isNull()
    }

    @Test
    fun openDetailReportsMissingTransaction() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(scope = this)

        presenter.openDetail(9L)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(presenter.uiState.value.detail).isNull()
        assertThat(presenter.uiState.value.message).isEqualTo("Transaksi tidak ditemukan.")

        presenter.onMessageConsumed()

        assertThat(presenter.uiState.value.message).isNull()
    }

    @Test
    fun shareReceiptBuildsShareTextFromStoredTransaction() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createStoredTransactionPresenter(this)

        presenter.shareReceipt(1L)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val shareText = presenter.uiState.value.pendingReceiptShare
        assertThat(shareText).isNotNull()
        assertThat(requireNotNull(shareText)).contains("INV-1")
        assertThat(requireNotNull(shareText)).contains("Kopi Susu")
        assertThat(requireNotNull(shareText)).contains("Toko Uji")
        assertThat(requireNotNull(shareText)).contains("TOTAL : Rp 20.000")

        presenter.onReceiptShared()

        assertThat(presenter.uiState.value.pendingReceiptShare).isNull()
    }

    @Test
    fun printReceiptBuildsStrukPrintRequest() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createStoredTransactionPresenter(this)

        presenter.printReceipt(1L)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val printRequest = presenter.uiState.value.pendingReceiptPrint
        assertThat(printRequest).isNotNull()
        assertThat(requireNotNull(printRequest).documentName).isEqualTo("Struk INV-1")
        assertThat(requireNotNull(printRequest).content).contains("Invoice INV-1")
        assertThat(requireNotNull(printRequest).content).contains("TOTAL")

        presenter.onReceiptPrinted()

        assertThat(presenter.uiState.value.pendingReceiptPrint).isNull()
    }

    private fun createStoredTransactionPresenter(scope: CoroutineScope): ReportsPresenter = createPresenter(
        transactionRepository = FakeTransactionRepository(
            itemsByTransaction = mapOf(
                1L to listOf(
                    sampleTransactionItem(
                        id = 1L,
                        transactionId = 1L,
                        productId = 10L,
                        qty = 2.0,
                        subtotal = 30_000L,
                    ),
                ),
            ),
            transactionsById = mapOf(1L to sampleTransaction(id = 1L, invoiceNo = "INV-1")),
        ),
        productRepository = FakeProductRepository(mapOf(10L to sampleProduct(name = "Kopi Susu"))),
        settingsRepository = FakeSettingsRepository(sampleSettings(storeName = "Toko Uji", cashierName = "Sari")),
        scope = scope,
    )

    private fun createPresenter(
        reportRepository: ReportRepository = FakeReportRepository(),
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        productRepository: FakeProductRepository = FakeProductRepository(),
        customerRepository: FakeCustomerRepository = FakeCustomerRepository(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
        scope: CoroutineScope,
    ): ReportsPresenter = ReportsPresenter(
        reportRepository = reportRepository,
        transactionRepository = transactionRepository,
        productRepository = productRepository,
        customerRepository = customerRepository,
        settingsRepository = settingsRepository,
        csvExporter = SalesCsvExporter(reportRepository, RecordingCsvSink(), utc),
        xlsxExporter = XlsxReportExporter(RecordingBinarySink(), utc),
        adController = RecordingAdController(),
        coroutineScope = scope,
        clock = clock,
        timeZone = utc,
    )
}
