package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction

enum class ReportsRangePreset(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("7 days"),
    LAST_30_DAYS("30 days"),
    THIS_MONTH("This month"),
    THIS_YEAR("This year"),
    ALL_TIME("All time"),
    CUSTOM("Custom"),
}

sealed interface ReportsDrillDown {
    val transactions: List<Transaction>

    data class Summary(override val transactions: List<Transaction>) : ReportsDrillDown

    data class Day(
        val epochDay: Long,
        override val transactions: List<Transaction>,
    ) : ReportsDrillDown
}

sealed interface ReportsExportState {
    data object Idle : ReportsExportState

    data object Running : ReportsExportState

    data class Succeeded(val artifactPath: String, val byteCount: Long) : ReportsExportState

    data class Failed(val error: AppError) : ReportsExportState
}

data class ReportsUiState(
    val range: ReportRange,
    val preset: ReportsRangePreset,
    val summary: SalesSummary? = null,
    val dailySales: List<DailySales> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val drillDown: ReportsDrillDown? = null,
    val pendingReturnTransactionId: Long? = null,
    val exportState: ReportsExportState = ReportsExportState.Idle,
)
