package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.TimeZone

class ReportsPresenter(
    private val reportRepository: ReportRepository,
    private val csvExporter: SalesCsvExporter,
    private val exportDirectory: File,
    private val coroutineScope: CoroutineScope,
    clock: Clock,
    timeZone: TimeZone = TimeZone.getDefault(),
) {

    private val rangeFactory = ReportRangeFactory(clock, timeZone)

    private val mutableState = MutableStateFlow(
        ReportsUiState(
            range = rangeFactory.today(),
            preset = ReportsRangePreset.TODAY,
        ),
    )

    val uiState: StateFlow<ReportsUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var exportJob: Job? = null

    fun start() {
        refresh()
    }

    fun refresh() {
        loadReports(mutableState.value.range, mutableState.value.preset)
    }

    fun selectPreset(preset: ReportsRangePreset) {
        val range = when (preset) {
            ReportsRangePreset.TODAY -> rangeFactory.today()
            ReportsRangePreset.LAST_7_DAYS -> rangeFactory.lastDays(7)
            ReportsRangePreset.LAST_30_DAYS -> rangeFactory.lastDays(30)
            ReportsRangePreset.THIS_MONTH -> rangeFactory.thisMonth()
            ReportsRangePreset.THIS_YEAR -> rangeFactory.thisYear()
            ReportsRangePreset.ALL_TIME -> rangeFactory.allTime()
            ReportsRangePreset.CUSTOM -> return
        }
        loadReports(range, preset)
    }

    fun selectCustomDays(startEpochDay: Long, endEpochDay: Long) {
        loadReports(rangeFactory.forEpochDayRange(startEpochDay, endEpochDay), ReportsRangePreset.CUSTOM)
    }

    fun openSummaryDrillDown() {
        val currentRange = mutableState.value.range
        loadDrillDown(day = null, range = currentRange)
    }

    fun openDayDrillDown(epochDay: Long) {
        loadDrillDown(day = epochDay, range = rangeFactory.forEpochDay(epochDay))
    }

    fun closeDrillDown() {
        mutableState.update { current -> current.copy(drillDown = null) }
    }

    fun requestReturn(transactionId: Long) {
        mutableState.update { current -> current.copy(pendingReturnTransactionId = transactionId) }
    }

    fun dismissReturn() {
        mutableState.update { current -> current.copy(pendingReturnTransactionId = null) }
    }

    fun onReturnProcessed() {
        mutableState.update { current -> current.copy(pendingReturnTransactionId = null) }
        refresh()
    }

    fun exportCsv() {
        exportJob?.cancel()
        exportJob = coroutineScope.launch {
            val currentRange = mutableState.value.range
            mutableState.update { current -> current.copy(exportState = ReportsExportState.Running) }
            val targetPath = File(
                exportDirectory,
                "transactions-${currentRange.startMillis}-${currentRange.endMillis}.csv",
            ).absolutePath
            when (val result = csvExporter.exportTransactions(currentRange, targetPath)) {
                is AppResult.Success -> mutableState.update { current ->
                    current.copy(
                        exportState = ReportsExportState.Succeeded(
                            artifactPath = result.value.path,
                            byteCount = result.value.byteCount,
                        ),
                    )
                }

                is AppResult.Failure -> mutableState.update { current ->
                    current.copy(exportState = ReportsExportState.Failed(result.error))
                }
            }
        }
    }

    fun dismissExportState() {
        mutableState.update { current -> current.copy(exportState = ReportsExportState.Idle) }
    }

    private fun loadReports(range: ReportRange, preset: ReportsRangePreset) {
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            mutableState.update { current ->
                current.copy(
                    range = range,
                    preset = preset,
                    isLoading = true,
                    error = null,
                    drillDown = null,
                )
            }
            try {
                val summary = reportRepository.salesSummary(range)
                val dailySales = reportRepository.salesByDay(range)
                val transactions = reportRepository.transactionsInRange(range)
                mutableState.update { current ->
                    current.copy(
                        summary = summary,
                        averageTransaction = averageTransactionOf(summary),
                        dailySales = dailySales,
                        paymentShares = paymentShares(transactions),
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutableState.update { current ->
                    current.copy(
                        isLoading = false,
                        error = AppError.Unexpected(error.message ?: "failed to load report"),
                    )
                }
            }
        }
    }

    private fun averageTransactionOf(summary: SalesSummary): Long =
        if (summary.transactionCount > 0) summary.totalSales / summary.transactionCount else 0L

    private fun paymentShares(transactions: List<Transaction>): List<ReportsPaymentShare> =
        transactions.groupBy { transaction -> transaction.paymentMethod }
            .map { (method, group) ->
                ReportsPaymentShare(
                    paymentMethod = method,
                    amount = group.sumOf { transaction -> transaction.grandTotal },
                    transactionCount = group.size,
                )
            }
            .sortedByDescending { share -> share.amount }

    private fun loadDrillDown(day: Long?, range: ReportRange) {
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            mutableState.update { current -> current.copy(isLoading = true, drillDown = null) }
            try {
                val transactions = reportRepository.transactionsInRange(range)
                val drillDown = if (day == null) {
                    ReportsDrillDown.Summary(transactions)
                } else {
                    ReportsDrillDown.Day(epochDay = day, transactions = transactions)
                }
                mutableState.update { current ->
                    current.copy(isLoading = false, drillDown = drillDown)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutableState.update { current ->
                    current.copy(
                        isLoading = false,
                        error = AppError.Unexpected(error.message ?: "failed to load transactions"),
                    )
                }
            }
        }
    }
}
