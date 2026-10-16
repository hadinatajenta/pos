package id.umkm.pos.pro.ui

import id.umkm.pos.pro.core.contract.AccountingService
import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.AppContainer
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.contract.ScannerInput
import id.umkm.pos.pro.core.contract.ScanEvent
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.ReturnLine
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.domain.accounting.DefaultAccountingService
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.hardware.receipt.AndroidReceiptRenderer
import id.umkm.pos.pro.monetization.admob.NoAdsController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

internal object AppRootFakeContainer : AppContainer {

    private val products = MutableStateFlow<List<Product>>(emptyList())
    private val customers = MutableStateFlow<List<Customer>>(emptyList())
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val settings = MutableStateFlow(
        AppSettings(
            storeName = "Toko Uji",
            cashierName = null,
            taxRatePercent = 0,
            roundingIncrement = 0L,
            isPinEnabled = false,
            printerAddress = null,
            qrisImagePath = null,
        ),
    )

    override val productRepository: ProductRepository = object : ProductRepository {
        override fun observeProducts(query: String?): Flow<List<Product>> = products

        override suspend fun getProductById(productId: Long): Product? =
            products.value.firstOrNull { it.id == productId }

        override suspend fun findProductByBarcode(barcode: String): Product? = null

        override suspend fun upsertProduct(product: Product): Long = product.id

        override suspend fun deleteProduct(productId: Long) = Unit

        override suspend fun getUnits(productId: Long): List<ProductUnit> = emptyList()

        override suspend fun findUnitByBarcode(barcode: String): ProductUnit? = null

        override suspend fun upsertUnit(unit: ProductUnit): Long = unit.id

        override suspend fun deleteUnit(unitId: Long) = Unit
    }

    override val customerRepository: CustomerRepository = object : CustomerRepository {
        override fun observeCustomers(query: String?): Flow<List<Customer>> = customers

        override suspend fun getCustomer(customerId: Long): Customer? =
            customers.value.firstOrNull { it.id == customerId }

        override suspend fun upsertCustomer(customer: Customer): Long = customer.id

        override suspend fun deleteCustomer(customerId: Long) = Unit

        override fun observeDebtPayments(customerId: Long): Flow<List<DebtPayment>> = flowOf(emptyList())
    }

    override val transactionRepository: TransactionRepository = object : TransactionRepository {
        override suspend fun saveTransaction(
            transaction: Transaction,
            items: List<TransactionItem>,
        ): AppResult<Long> = AppResult.Success(0L)

        override suspend fun getTransaction(transactionId: Long): Transaction? = null

        override fun observeTransactions(range: ReportRange): Flow<List<Transaction>> = transactions

        override suspend fun getItems(transactionId: Long): List<TransactionItem> = emptyList()

        override suspend fun recordDebtPayment(payment: DebtPayment): AppResult<Long> = AppResult.Success(0L)

        override suspend fun markReturned(
            transactionId: Long,
            returnedItems: List<TransactionItem>,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    override val returnRepository: ReturnRepository = object : ReturnRepository {
        override suspend fun recordReturn(record: ReturnRecord, lines: List<ReturnLine>): AppResult<Long> =
            AppResult.Success(0L)

        override suspend fun getReturns(transactionId: Long): List<ReturnRecord> = emptyList()

        override suspend fun returnsInRange(range: ReportRange): List<ReturnRecord> = emptyList()
    }

    override val shiftRepository: ShiftRepository = object : ShiftRepository {
        override suspend fun openShift(shift: CashierShift): Long = 0L

        override suspend fun closeShift(shiftId: Long, actualEndingCash: Long) = Unit

        override suspend fun getActiveShift(): CashierShift? = null
    }

    override val securityRepository: SecurityRepository = object : SecurityRepository {
        override suspend fun isPinEnabled(): Boolean = false

        override suspend fun setPin(pinHash: String, recoveryAnswerHash: String) = Unit

        override suspend fun clearPin() = Unit

        override suspend fun verifyPin(pinHash: String): Boolean = false

        override suspend fun verifyRecoveryAnswer(recoveryAnswerHash: String): Boolean = false
    }

    override val settingsRepository: SettingsRepository = object : SettingsRepository {
        override fun observeSettings(): Flow<AppSettings> = settings

        override suspend fun getSettings(): AppSettings = settings.value

        override suspend fun updateSettings(settings: AppSettings) {
            this@AppRootFakeContainer.settings.value = settings
        }
    }

    override val reportRepository: ReportRepository = object : ReportRepository {
        override suspend fun salesSummary(range: ReportRange): SalesSummary =
            SalesSummary(totalSales = 0L, totalHpp = 0L, netProfit = 0L, transactionCount = 0)

        override suspend fun salesByDay(range: ReportRange): List<DailySales> = emptyList()

        override suspend fun transactionsInRange(range: ReportRange): List<Transaction> = emptyList()
    }

    override val priceEngine: PriceEngine = DefaultPriceEngine()

    override val accountingService: AccountingService = DefaultAccountingService()

    override val receiptPrinter: ReceiptPrinter = object : ReceiptPrinter {
        override val connectionState: Flow<PrinterConnection> = flowOf(PrinterConnection.DISCONNECTED)

        override suspend fun connect(printer: PrinterInfo): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun disconnect() = Unit

        override suspend fun print(receipt: ReceiptDocument): AppResult<Unit> = AppResult.Success(Unit)
    }

    override val receiptRenderer: ReceiptRenderer = AndroidReceiptRenderer()

    override val scannerInput: ScannerInput = object : ScannerInput {
        override val scanEvents: Flow<ScanEvent> = emptyFlow()
    }

    override val adController: AdController = NoAdsController()

    override val clock: Clock = object : Clock {
        override fun nowMillis(): Long = 1_000L
    }
}
