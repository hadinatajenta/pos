package id.umkm.pos.pro.ui

import id.umkm.pos.pro.core.contract.AccountingService
import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppContainer
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.core.contract.BackupImportSummary
import id.umkm.pos.pro.core.contract.DataBackupService
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.OnboardingStore
import id.umkm.pos.pro.core.contract.PinHasher
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.contract.ProductImageRepository
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.QrisImport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.contract.ScannerPreferences
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
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityCredentials
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.ShiftSwitchResult
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.domain.accounting.DefaultAccountingService
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.security.SecurityService
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

    override val productImageRepository: ProductImageRepository = object : ProductImageRepository {
        override suspend fun importImage(sourceUri: String): AppResult<String> = AppResult.Success(sourceUri)

        override suspend fun deleteImage(imagePath: String?) = Unit
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

        override suspend fun nextInvoiceNumber(createdAtMillis: Long): String = "INV-$createdAtMillis"

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

    private val defaultActiveShift = CashierShift(
        id = 1L,
        cashierName = "Kasir Uji",
        openedAt = 0L,
        closedAt = null,
        startingCash = 0L,
        expectedEndingCash = null,
        actualEndingCash = null,
    )

    private val activeShift = MutableStateFlow<CashierShift?>(defaultActiveShift)

    fun resetActiveShift() {
        activeShift.value = defaultActiveShift
    }

    override val shiftRepository: ShiftRepository = object : ShiftRepository {
        override fun observeActiveShift(): Flow<CashierShift?> = activeShift

        override suspend fun getActiveShift(): CashierShift? = activeShift.value

        override suspend fun getRecentCashierNames(limit: Int): List<String> =
            listOfNotNull(activeShift.value?.cashierName)

        override suspend fun getShiftHistory(limit: Int): List<CashierShift> =
            listOfNotNull(activeShift.value)

        override suspend fun getShiftSummary(shiftId: Long): AppResult<ShiftCashSummary> {
            val shift = activeShift.value
                ?: return AppResult.Failure(AppError.NotFound("shift $shiftId was not found"))
            return AppResult.Success(
                ShiftCashSummary(
                    startingCash = shift.startingCash,
                    cashSales = 0L,
                    cashDebtPayments = 0L,
                    cashRefunds = 0L,
                    transactionCount = 0,
                ),
            )
        }

        override suspend fun openShift(
            cashierName: String,
            startingCash: Long,
        ): AppResult<CashierShift> {
            val shift = CashierShift(
                id = (activeShift.value?.id ?: 0L) + 1L,
                cashierName = cashierName,
                openedAt = 0L,
                closedAt = null,
                startingCash = startingCash,
                expectedEndingCash = null,
                actualEndingCash = null,
            )
            activeShift.value = shift
            return AppResult.Success(shift)
        }

        override suspend fun closeShift(
            shiftId: Long,
            actualEndingCash: Long,
        ): AppResult<CashierShift> {
            val shift = activeShift.value
                ?: return AppResult.Failure(AppError.NotFound("shift $shiftId was not found"))
            val closed = shift.copy(closedAt = 0L, actualEndingCash = actualEndingCash)
            activeShift.value = null
            return AppResult.Success(closed)
        }

        override suspend fun switchShift(
            closeShiftId: Long,
            actualEndingCash: Long,
            nextCashierName: String,
            nextStartingCash: Long,
        ): AppResult<ShiftSwitchResult> {
            val current = activeShift.value
                ?: return AppResult.Failure(AppError.NotFound("shift $closeShiftId was not found"))
            val closed = current.copy(closedAt = 0L, actualEndingCash = actualEndingCash)
            val opened = CashierShift(
                id = current.id + 1L,
                cashierName = nextCashierName,
                openedAt = 0L,
                closedAt = null,
                startingCash = nextStartingCash,
                expectedEndingCash = null,
                actualEndingCash = null,
            )
            activeShift.value = opened
            return AppResult.Success(ShiftSwitchResult(closedShift = closed, openedShift = opened))
        }
    }

    override val securityRepository: SecurityRepository = object : SecurityRepository {
        override fun observeState(): Flow<SecurityState> = flowOf(SecurityState())

        override suspend fun getState(): SecurityState = SecurityState()

        override suspend fun getCredentials(): SecurityCredentials = SecurityCredentials(null, null)

        override suspend fun saveCredentials(
            pinHash: String,
            recoveryCodeHash: String,
            pinUpdatedAtMillis: Long,
            audit: SecurityAuditEntry?,
        ) = Unit

        override suspend fun clearCredentials(audit: SecurityAuditEntry?) = Unit

        override suspend fun recordAttempt(
            failedAttempts: Int,
            lockoutUntilMillis: Long?,
            audit: SecurityAuditEntry?,
        ) = Unit

        override suspend fun updateSession(role: SecurityRole?, unlockedUntilMillis: Long?) = Unit

        override suspend fun appendAudit(entry: SecurityAuditEntry) = Unit

        override suspend fun recentAudit(limit: Int): List<SecurityAuditEntry> = emptyList()

        override suspend fun trimAudit(keep: Int) = Unit
    }

    override val pinHasher: PinHasher = object : PinHasher {
        override fun hash(secret: String): String = secret

        override fun verify(secret: String, encodedHash: String): Boolean = secret == encodedHash
    }

    override val securityService: SecurityService = SecurityService(
        securityRepository = securityRepository,
        pinHasher = pinHasher,
        clock = object : Clock {
            override fun nowMillis(): Long = 0L
        },
    )

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

    var isOnboardingCompleted = true

    override val onboardingStore: OnboardingStore = object : OnboardingStore {
        override fun hasCompletedOnboarding(): Boolean = isOnboardingCompleted

        override fun markOnboardingCompleted() {
            isOnboardingCompleted = true
        }
    }

    override val dataBackupService: DataBackupService = object : DataBackupService {
        override suspend fun exportJson(displayName: String): AppResult<BackupArtifact> =
            AppResult.Failure(AppError.Unexpected("instrumented test container"))

        override suspend fun importJson(sourceUri: String): AppResult<BackupImportSummary> =
            AppResult.Failure(AppError.Unexpected("instrumented test container"))
    }

    override val scannerPreferences: ScannerPreferences = object : ScannerPreferences {
        override fun observeHidScannerEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setHidScannerEnabled(isEnabled: Boolean) = Unit
    }

    override val qrisService: QrisService = object : QrisService {
        override suspend fun importStaticQris(sourceUri: String): AppResult<QrisImport> =
            AppResult.Failure(AppError.Validation("test"))

        override suspend fun deleteQrisImage(imagePath: String?) = Unit
    }

    override val clock: Clock = object : Clock {
        override fun nowMillis(): Long = 1_000L
    }
}
