package id.umkm.pos.pro.ui.settings

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.QrisImport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.data.backup.BackupFileNames
import id.umkm.pos.pro.ui.FakeClock
import id.umkm.pos.pro.ui.FakeDataBackupService
import id.umkm.pos.pro.ui.FakeScannerInput
import id.umkm.pos.pro.ui.FakeScannerPreferences
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

    private val backupService = FakeDataBackupService()

    private val clock = FakeClock()

    private val scannerPreferences = FakeScannerPreferences()

    private val scannerInput = FakeScannerInput()

    private var importResult: AppResult<QrisImport> =
        AppResult.Success(QrisImport(imagePath = "/data/qris.jpg", payload = "000201payload"))

    private var deletedImagePath: String? = null

    private val qrisService = object : QrisService {
        override suspend fun importStaticQris(sourceUri: String): AppResult<QrisImport> = importResult

        override suspend fun deleteQrisImage(imagePath: String?) {
            deletedImagePath = imagePath
        }
    }

    private fun kotlinx.coroutines.test.TestScope.runCurrent() {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            settings,
            qrisService,
            backupService,
            scannerPreferences,
            scannerInput,
            clock,
        )

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
            dataBackupService = backupService,
            scannerPreferences = scannerPreferences,
            scannerInput = scannerInput,
            clock = clock,
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
            dataBackupService = backupService,
            scannerPreferences = scannerPreferences,
            scannerInput = scannerInput,
            clock = clock,
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
            dataBackupService = backupService,
            scannerPreferences = scannerPreferences,
            scannerInput = scannerInput,
            clock = clock,
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

    @Test
    fun exportingBackupWritesATimestampedJsonFile() = runTest(mainDispatcherRule.testDispatcher) {
        clock.setNow(1_700_000_000_000L)
        val viewModel = createViewModel()

        viewModel.onExportBackup()

        val expectedName = BackupFileNames.forTimestamp(1_700_000_000_000L)
        assertThat(backupService.lastExportedName).isEqualTo(expectedName)
        val backupState = viewModel.uiState.value.backupState
        assertThat(backupState).isInstanceOf(BackupUiState.Exported::class.java)
        assertThat((backupState as BackupUiState.Exported).fileName).isEqualTo(expectedName)
    }

    @Test
    fun exportingBackupReportsAStorageFailure() = runTest(mainDispatcherRule.testDispatcher) {
        backupService.exportResult = AppResult.Failure(AppError.Storage("disk full"))
        val viewModel = createViewModel()

        viewModel.onExportBackup()

        val backupState = viewModel.uiState.value.backupState
        assertThat(backupState).isInstanceOf(BackupUiState.Failed::class.java)
        assertThat((backupState as BackupUiState.Failed).error).isInstanceOf(AppError.Storage::class.java)
    }

    @Test
    fun importingBackupReportsTheRestoredRowCount() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onImportBackup("content://backup.json")

        assertThat(backupService.lastImportedUri).isEqualTo("content://backup.json")
        val backupState = viewModel.uiState.value.backupState
        assertThat(backupState).isInstanceOf(BackupUiState.Imported::class.java)
        assertThat((backupState as BackupUiState.Imported).rowCount).isEqualTo(5)
    }

    @Test
    fun importingAnInvalidBackupReportsAValidationFailure() = runTest(mainDispatcherRule.testDispatcher) {
        backupService.importResult = AppResult.Failure(AppError.Validation("invalid"))
        val viewModel = createViewModel()

        viewModel.onImportBackup("content://broken.json")

        val backupState = viewModel.uiState.value.backupState
        assertThat(backupState).isInstanceOf(BackupUiState.Failed::class.java)
        assertThat((backupState as BackupUiState.Failed).error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun dismissingTheBackupStateReturnsToIdle() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onExportBackup()

        viewModel.onDismissBackupState()

        assertThat(viewModel.uiState.value.backupState).isEqualTo(BackupUiState.Idle)
    }

    @Test
    fun turningOffTheHardwareScannerPersistsTheChoice() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onSetHidScannerEnabled(false)
        runCurrent()

        assertThat(scannerPreferences.lastSetValue).isFalse()
        assertThat(viewModel.uiState.value.isHidScannerEnabled).isFalse()
    }

    @Test
    fun aScanFromTheHardwareScannerIsShownAsTheLastReadCode() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        scannerInput.emitScan("8991234567890")
        runCurrent()

        assertThat(viewModel.uiState.value.lastScannedCode).isEqualTo("8991234567890")
    }
}
