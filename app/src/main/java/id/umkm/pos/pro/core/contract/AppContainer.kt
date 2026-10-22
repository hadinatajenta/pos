package id.umkm.pos.pro.core.contract

import id.umkm.pos.pro.domain.security.SecurityService

interface AppContainer {
    val productRepository: ProductRepository
    val productImageRepository: ProductImageRepository
    val customerRepository: CustomerRepository
    val transactionRepository: TransactionRepository
    val returnRepository: ReturnRepository
    val shiftRepository: ShiftRepository
    val staffRepository: StaffRepository
    val securityRepository: SecurityRepository
    val pinHasher: PinHasher
    val securityService: SecurityService
    val settingsRepository: SettingsRepository
    val reportRepository: ReportRepository
    val priceEngine: PriceEngine
    val accountingService: AccountingService
    val receiptPrinter: ReceiptPrinter
    val receiptRenderer: ReceiptRenderer
    val scannerInput: ScannerInput
    val scannerPreferences: ScannerPreferences
    val adController: AdController
    val onboardingStore: OnboardingStore
    val qrisService: QrisService
    val dataBackupService: DataBackupService
    val clock: Clock
}
