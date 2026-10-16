package id.umkm.pos.pro.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.feature.reports.ReportRangeFactory
import id.umkm.pos.pro.ui.components.ChartPoint
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.components.ChartBuckets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DashboardRangePreset(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari Terakhir"),
    LAST_30_DAYS("30 Hari Terakhir"),
    THIS_MONTH("Bulan Ini"),
}

data class BestSeller(
    val productId: Long,
    val name: String,
    val category: String?,
    val quantitySold: Double,
    val salesValue: Long,
)

data class PaymentShare(
    val method: PaymentMethod,
    val label: String,
    val amount: Long,
    val transactionCount: Int,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val storeName: String = "",
    val cashierName: String? = null,
    val preset: DashboardRangePreset = DashboardRangePreset.LAST_7_DAYS,
    val range: ReportRange? = null,
    val totalSales: Long = 0L,
    val transactionCount: Int = 0,
    val itemsSold: Double = 0.0,
    val averageTransaction: Long = 0L,
    val netProfit: Long = 0L,
    val dailyPoints: List<ChartPoint> = emptyList(),
    val bestSellers: List<BestSeller> = emptyList(),
    val paymentShares: List<PaymentShare> = emptyList(),
    val comparisonPercent: Int? = null,
    val hasAnySales: Boolean = false,
)

class DashboardViewModel(
    private val reportRepository: ReportRepository,
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository,
    clock: Clock,
) : ViewModel() {

    private val rangeFactory = ReportRangeFactory(clock)
    private val state = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = state.asStateFlow()

    init {
        load(DashboardRangePreset.LAST_7_DAYS)
    }

    fun selectPreset(preset: DashboardRangePreset) {
        state.update { it.copy(preset = preset) }
        load(preset)
    }

    fun refresh() {
        load(state.value.preset)
    }

    private fun load(preset: DashboardRangePreset) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val range = rangeFor(preset)
                val settings = settingsRepository.getSettings()
                val summary = reportRepository.salesSummary(range)
                val daily = reportRepository.salesByDay(range)
                val transactions = reportRepository.transactionsInRange(range)
                val previousRange = previousRangeOf(range)
                val previousSummary = reportRepository.salesSummary(previousRange)

                val itemsSold = transactions.sumOf { transaction ->
                    transactionRepository.getItems(transaction.id).sumOf { item -> item.qty }
                }
                val salesValueByProduct = LinkedHashMap<Long, Long>()
                val quantityByProduct = LinkedHashMap<Long, Double>()
                transactions.forEach { transaction ->
                    transactionRepository.getItems(transaction.id).forEach { item ->
                        quantityByProduct[item.productId] =
                            (quantityByProduct[item.productId] ?: 0.0) + item.qty
                        salesValueByProduct[item.productId] =
                            (salesValueByProduct[item.productId] ?: 0L) + item.subtotal
                    }
                }
                val bestSellers = quantityByProduct.entries
                    .sortedByDescending { it.value }
                    .take(BEST_SELLER_LIMIT)
                    .mapNotNull { entry ->
                        productRepository.getProductById(entry.key)?.let { product ->
                            BestSeller(
                                productId = product.id,
                                name = product.name,
                                category = product.category,
                                quantitySold = entry.value,
                                salesValue = salesValueByProduct[entry.key] ?: 0L,
                            )
                        }
                    }

                DashboardUiState(
                    isLoading = false,
                    storeName = settings.storeName,
                    cashierName = settings.cashierName,
                    preset = preset,
                    range = range,
                    totalSales = summary.totalSales,
                    transactionCount = summary.transactionCount,
                    itemsSold = itemsSold,
                    averageTransaction = if (summary.transactionCount > 0) {
                        summary.totalSales / summary.transactionCount
                    } else {
                        0L
                    },
                    netProfit = summary.netProfit,
                    dailyPoints = ChartBuckets.dailyZeroFilled(range, daily).map { day ->
                        ChartPoint(
                            label = if (daily.size <= 7) {
                                ChartBuckets.weekdayLabel(day.epochDay)
                            } else {
                                ChartBuckets.dayOfMonthLabel(day.epochDay)
                            },
                            value = day.totalSales.toDouble(),
                        )
                    },
                    bestSellers = bestSellers,
                    paymentShares = paymentShares(transactions),
                    comparisonPercent = comparisonPercent(summary.totalSales, previousSummary.totalSales),
                    hasAnySales = summary.transactionCount > 0,
                )
            }.fold(
                onSuccess = { loaded -> state.value = loaded },
                onFailure = { throwable ->
                    state.update { it.copy(isLoading = false, error = AppError.Unexpected(throwable.message ?: "Gagal memuat data")) }
                },
            )
        }
    }

    private fun rangeFor(preset: DashboardRangePreset): ReportRange = when (preset) {
        DashboardRangePreset.TODAY -> rangeFactory.today()
        DashboardRangePreset.LAST_7_DAYS -> rangeFactory.lastDays(7)
        DashboardRangePreset.LAST_30_DAYS -> rangeFactory.lastDays(30)
        DashboardRangePreset.THIS_MONTH -> rangeFactory.thisMonth()
    }

    private fun previousRangeOf(range: ReportRange): ReportRange {
        val length = (range.endMillis - range.startMillis).coerceAtLeast(1L)
        return ReportRange(
            startMillis = range.startMillis - length - 1,
            endMillis = range.startMillis - 1,
        )
    }

    private fun comparisonPercent(current: Long, previous: Long): Int? {
        if (previous <= 0L) return null
        return (((current - previous) * 100) / previous).toInt()
    }

    private fun paymentShares(transactions: List<id.umkm.pos.pro.core.model.Transaction>): List<PaymentShare> =
        transactions.groupBy { it.paymentMethod }
            .map { (method, group) ->
                PaymentShare(
                    method = method,
                    label = paymentMethodLabel(method),
                    amount = group.sumOf { it.grandTotal },
                    transactionCount = group.size,
                )
            }
            .sortedByDescending { it.amount }

    private companion object {
        const val BEST_SELLER_LIMIT = 5
    }
}
