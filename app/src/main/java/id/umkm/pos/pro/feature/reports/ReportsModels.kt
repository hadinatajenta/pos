package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.ui.components.ChartPoint

enum class ReportsRangePreset(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari Terakhir"),
    LAST_30_DAYS("30 Hari Terakhir"),
    THIS_MONTH("Bulan Ini"),
    THIS_YEAR("Tahun Ini"),
    ALL_TIME("Semua"),
    CUSTOM("Kustom"),
}

data class ReportsPaymentShare(
    val paymentMethod: PaymentMethod,
    val amount: Long,
    val transactionCount: Int,
)

data class ReportProductSummary(
    val productId: Long,
    val name: String,
    val quantitySold: Double,
    val salesValue: Long,
)

data class ReportContent(
    val transactions: List<Transaction>,
    val productSummaries: List<ReportProductSummary>,
)

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

    data object PermissionDenied : ReportsExportState
}

data class ReportsUiState(
    val range: ReportRange,
    val preset: ReportsRangePreset,
    val summary: SalesSummary? = null,
    val averageTransaction: Long = 0L,
    val dailySales: List<DailySales> = emptyList(),
    val salesChartPoints: List<ChartPoint> = emptyList(),
    val paymentShares: List<ReportsPaymentShare> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val drillDown: ReportsDrillDown? = null,
    val pendingReturnTransactionId: Long? = null,
    val exportState: ReportsExportState = ReportsExportState.Idle,
)
