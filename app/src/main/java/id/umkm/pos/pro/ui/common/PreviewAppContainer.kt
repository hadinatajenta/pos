package id.umkm.pos.pro.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import id.umkm.pos.pro.core.contract.AccountingService
import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AppContainer
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.OnboardingStore
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.ProductImageRepository
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.QrisImport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.contract.ScannerInput
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.monetization.admob.NoAdsController
import id.umkm.pos.pro.ui.LocalAppContainer

internal object PreviewAppContainer : AppContainer {

    override val adController: AdController = NoAdsController()

    override val onboardingStore: OnboardingStore = object : OnboardingStore {
        override fun hasCompletedOnboarding(): Boolean = true

        override fun markOnboardingCompleted() = Unit
    }

    override val qrisService: QrisService = object : QrisService {
        override suspend fun importStaticQris(sourceUri: String): AppResult<QrisImport> =
            AppResult.Failure(AppError.Validation("preview"))

        override suspend fun deleteQrisImage(imagePath: String?) = Unit
    }

    override val productRepository: ProductRepository get() = previewUnavailable("productRepository")
    override val productImageRepository: ProductImageRepository get() = previewUnavailable("productImageRepository")
    override val customerRepository: CustomerRepository get() = previewUnavailable("customerRepository")
    override val transactionRepository: TransactionRepository get() = previewUnavailable("transactionRepository")
    override val returnRepository: ReturnRepository get() = previewUnavailable("returnRepository")
    override val shiftRepository: ShiftRepository get() = previewUnavailable("shiftRepository")
    override val securityRepository: SecurityRepository get() = previewUnavailable("securityRepository")
    override val settingsRepository: SettingsRepository get() = previewUnavailable("settingsRepository")
    override val reportRepository: ReportRepository get() = previewUnavailable("reportRepository")
    override val priceEngine: PriceEngine get() = previewUnavailable("priceEngine")
    override val accountingService: AccountingService get() = previewUnavailable("accountingService")
    override val receiptPrinter: ReceiptPrinter get() = previewUnavailable("receiptPrinter")
    override val receiptRenderer: ReceiptRenderer get() = previewUnavailable("receiptRenderer")
    override val scannerInput: ScannerInput get() = previewUnavailable("scannerInput")
    override val clock: Clock get() = previewUnavailable("clock")

    private fun previewUnavailable(member: String): Nothing =
        error("PreviewAppContainer does not provide $member")
}

@Composable
internal fun PreviewAppContainerProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppContainer provides PreviewAppContainer, content = content)
}
