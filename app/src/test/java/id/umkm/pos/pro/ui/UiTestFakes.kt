package id.umkm.pos.pro.ui

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.core.contract.BackupImportSummary
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.DataBackupService
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ScannerPreferences
import id.umkm.pos.pro.core.contract.ScannerInput
import id.umkm.pos.pro.core.contract.ScanEvent
import id.umkm.pos.pro.core.contract.ScanSource
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class FakeCatalogRepository : ProductRepository {

    private val products = MutableStateFlow<List<Product>>(emptyList())
    private val units = mutableMapOf<Long, List<ProductUnit>>()
    private var nextGeneratedId = 1_000L

    fun setProducts(values: List<Product>) {
        products.value = values
    }

    fun setUnits(productId: Long, values: List<ProductUnit>) {
        units[productId] = values
    }

    fun currentProducts(): List<Product> = products.value

    fun unitsFor(productId: Long): List<ProductUnit> = units[productId].orEmpty()

    override fun observeProducts(query: String?): Flow<List<Product>> = products.map { list ->
        list.filter { product -> matchesQuery(product, query) }
    }

    override suspend fun getProductById(productId: Long): Product? =
        products.value.firstOrNull { it.id == productId }

    override suspend fun findProductByBarcode(barcode: String): Product? =
        products.value.firstOrNull { it.barcode == barcode }

    override suspend fun upsertProduct(product: Product): Long {
        val productId = if (product.id == 0L) nextGeneratedId++ else product.id
        products.value = products.value.filterNot { it.id == productId } + product.copy(id = productId)
        return productId
    }

    override suspend fun deleteProduct(productId: Long) {
        products.value = products.value.filterNot { it.id == productId }
    }

    override suspend fun getUnits(productId: Long): List<ProductUnit> = units[productId].orEmpty()

    override suspend fun findUnitByBarcode(barcode: String): ProductUnit? =
        units.values.flatten().firstOrNull { it.customBarcode == barcode }

    override suspend fun upsertUnit(unit: ProductUnit): Long {
        val unitId = if (unit.id == 0L) nextGeneratedId++ else unit.id
        units[unit.productId] = units[unit.productId].orEmpty().filterNot { it.id == unitId } + unit.copy(id = unitId)
        return unitId
    }

    override suspend fun deleteUnit(unitId: Long) {
        units.keys.toList().forEach { key ->
            units[key] = units.getValue(key).filterNot { it.id == unitId }
        }
    }

    private fun matchesQuery(product: Product, query: String?): Boolean {
        val trimmed = query?.trim().orEmpty()
        if (trimmed.isEmpty()) return true
        return product.name.contains(trimmed, ignoreCase = true) ||
            product.sku.contains(trimmed, ignoreCase = true) ||
            product.barcode?.contains(trimmed, ignoreCase = true) == true
    }
}

class FakeCustomerStore : CustomerRepository {

    private val customers = MutableStateFlow<List<Customer>>(emptyList())
    private var nextGeneratedId = 1L

    fun setCustomers(values: List<Customer>) {
        customers.value = values
    }

    fun currentCustomers(): List<Customer> = customers.value

    override fun observeCustomers(query: String?): Flow<List<Customer>> = customers.map { list ->
        val trimmed = query?.trim().orEmpty()
        if (trimmed.isEmpty()) list else list.filter { it.name.contains(trimmed, ignoreCase = true) }
    }

    override suspend fun getCustomer(customerId: Long): Customer? =
        customers.value.firstOrNull { it.id == customerId }

    override suspend fun upsertCustomer(customer: Customer): Long {
        val customerId = if (customer.id == 0L) nextGeneratedId++ else customer.id
        customers.value = customers.value.filterNot { it.id == customerId } + customer.copy(id = customerId)
        return customerId
    }

    override suspend fun deleteCustomer(customerId: Long) {
        customers.value = customers.value.filterNot { it.id == customerId }
    }

