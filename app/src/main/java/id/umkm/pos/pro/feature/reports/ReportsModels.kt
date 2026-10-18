package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.ReceiptPrintRequest
import id.umkm.pos.pro.ui.common.UiText

enum class ReportsRangePreset {
    TODAY,
    LAST_7_DAYS,
    LAST_28_DAYS,
    CUSTOM,
}

fun ReportsRangePreset.label(strings: PosStrings): String = when (this) {
    ReportsRangePreset.TODAY -> strings.reportsRangeTodayLower
    ReportsRangePreset.LAST_7_DAYS -> strings.reportsPresetLast7Days
    ReportsRangePreset.LAST_28_DAYS -> strings.reportsPresetLast30Days
    ReportsRangePreset.CUSTOM -> strings.reportsRangeCustomHint
}

fun ReportsRangePreset.pickerLabel(strings: PosStrings): String = when (this) {
    ReportsRangePreset.TODAY -> strings.reportsPresetToday
    ReportsRangePreset.LAST_7_DAYS -> strings.reportsPresetLast7Days
    ReportsRangePreset.LAST_28_DAYS -> strings.reportsPresetLast30Days
    ReportsRangePreset.CUSTOM -> strings.reportsRangeCustomHint
}

enum class TransactionStatusFilter(
    val statuses: Set<TransactionStatus>,
) {
    COMPLETED(setOf(TransactionStatus.COMPLETED, TransactionStatus.PARTIAL_DEBT)),
    KASBON(setOf(TransactionStatus.PARTIAL_DEBT)),
    REFUND(setOf(TransactionStatus.RETURNED)),
}

fun TransactionStatusFilter.label(strings: PosStrings): String = when (this) {
    TransactionStatusFilter.COMPLETED -> strings.reportsStatusFilterCompleted
    TransactionStatusFilter.KASBON -> strings.reportsStatusFilterKasbon
    TransactionStatusFilter.REFUND -> strings.reportsStatusFilterRefund
}

internal fun List<Transaction>.matchingStatusFilter(
    statusFilter: TransactionStatusFilter?,
    searchQuery: String = "",
): List<Transaction> {
    val trimmedQuery = searchQuery.trim()
    return filter { transaction ->
        val matchesStatus = statusFilter == null || transaction.status in statusFilter.statuses
        val matchesSearch = trimmedQuery.isEmpty() ||
            transaction.invoiceNo.contains(trimmedQuery, ignoreCase = true) ||
            transaction.notes?.contains(trimmedQuery, ignoreCase = true) == true ||
            transaction.grandTotal.toString().contains(trimmedQuery)
        matchesStatus && matchesSearch
    }
}

data class TransactionDetailLine(
    val name: String,
    val quantityLabel: String,
    val unitPrice: Long,
    val itemDiscount: Long,
    val lineTotal: Long,
)

data class TransactionDetail(
    val transaction: Transaction,
    val customerName: String?,
    val cashierName: String?,
    val lines: List<TransactionDetailLine>,
)

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
    val topProducts: List<ReportProductSummary> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val visibleTransactions: List<Transaction> = emptyList(),
    val statusFilter: TransactionStatusFilter? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val detail: TransactionDetail? = null,
    val isDetailLoading: Boolean = false,
    val pendingReturnTransactionId: Long? = null,
    val pendingReceiptShare: String? = null,
    val pendingReceiptPrint: ReceiptPrintRequest? = null,
    val message: UiText? = null,
    val exportState: ReportsExportState = ReportsExportState.Idle,
)
