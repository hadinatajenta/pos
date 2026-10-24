package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.RewardedAdOutcome
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.feature.receipt.ReceiptShareText
import id.umkm.pos.pro.feature.receipt.StoredReceipt
import id.umkm.pos.pro.feature.receipt.StoredReceiptBuilder
import id.umkm.pos.pro.feature.receipt.StoredReceiptLoader
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.ReceiptPrintRequest
import id.umkm.pos.pro.ui.common.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.TimeZone

class ReportsPresenter(
    private val reportRepository: ReportRepository,
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository,
    private val csvExporter: SalesCsvExporter,
    private val xlsxExporter: XlsxReportExporter,
    private val adController: AdController,
    private val coroutineScope: CoroutineScope,
    clock: Clock,
    private val stringsFlow: StateFlow<PosStrings>,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {

    private val rangeFactory = ReportRangeFactory(clock, timeZone)
    private val zoneId: ZoneId = timeZone.toZoneId()

    private val mutableState = MutableStateFlow(
        ReportsUiState(
            range = rangeFactory.today(),
            preset = ReportsRangePreset.TODAY,
        ),
    )

    val uiState: StateFlow<ReportsUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var exportJob: Job? = null
    private var detailJob: Job? = null
    private var receiptJob: Job? = null

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
            ReportsRangePreset.LAST_28_DAYS -> rangeFactory.lastDays(28)
            ReportsRangePreset.CUSTOM -> return
        }
        loadReports(range, preset)
    }

    fun selectCustomDays(startEpochDay: Long, endEpochDay: Long) {
        loadReports(rangeFactory.forEpochDayRange(startEpochDay, endEpochDay), ReportsRangePreset.CUSTOM)
    }

    fun toggleStatusFilter(statusFilter: TransactionStatusFilter) {
        mutableState.update { current ->
            val updatedFilter = if (current.statusFilter == statusFilter) null else statusFilter
            current.copy(
                statusFilter = updatedFilter,
                visibleTransactions = current.transactions.matchingStatusFilter(
                    statusFilter = updatedFilter,
                    searchQuery = current.searchQuery,
                ),
            )
        }
    }

    fun selectStatusFilter(statusFilter: TransactionStatusFilter?) {
        mutableState.update { current ->
            current.copy(
                statusFilter = statusFilter,
                visibleTransactions = current.transactions.matchingStatusFilter(
                    statusFilter = statusFilter,
                    searchQuery = current.searchQuery,
                ),
            )
        }
    }

    fun updateSearchQuery(query: String) {
        mutableState.update { current ->
            current.copy(
                searchQuery = query,
                visibleTransactions = current.transactions.matchingStatusFilter(
                    statusFilter = current.statusFilter,
                    searchQuery = query,
                ),
            )
        }
    }

    fun openDetail(transactionId: Long) {
        detailJob?.cancel()
        detailJob = coroutineScope.launch {
            mutableState.update { current -> current.copy(isDetailLoading = true, detail = null, message = null) }
            try {
                val detail = TransactionDetailLoader.load(
                    transactionId = transactionId,
                    transactionRepository = transactionRepository,
                    productRepository = productRepository,
                    settingsRepository = settingsRepository,
                )
                mutableState.update { current ->
                    if (detail == null) {
                        current.copy(
                            isDetailLoading = false,
                            message = UiText.from { strings -> strings.transactionsNotFound },
                        )
                    } else {
                        current.copy(isDetailLoading = false, detail = detail)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutableState.update { current ->
                    current.copy(
                        isDetailLoading = false,
                        message = UiText.from { strings -> strings.transactionsDetailLoadFailed },
                    )
                }
            }
        }
    }

    fun closeDetail() {
        mutableState.update { current -> current.copy(detail = null, isDetailLoading = false) }
    }

    fun shareReceipt(transactionId: Long) {
        prepareStoredReceipt(transactionId) { current, receipt ->
            current.copy(pendingReceiptShare = ReceiptShareText.build(receipt.shareData, stringsFlow.value))
        }
    }

    fun printReceipt(transactionId: Long) {
        prepareStoredReceipt(transactionId) { current, receipt ->
            current.copy(
                pendingReceiptPrint = ReceiptPrintRequest(
                    documentName = stringsFlow.value.receiptPrintJobName(receipt.shareData.invoiceNo),
                    content = StoredReceiptBuilder.toPrintText(receipt),
                ),
            )
        }
    }

    fun onReceiptShared() {
        mutableState.update { current -> current.copy(pendingReceiptShare = null) }
    }

    fun onReceiptPrinted() {
        mutableState.update { current -> current.copy(pendingReceiptPrint = null) }
    }

    fun onMessageConsumed() {
        mutableState.update { current -> current.copy(message = null) }
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

    fun exportReport(format: ReportExportFormat) {
        exportJob?.cancel()
        exportJob = coroutineScope.launch {
            val currentRange = mutableState.value.range
            mutableState.update { current -> current.copy(exportState = ReportsExportState.Running) }
            try {
                val adOutcome = requestExportRewardedAd(adController)
                if (adOutcome == RewardedAdOutcome.DISMISSED) {
                    mutableState.update { current -> current.copy(exportState = ReportsExportState.AdRequired) }
                    return@launch
                }
                val content = ReportContentLoader.load(
                    range = currentRange,
                    reportRepository = reportRepository,
                    transactionRepository = transactionRepository,
                    productRepository = productRepository,
                )
                val fileName = ReportExportFileName.forRange(currentRange, format, timeZone, stringsFlow.value)
                val result = when (format) {
                    ReportExportFormat.CSV -> csvExporter.exportReport(content, fileName, stringsFlow.value)
                    ReportExportFormat.XLSX -> xlsxExporter.exportReport(content, fileName, stringsFlow.value)
                }
                mutableState.update { current -> current.withExportResult(result) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutableState.update { current ->
                    current.copy(
                        exportState = ReportsExportState.Failed(
                            AppError.Storage(error.message ?: "failed to export report"),
                        ),
                    )
                }
            }
        }
    }

    fun dismissExportState() {
        mutableState.update { current -> current.copy(exportState = ReportsExportState.Idle) }
    }

    fun onExportPermissionDenied() {
        mutableState.update { current -> current.copy(exportState = ReportsExportState.PermissionDenied) }
    }

    private fun prepareStoredReceipt(
        transactionId: Long,
        onReady: (ReportsUiState, StoredReceipt) -> ReportsUiState,
    ) {
        receiptJob?.cancel()
        receiptJob = coroutineScope.launch {
            try {
                val receipt = StoredReceiptLoader.load(
                    transactionId = transactionId,
                    transactionRepository = transactionRepository,
                    productRepository = productRepository,
                    settingsRepository = settingsRepository,
                    strings = stringsFlow.value,
                )
                mutableState.update { current ->
                    if (receipt == null) {
                        current.copy(
                            message = UiText.from { strings -> strings.transactionsNotFound },
                        )
                    } else {
                        onReady(current, receipt)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                mutableState.update { current ->
                    current.copy(
                        message = UiText.from { strings -> strings.transactionsReceiptPrepareFailed },
                    )
                }
            }
        }
    }

    private fun ReportsUiState.withExportResult(result: AppResult<BackupArtifact>): ReportsUiState =
        when (result) {
            is AppResult.Success -> copy(
                exportState = ReportsExportState.Succeeded(
                    artifactPath = result.value.path,
                    byteCount = result.value.byteCount,
                ),
            )

            is AppResult.Failure -> copy(exportState = ReportsExportState.Failed(result.error))
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
                )
            }
            try {
                val summary = reportRepository.salesSummary(range)
                val dailySales = reportRepository.salesByDay(range)
                val content = ReportContentLoader.load(
                    range = range,
                    reportRepository = reportRepository,
                    transactionRepository = transactionRepository,
                    productRepository = productRepository,
                )
                val transactions = content.transactions
                    .sortedByDescending { transaction -> transaction.createdAt }
                mutableState.update { current ->
                    current.copy(
                        summary = summary,
                        averageTransaction = averageTransactionOf(summary),
                        dailySales = dailySales,
                        salesChartPoints = chartPointsFor(range, dailySales, zoneId, stringsFlow.value),
                        paymentShares = paymentShares(transactions),
                        topProducts = content.productSummaries,
                        transactions = transactions,
                        visibleTransactions = transactions.matchingStatusFilter(
                            statusFilter = current.statusFilter,
                            searchQuery = current.searchQuery,
                        ),
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
}
