package id.umkm.pos.pro.core.contract

interface AppContainer {
    val productRepository: ProductRepository
    val customerRepository: CustomerRepository
    val transactionRepository: TransactionRepository
    val returnRepository: ReturnRepository
    val shiftRepository: ShiftRepository
    val securityRepository: SecurityRepository
    val settingsRepository: SettingsRepository
    val reportRepository: ReportRepository
    val priceEngine: PriceEngine
    val accountingService: AccountingService
    val receiptPrinter: ReceiptPrinter
    val receiptRenderer: ReceiptRenderer
    val scannerInput: ScannerInput
    val adController: AdController
    val clock: Clock
}