    override fun observeDebtPayments(customerId: Long): Flow<List<DebtPayment>> = flowOf(emptyList())
}

class FakeTransactionStore : TransactionRepository {

    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextGeneratedId = 1L

    val recordedPayments = mutableListOf<DebtPayment>()

    fun setTransactions(values: List<Transaction>) {
        transactions.value = values
    }

    fun savedTransactions(): List<Transaction> = transactions.value

    override fun observeTransactions(range: ReportRange): Flow<List<Transaction>> = transactions.map { list ->
        list.filter { it.createdAt >= range.startMillis && it.createdAt <= range.endMillis }
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? =
        transactions.value.firstOrNull { it.id == transactionId }

    override suspend fun getItems(transactionId: Long): List<TransactionItem> = emptyList()

    override suspend fun saveTransaction(
        transaction: Transaction,
        items: List<TransactionItem>,
    ): AppResult<Long> {
        val transactionId = nextGeneratedId++
        transactions.value = transactions.value + transaction.copy(id = transactionId)
        return AppResult.Success(transactionId)
    }

    override suspend fun nextInvoiceNumber(createdAtMillis: Long): String = "INV-$createdAtMillis"

    override suspend fun recordDebtPayment(payment: DebtPayment): AppResult<Long> {
        val existing = transactions.value.firstOrNull { it.id == payment.transactionId }
            ?: return AppResult.Failure(AppError.NotFound("transaction ${payment.transactionId} was not found"))
        val remainingDebt = (existing.remainingDebt - payment.amount).coerceAtLeast(0L)
        transactions.value = transactions.value.map { transaction ->
            if (transaction.id == existing.id) {
                transaction.copy(
                    remainingDebt = remainingDebt,
                    status = if (remainingDebt == 0L) TransactionStatus.COMPLETED else TransactionStatus.PARTIAL_DEBT,
                )
            } else {
                transaction
            }
        }
        recordedPayments += payment
        return AppResult.Success(recordedPayments.size.toLong())
    }

    override suspend fun markReturned(
        transactionId: Long,
        returnedItems: List<TransactionItem>,
    ): AppResult<Unit> = AppResult.Success(Unit)
}

class FakeSettingsRepository(
    initialSettings: AppSettings = AppSettings(
        storeName = "Toko Uji",
        cashierName = null,
        taxRatePercent = 0,
        roundingIncrement = 0L,
        isPinEnabled = false,
        printerAddress = null,
        qrisImagePath = null,
    ),
) : SettingsRepository {

    private val settings = MutableStateFlow(initialSettings)

    override fun observeSettings(): Flow<AppSettings> = settings

    override suspend fun getSettings(): AppSettings = settings.value

    override suspend fun updateSettings(settings: AppSettings) {
        this.settings.value = settings
    }
}

class FakeDataBackupService(
    var exportResult: AppResult<BackupArtifact> = AppResult.Success(
        BackupArtifact(path = "umkm-pos-backup-20261019-120000.json", byteCount = 128L),
    ),
    var importResult: AppResult<BackupImportSummary> = AppResult.Success(
        BackupImportSummary(mapOf("products" to 3, "transactions" to 2)),
    ),
) : DataBackupService {

    var lastExportedName: String? = null
    var lastImportedUri: String? = null

    override suspend fun exportJson(displayName: String): AppResult<BackupArtifact> {
        lastExportedName = displayName
        return when (val result = exportResult) {
            is AppResult.Success -> AppResult.Success(result.value.copy(path = displayName))
            is AppResult.Failure -> result
        }
    }

    override suspend fun importJson(sourceUri: String): AppResult<BackupImportSummary> {
        lastImportedUri = sourceUri
        return importResult
    }
}

class FakeClock(private var currentMillis: Long = 1_700_000_000_000L) : Clock {

    override fun nowMillis(): Long = currentMillis

    fun setNow(millis: Long) {
        currentMillis = millis
    }
}

class FakeScannerPreferences(initiallyEnabled: Boolean = true) : ScannerPreferences {

