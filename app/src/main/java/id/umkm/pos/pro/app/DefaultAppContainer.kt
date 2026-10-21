package id.umkm.pos.pro.app

import android.content.Context
import android.os.Build
import id.umkm.pos.pro.BuildConfig
import id.umkm.pos.pro.core.contract.AccountingService
import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AppContainer
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.DataBackupService
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.OnboardingStore
import id.umkm.pos.pro.core.contract.PinHasher
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.ProductImageRepository
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ScannerPreferences
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.contract.ScannerInput
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.data.image.ProductImageStore
import id.umkm.pos.pro.data.backup.JsonDataBackupService
import id.umkm.pos.pro.data.file.DownloadsFileSink
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.OnboardingPreferences
import id.umkm.pos.pro.data.local.repository.CustomerRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ProductRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ReportRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ReturnRepositoryImpl
import id.umkm.pos.pro.data.local.repository.SecurityRepositoryImpl
import id.umkm.pos.pro.data.local.repository.SettingsRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ShiftRepositoryImpl
import id.umkm.pos.pro.data.local.repository.TransactionRepositoryImpl
import id.umkm.pos.pro.data.qris.AndroidQrisService
import id.umkm.pos.pro.data.seed.DefaultDemoDataSeeder
import id.umkm.pos.pro.data.seed.DemoDataSeeder
import id.umkm.pos.pro.domain.accounting.DefaultAccountingService
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.hardware.printer.BluetoothReceiptPrinter
import id.umkm.pos.pro.hardware.receipt.AndroidReceiptRenderer
import id.umkm.pos.pro.data.local.ScannerPreferenceStore
import id.umkm.pos.pro.data.local.security.Pbkdf2PinHasher
import id.umkm.pos.pro.data.local.security.PinHashAlgorithm
import id.umkm.pos.pro.domain.security.SecurityService
import id.umkm.pos.pro.hardware.scanner.HardwareScannerInput
import id.umkm.pos.pro.monetization.admob.NoAdsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class DefaultAppContainer(
    context: Context,
    applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AppContainer {

    private val applicationContext: Context = context.applicationContext
    private val database: UmkmDatabase = UmkmDatabase.build(applicationContext)


    override val clock: Clock = SystemClock

    override val productRepository: ProductRepository = ProductRepositoryImpl(database.productDao())

    override val productImageRepository: ProductImageRepository = ProductImageStore(applicationContext)

    override val customerRepository: CustomerRepository =
        CustomerRepositoryImpl(database.customerDao(), database.debtPaymentDao())

    override val transactionRepository: TransactionRepository = TransactionRepositoryImpl(
        database,
        database.transactionDao(),
        database.debtPaymentDao(),
        database.customerDao(),
    )

    override val returnRepository: ReturnRepository = ReturnRepositoryImpl(
        database,
        database.returnDao(),
        database.transactionDao(),
        database.productDao(),
    )

    override val shiftRepository: ShiftRepository =
        ShiftRepositoryImpl(database.shiftDao(), database.transactionDao(), clock)

    override val securityRepository: SecurityRepository = SecurityRepositoryImpl(
        database,
        database.securityDao(),
        database.securityAuditDao(),
        database.settingsDao(),
    )

    override val pinHasher: PinHasher = Pbkdf2PinHasher(
        defaultAlgorithm = PinHashAlgorithm.forApiLevel(Build.VERSION.SDK_INT),
    )

    override val securityService: SecurityService = SecurityService(
        securityRepository = securityRepository,
        pinHasher = pinHasher,
        clock = clock,
    )

    override val settingsRepository: SettingsRepository = SettingsRepositoryImpl(database.settingsDao())

    override val reportRepository: ReportRepository =
        ReportRepositoryImpl(database.reportDao(), database.transactionDao())

    override val priceEngine: PriceEngine = DefaultPriceEngine()

    override val accountingService: AccountingService = DefaultAccountingService()

    override val receiptRenderer: ReceiptRenderer = AndroidReceiptRenderer()

    override val receiptPrinter: ReceiptPrinter =
        BluetoothReceiptPrinter(applicationContext, receiptRenderer, lifecycleScope = applicationScope)

    val hidScanner: HardwareScannerInput = HardwareScannerInput()

    override val scannerInput: ScannerInput = hidScanner

    override val scannerPreferences: ScannerPreferences = ScannerPreferenceStore(applicationContext)

    override val adController: AdController = NoAdsController()

    override val onboardingStore: OnboardingStore = OnboardingPreferences(applicationContext)

    override val qrisService: QrisService = AndroidQrisService(applicationContext)

    private val downloadsSink: DownloadsFileSink = DownloadsFileSink(applicationContext)

    override val dataBackupService: DataBackupService =
        JsonDataBackupService(applicationContext, database, downloadsSink, clock)

    init {
        applicationScope.launch {
            scannerPreferences.observeHidScannerEnabled().collect { isEnabled ->
                hidScanner.setEnabled(isEnabled)
            }
        }
        applicationScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                receiptPrinter.setPaperWidth(settings.paperWidthMm)
            }
        }
    }


    val demoDataSeeder: DemoDataSeeder? =
        if (BuildConfig.DEBUG) {
            DefaultDemoDataSeeder(
                database = database,
                productRepository = productRepository,
                customerRepository = customerRepository,
                transactionRepository = transactionRepository,
                returnRepository = returnRepository,
                priceEngine = priceEngine,
                anchorClock = clock,
            )
        } else {
            null
        }
}
