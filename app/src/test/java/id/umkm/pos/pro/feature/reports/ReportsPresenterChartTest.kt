package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.PosStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.TimeZone

class ReportsPresenterChartTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val utc = TimeZone.getTimeZone("UTC")
    private val clock = FixedClock(1_705_320_000_000L)

    @Test
    fun weeklyRangeZeroFillsEveryExpectedDay() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            FakeReportRepository(
                dailySales = listOf(
                    DailySales(epochDay = 19_731L, totalSales = 512_000L, netProfit = 180_000L, transactionCount = 3),
                    DailySales(epochDay = 19_737L, totalSales = 1_180_000L, netProfit = 410_000L, transactionCount = 6),
                ),
            ),
            this,
        )

        presenter.selectPreset(ReportsRangePreset.LAST_7_DAYS)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val points = presenter.uiState.value.salesChartPoints
        assertThat(points.map { point -> point.label })
            .containsExactly("Sel", "Rab", "Kam", "Jum", "Sab", "Min", "Sen")
            .inOrder()
        assertThat(points.map { point -> point.value })
            .containsExactly(512_000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1_180_000.0)
            .inOrder()
    }

    @Test
    fun longRangeLabelsEveryBucketByDayOfMonth() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            FakeReportRepository(
                dailySales = listOf(
                    DailySales(epochDay = 19_710L, totalSales = 240_000L, netProfit = 90_000L, transactionCount = 2),
                    DailySales(epochDay = 19_737L, totalSales = 1_180_000L, netProfit = 410_000L, transactionCount = 6),
                ),
            ),
            this,
        )

        presenter.selectPreset(ReportsRangePreset.LAST_28_DAYS)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val points = presenter.uiState.value.salesChartPoints
        assertThat(points).hasSize(28)
        assertThat(points.first().label).isEqualTo("19")
        assertThat(points.first().value).isEqualTo(240_000.0)
        assertThat(points.last().label).isEqualTo("15")
        assertThat(points.last().value).isEqualTo(1_180_000.0)
    }

    @Test
    fun singleDayRangeRendersOnePoint() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            FakeReportRepository(
                dailySales = listOf(
                    DailySales(epochDay = 19_737L, totalSales = 1_180_000L, netProfit = 410_000L, transactionCount = 6),
                ),
            ),
            this,
        )

        presenter.selectPreset(ReportsRangePreset.TODAY)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val points = presenter.uiState.value.salesChartPoints
        assertThat(points).hasSize(1)
        assertThat(points.single().label).isEqualTo("Sen")
        assertThat(points.single().value).isEqualTo(1_180_000.0)
    }

    @Test
    fun rangeWithoutSalesHasNoChartPoints() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            FakeReportRepository(
                dailySales = listOf(
                    DailySales(epochDay = 19_736L, totalSales = 0L, netProfit = 0L, transactionCount = 0),
                ),
            ),
            this,
        )

        presenter.selectPreset(ReportsRangePreset.LAST_7_DAYS)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = presenter.uiState.value
        assertThat(state.salesChartPoints).isEmpty()
        assertThat(state.summary).isEqualTo(SalesSummary(0L, 0L, 0L, 0))
    }

    @Test
    fun weeklyRangeLabelsFollowTheSelectedCatalogue() = runTest(mainDispatcherRule.testDispatcher) {
        val presenter = createPresenter(
            FakeReportRepository(
                dailySales = listOf(
                    DailySales(epochDay = 19_731L, totalSales = 512_000L, netProfit = 180_000L, transactionCount = 3),
                    DailySales(epochDay = 19_737L, totalSales = 1_180_000L, netProfit = 410_000L, transactionCount = 6),
                ),
            ),
            this,
            EnStrings,
        )

        presenter.selectPreset(ReportsRangePreset.LAST_7_DAYS)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val points = presenter.uiState.value.salesChartPoints
        assertThat(points.map { point -> point.label })
            .containsExactly("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon")
            .inOrder()
    }

    private fun createPresenter(
        repository: ReportRepository,
        coroutineScope: CoroutineScope,
        strings: PosStrings = IdStrings,
    ): ReportsPresenter = ReportsPresenter(
        reportRepository = repository,
        transactionRepository = FakeTransactionRepository(),
        productRepository = FakeProductRepository(),
        customerRepository = FakeCustomerRepository(),
        settingsRepository = FakeSettingsRepository(),
        csvExporter = SalesCsvExporter(repository, RecordingCsvSink(), utc),
        xlsxExporter = XlsxReportExporter(RecordingBinarySink(), utc),
        adController = RecordingAdController(),
        coroutineScope = coroutineScope,
        clock = clock,
        stringsFlow = stringsFlowFor(strings),
        timeZone = utc,
    )
}