    private val enabled = MutableStateFlow(initiallyEnabled)

    var lastSetValue: Boolean? = null

    override fun observeHidScannerEnabled(): Flow<Boolean> = enabled

    override suspend fun setHidScannerEnabled(isEnabled: Boolean) {
        lastSetValue = isEnabled
        enabled.value = isEnabled
    }
}

class FakeReceiptPrinter : ReceiptPrinter {

    var lastPrintedDocument: ReceiptDocument? = null
    var printResult: AppResult<Unit> = AppResult.Success(Unit)
    var lastPaperWidthMm: Int = 58
    var availablePrintersList: List<PrinterInfo> = emptyList()

    override val connectionState: Flow<PrinterConnection> = flowOf(PrinterConnection.DISCONNECTED)

    override suspend fun connect(printer: PrinterInfo): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun disconnect() = Unit

    override suspend fun print(receipt: ReceiptDocument): AppResult<Unit> {
        lastPrintedDocument = receipt
        return printResult
    }

    override fun getAvailablePrinters(): List<PrinterInfo> = availablePrintersList

    override fun setPaperWidth(paperWidthMm: Int) {
        lastPaperWidthMm = paperWidthMm
    }
}


class FakeReceiptRenderer : ReceiptRenderer {

    var lastWidthChars: Int = 0
    var lastRenderedDocument: ReceiptDocument? = null

    override fun renderText(receipt: ReceiptDocument, widthChars: Int): String {
        lastWidthChars = widthChars
        lastRenderedDocument = receipt
        return receipt.lines.joinToString(separator = "\n") { it.text }
    }

    override fun renderBitmap(receipt: ReceiptDocument, widthPx: Int): ByteArray = ByteArray(0)

    override fun renderPng(receipt: ReceiptDocument, widthPx: Int): ByteArray = ByteArray(0)
}

class FakeScannerInput : ScannerInput {

    private val events = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)

    override val scanEvents: Flow<ScanEvent> = events

    fun emitScan(code: String) {
        events.tryEmit(ScanEvent(code = code, source = ScanSource.HARDWARE_HID, timestampMillis = 0L))
    }
}

fun uiSampleProduct(
    id: Long,
    name: String = "Produk $id",
    basePrice: Long = 2_000L,
    baseHpp: Long = 1_000L,
    stockBaseQty: Double = 100.0,
    minStockAlert: Double = 5.0,
    isActive: Boolean = true,
    barcode: String? = null,
): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = barcode,
    name = name,
    category = "Umum",
    baseUnit = "pcs",
    baseHpp = baseHpp,
    basePrice = basePrice,
    stockBaseQty = stockBaseQty,
    minStockAlert = minStockAlert,
    isActive = isActive,
)

fun uiSampleCustomer(id: Long, name: String, totalDebt: Long): Customer = Customer(
    id = id,
    name = name,
    whatsappNumber = null,
    totalDebt = totalDebt,
    notes = null,
)

fun uiSampleDebtTransaction(
    id: Long,
    customerId: Long,
    remainingDebt: Long,
    paidAmount: Long = 0L,
    dueDate: Long? = null,
    createdAt: Long = 1_000L,
): Transaction = Transaction(
    id = id,
    invoiceNo = "INV-$id",
    createdAt = createdAt,
    cashierId = null,
    customerId = customerId,
    subtotal = remainingDebt + paidAmount,
    discountTotal = 0L,
    invoiceDiscount = 0L,
    taxTotal = 0L,
    rounding = 0L,
    grandTotal = remainingDebt + paidAmount,
    totalHpp = 0L,
    netProfit = 0L,
    paymentMethod = PaymentMethod.KASBON,
    paidAmount = paidAmount,
    changeAmount = 0L,
    remainingDebt = remainingDebt,
    dueDate = dueDate,
    status = if (remainingDebt == 0L) TransactionStatus.COMPLETED else TransactionStatus.PARTIAL_DEBT,
    notes = null,
)
