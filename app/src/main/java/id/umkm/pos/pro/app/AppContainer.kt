package id.umkm.pos.pro.app

import id.umkm.pos.pro.core.contract.AccountingService
import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.BackupService
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.ImportExportService
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.ScannerInput
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.contract.TransactionRepository

interface AppContainer {
    val productRepository: ProductRepository
    val customerRepository: CustomerRepository
    val transactionRepository: TransactionRepository
    val shiftRepository: ShiftRepository
    val securityRepository: SecurityRepository
    val settingsRepository: SettingsRepository
    val reportRepository: ReportRepository
    val priceEngine: PriceEngine
    val accountingService: AccountingService
    val receiptPrinter: ReceiptPrinter
    val receiptRenderer: ReceiptRenderer
    val scannerInput: ScannerInput
    val backupService: BackupService
    val importExportService: ImportExportService
    val adController: AdController
    val clock: Clock
}
