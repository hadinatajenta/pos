package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.data.file.BinarySink
import id.umkm.pos.pro.data.file.CsvSink
import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.RewardedAdOutcome
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.PosStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

internal fun stringsFlowFor(strings: PosStrings = IdStrings): StateFlow<PosStrings> =
    MutableStateFlow(strings)

internal fun sampleTransaction(
    id: Long = 1L,
    invoiceNo: String = "INV-1",
    createdAt: Long = 1_705_276_800_000L,
    paymentMethod: PaymentMethod = PaymentMethod.CASH,
    status: TransactionStatus = TransactionStatus.COMPLETED,
    subtotal: Long = 15_000L,
    grandTotal: Long = 20_000L,
    totalHpp: Long = 10_000L,
    netProfit: Long = 5_000L,
): Transaction = Transaction(
    id = id,
    invoiceNo = invoiceNo,
    createdAt = createdAt,
    cashierId = null,
    subtotal = subtotal,
    discountTotal = 0L,
    invoiceDiscount = 0L,
    taxTotal = 0L,
    rounding = 0L,
    grandTotal = grandTotal,
    totalHpp = totalHpp,
    netProfit = netProfit,
    paymentMethod = paymentMethod,
    paidAmount = grandTotal,
    changeAmount = 0L,
    status = status,
    notes = null,
)

internal fun sampleTransactionItem(
    id: Long = 1L,
    transactionId: Long = 1L,
    productId: Long = 10L,
    qty: Double = 3.0,
    subtotal: Long = 45_000L,
): TransactionItem = TransactionItem(
    id = id,
    transactionId = transactionId,
    productId = productId,
    variantName = null,
    qty = qty,
    unitHpp = 5_000L,
    appliedUnitPrice = 15_000L,
    isNegoPrice = false,
    isWholesaleApplied = false,
    itemDiscount = 0L,
    subtotal = subtotal,
)

internal fun sampleProduct(
    id: Long = 10L,
    name: String = "Kopi Susu",
): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = null,
    name = name,
    category = null,
    baseUnit = "pcs",
    baseHpp = 5_000L,
    basePrice = 15_000L,
    stockBaseQty = 100.0,
    minStockAlert = 5.0,
    isActive = true,
)

internal class FakeReportRepository(
    private val transactions: List<Transaction> = emptyList(),
    private val summary: SalesSummary = SalesSummary(0L, 0L, 0L, 0),
    private val dailySales: List<DailySales> = emptyList(),
    private val failRead: Boolean = false,
) : ReportRepository {

    override suspend fun salesSummary(range: ReportRange): SalesSummary = summary

    override suspend fun salesByDay(range: ReportRange): List<DailySales> = dailySales

    override suspend fun transactionsInRange(range: ReportRange): List<Transaction> {
        if (failRead) throw IllegalStateException("report store unavailable")
        return transactions
    }
}

internal class FakeTransactionRepository(
    private val itemsByTransaction: Map<Long, List<TransactionItem>> = emptyMap(),
    private val transactionsById: Map<Long, Transaction> = emptyMap(),
    private val failItemsRead: Boolean = false,
) : TransactionRepository {

    override suspend fun saveTransaction(transaction: Transaction, items: List<TransactionItem>): AppResult<Long> =
        error("saveTransaction is not used in report tests")

    override suspend fun nextInvoiceNumber(createdAtMillis: Long): String =
        error("nextInvoiceNumber is not used in report tests")

    override suspend fun getTransaction(transactionId: Long): Transaction? =
        transactionsById[transactionId]

    override fun observeTransactions(range: ReportRange): Flow<List<Transaction>> =
        error("observeTransactions is not used in report tests")

    override suspend fun getItems(transactionId: Long): List<TransactionItem> {
        if (failItemsRead) throw IllegalStateException("transaction items unavailable")
        return itemsByTransaction[transactionId].orEmpty()
    }

    override suspend fun markReturned(transactionId: Long, returnedItems: List<TransactionItem>): AppResult<Unit> =
        error("markReturned is not used in report tests")
}

internal class FakeSettingsRepository(
    private val settings: AppSettings = sampleSettings(),
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = flowOf(settings)

    override suspend fun getSettings(): AppSettings = settings

    override suspend fun updateSettings(settings: AppSettings) =
        error("updateSettings is not used in report tests")
}

internal fun sampleSettings(
    storeName: String = "Toko Uji",
    cashierName: String? = "Sari",
): AppSettings = AppSettings(
    storeName = storeName,
    cashierName = cashierName,
    taxRatePercent = 0,
    roundingIncrement = 0,
    isPinEnabled = false,
    printerAddress = null,
    qrisImagePath = null,
    qrisPayload = null,
)

internal class FakeProductRepository(
    private val products: Map<Long, Product> = emptyMap(),
) : ProductRepository {

    override fun observeProducts(query: String?): Flow<List<Product>> =
        error("observeProducts is not used in report tests")

    override suspend fun getProductById(productId: Long): Product? = products[productId]

    override suspend fun findProductByBarcode(barcode: String): Product? =
        error("findProductByBarcode is not used in report tests")

    override suspend fun upsertProduct(product: Product): Long =
        error("upsertProduct is not used in report tests")

    override suspend fun deleteProduct(productId: Long) =
        error("deleteProduct is not used in report tests")

    override suspend fun getUnits(productId: Long): List<ProductUnit> =
        error("getUnits is not used in report tests")

    override suspend fun findUnitByBarcode(barcode: String): ProductUnit? =
        error("findUnitByBarcode is not used in report tests")

    override suspend fun upsertUnit(unit: ProductUnit): Long =
        error("upsertUnit is not used in report tests")

    override suspend fun deleteUnit(unitId: Long) =
        error("deleteUnit is not used in report tests")
}

internal class RecordingBinarySink(private val failWrite: Boolean = false) : BinarySink {
    var writtenPath: String? = null
    var writtenBytes: ByteArray? = null

    override suspend fun write(targetPath: String, content: ByteArray): Long {
        if (failWrite) throw IllegalStateException("disk full")
        writtenPath = targetPath
        writtenBytes = content
        return content.size.toLong()
    }
}

internal class RecordingAdController(
    private val outcome: RewardedAdOutcome = RewardedAdOutcome.EARNED,
    private val shouldThrow: Boolean = false,
) : AdController {

    var requestedPlacement: AdPlacement? = null
    var callCount: Int = 0

    override fun observeAvailability(placement: AdPlacement): Flow<Boolean> = flowOf(true)

    override suspend fun showRewarded(placement: AdPlacement): RewardedAdOutcome {
        requestedPlacement = placement
        callCount++
        if (shouldThrow) throw IllegalStateException("rewarded ad sdk crashed")
        return outcome
    }
}

internal class RecordingCsvSink(private val failWrite: Boolean = false) : CsvSink {
    var writtenPath: String? = null
    var writtenContent: String? = null

    override suspend fun write(targetPath: String, content: String): Long {
        if (failWrite) throw IllegalStateException("disk full")
        writtenPath = targetPath
        writtenContent = content
        return content.toByteArray(Charsets.UTF_8).size.toLong()
    }
}

internal class FixedClock(private val now: Long) : Clock {
    override fun nowMillis(): Long = now
}
