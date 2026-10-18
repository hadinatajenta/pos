package id.umkm.pos.pro.ui.settings

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.QrisImport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.ui.FakeReceiptPrinter
import id.umkm.pos.pro.ui.FakeSettingsRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import kotlinx.coroutines.test.runTest

import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    private var importResult: AppResult<QrisImport> =
        AppResult.Success(QrisImport(imagePath = "/data/qris.jpg", payload = "000201payload"))

    private var deletedImagePath: String? = null

    private val qrisService = object : QrisService {
        override suspend fun importStaticQris(sourceUri: String): AppResult<QrisImport> = importResult

        override suspend fun deleteQrisImage(imagePath: String?) {
            deletedImagePath = imagePath
        }
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(settings, qrisService)

    @Test
    fun importingAStaticQrisStoresTheImageAndPayload() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onQrisImagePicked("content://picked")

        val stored = settings.getSettings()
        assertThat(stored.qrisImagePath).isEqualTo("/data/qris.jpg")
        assertThat(stored.qrisPayload).isEqualTo("000201payload")
        assertThat(viewModel.uiState.value.message?.resolve(IdStrings))
            .isEqualTo("QRIS statis berhasil disimpan")
        assertThat(viewModel.uiState.value.isQrisConfigured).isTrue()
    }

    @Test
    fun unreadableQrisReportsAnErrorAndKeepsSettingsEmpty() = runTest(mainDispatcherRule.testDispatcher) {
        importResult = AppResult.Failure(AppError.Validation("unreadable"))
        val viewModel = createViewModel()

        viewModel.onQrisImagePicked("content://picked")

        assertThat(viewModel.uiState.value.errorMessage?.resolve(IdStrings))
            .isEqualTo("Gambar QRIS tidak terbaca. Pastikan foto QRIS terlihat utuh dan jelas.")
        assertThat(settings.getSettings().qrisPayload).isNull()
        assertThat(viewModel.uiState.value.isQrisConfigured).isFalse()
    }

    @Test
    fun removingTheQrisClearsTheStoredValueAndDeletesTheImage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onQrisImagePicked("content://picked")

            viewModel.onRemoveQris()

            assertThat(settings.getSettings().qrisPayload).isNull()
            assertThat(settings.getSettings().qrisImagePath).isNull()
            assertThat(deletedImagePath).isEqualTo("/data/qris.jpg")
            assertThat(viewModel.uiState.value.message?.resolve(IdStrings)).isEqualTo("QRIS statis dihapus")
        }

    @Test
    fun savingStoreSettingsUpdatesStoreProfileAndRules() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onSaveStoreSettings(
            storeName = "  Toko Maju Bersama  ",
            cashierName = "  Mbak Rini ",
            taxRatePercent = 11,
            roundingIncrement = 500L,
        )

        val stored = settings.getSettings()
        assertThat(stored.storeName).isEqualTo("Toko Maju Bersama")
        assertThat(stored.cashierName).isEqualTo("Mbak Rini")
        assertThat(stored.taxRatePercent).isEqualTo(11)
        assertThat(stored.roundingIncrement).isEqualTo(500L)
        assertThat(viewModel.uiState.value.message?.resolve(IdStrings))
            .isEqualTo("Pengaturan toko berhasil disimpan")
    }

    @Test
    fun savingStoreSettingsRejectsBlankStoreName() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onSaveStoreSettings(
            storeName = "   ",
            cashierName = "Kasir",
            taxRatePercent = 0,
            roundingIncrement = 0L,
        )

        assertThat(viewModel.uiState.value.errorMessage?.resolve(IdStrings))
            .isEqualTo("Nama toko tidak boleh kosong.")
    }

    @Test
    fun savingStoreSettingsBlocksPersistingWhenStoreNameIsBlank() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onSaveStoreSettings(
            storeName = "Toko Maju Bersama",
            cashierName = "Mbak Rini",
            taxRatePercent = 11,
            roundingIncrement = 500L,
        )

        viewModel.onSaveStoreSettings(
            storeName = "   ",
            cashierName = "Kasir",
            taxRatePercent = 0,
            roundingIncrement = 0L,
        )

        val stored = settings.getSettings()
        assertThat(stored.storeName).isEqualTo("Toko Maju Bersama")
        assertThat(stored.cashierName).isEqualTo("Mbak Rini")
        assertThat(stored.taxRatePercent).isEqualTo(11)
        assertThat(stored.roundingIncrement).isEqualTo(500L)
    }

    @Test
    fun selectingLanguageUpdatesStoredLanguageAndShowsFeedback() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onSelectLanguage("en")
        assertThat(settings.getSettings().languageCode).isEqualTo("en")
        assertThat(viewModel.uiState.value.message?.resolve(IdStrings)).isEqualTo("Language switched to English")

        viewModel.onSelectLanguage("zh")
        assertThat(settings.getSettings().languageCode).isEqualTo("zh")
        assertThat(viewModel.uiState.value.message?.resolve(IdStrings)).isEqualTo("语言已切换为中文")

        viewModel.onSelectLanguage("id")
        assertThat(settings.getSettings().languageCode).isEqualTo("id")
        assertThat(viewModel.uiState.value.message?.resolve(IdStrings))
            .isEqualTo("Bahasa berhasil diubah ke Bahasa Indonesia")
    }

    @Test
    fun languageFeedbackFollowsTheActiveCatalogue() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onSelectLanguage("id")
        val message = viewModel.uiState.value.message
        assertThat(message?.resolve(IdStrings)).isEqualTo("Bahasa berhasil diubah ke Bahasa Indonesia")
        assertThat(message?.resolve(EnStrings)).isEqualTo("Language switched to Indonesian")
        assertThat(message?.resolve(ZhStrings)).isEqualTo("语言已切换为印尼语")

        viewModel.onSelectLanguage("zh")
        val chineseMessage = viewModel.uiState.value.message
        assertThat(chineseMessage?.resolve(IdStrings)).isEqualTo("语言已切换为中文")
        assertThat(chineseMessage?.resolve(ZhStrings)).isEqualTo("语言已切换为中文")
    }

    @Test
    fun selectingPrinterUpdatesPrinterSettingsAndAutoConfiguresWidth() = runTest(mainDispatcherRule.testDispatcher) {
        val printer = FakeReceiptPrinter()
        val viewModel = SettingsViewModel(
            settingsRepository = settings,
            qrisService = qrisService,
            receiptPrinter = printer,
        )

        val sim80 = id.umkm.pos.pro.core.contract.PrinterInfo(
            name = "Thermal Simulator (80mm)",
            address = "SIMULATOR_80MM",
            transport = id.umkm.pos.pro.core.contract.PrinterTransport.BLUETOOTH_RFCOMM,
        )
        viewModel.onSelectPrinter(sim80)

        val stored = settings.getSettings()
        assertThat(stored.printerAddress).isEqualTo("SIMULATOR_80MM")
        assertThat(stored.printerName).isEqualTo("Thermal Simulator (80mm)")
        assertThat(stored.paperWidthMm).isEqualTo(80)
        assertThat(printer.lastPaperWidthMm).isEqualTo(80)
    }

    @Test
    fun settingPaperWidthUpdatesSettingsAndReceiptPrinter() = runTest(mainDispatcherRule.testDispatcher) {
        val printer = FakeReceiptPrinter()
        val viewModel = SettingsViewModel(
            settingsRepository = settings,
            qrisService = qrisService,
            receiptPrinter = printer,
        )

        viewModel.onSetPaperWidth(80)
        assertThat(settings.getSettings().paperWidthMm).isEqualTo(80)
        assertThat(printer.lastPaperWidthMm).isEqualTo(80)

        viewModel.onSetPaperWidth(58)
        assertThat(settings.getSettings().paperWidthMm).isEqualTo(58)
        assertThat(printer.lastPaperWidthMm).isEqualTo(58)
    }

    @Test
    fun testPrintInvokesPrinterAndOpensPreviewDialog() = runTest(mainDispatcherRule.testDispatcher) {
        val printer = FakeReceiptPrinter()
        val viewModel = SettingsViewModel(
            settingsRepository = settings,
            qrisService = qrisService,
            receiptPrinter = printer,
        )

        viewModel.onTestPrint()

        assertThat(printer.lastPrintedDocument).isNotNull()
        assertThat(viewModel.uiState.value.isPreviewDialogOpen).isTrue()
        assertThat(viewModel.uiState.value.message?.resolve(IdStrings))
            .isEqualTo("Struk uji coba berhasil dicetak!")

        viewModel.onDismissReceiptPreview()
        assertThat(viewModel.uiState.value.isPreviewDialogOpen).isFalse()
    }
}

