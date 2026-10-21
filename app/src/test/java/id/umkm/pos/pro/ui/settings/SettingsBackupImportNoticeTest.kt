package id.umkm.pos.pro.ui.settings

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupImportSummary
import id.umkm.pos.pro.core.contract.QrisImport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.ui.FakeClock
import id.umkm.pos.pro.ui.FakeDataBackupService
import id.umkm.pos.pro.ui.FakeScannerInput
import id.umkm.pos.pro.ui.FakeScannerPreferences
import id.umkm.pos.pro.ui.FakeSettingsRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsBackupImportNoticeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val qrisService = object : QrisService {
        override suspend fun importStaticQris(sourceUri: String): AppResult<QrisImport> =
            AppResult.Success(QrisImport(imagePath = "/data/qris.jpg", payload = "000201payload"))

        override suspend fun deleteQrisImage(imagePath: String?) = Unit
    }

    private fun createViewModel(backupService: FakeDataBackupService): SettingsViewModel =
        SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            qrisService = qrisService,
            dataBackupService = backupService,
            scannerPreferences = FakeScannerPreferences(),
            scannerInput = FakeScannerInput(),
            clock = FakeClock(),
        )

    @Test
    fun aRestoreThatResetsThePinCarriesTheNotice() = runTest(mainDispatcherRule.testDispatcher) {
        val backupService = FakeDataBackupService(
            importResult = AppResult.Success(
                BackupImportSummary(
                    tableRowCounts = mapOf("products" to 4, "customers" to 1),
                    securityResetFromBackup = true,
                    ignoredSecurityData = true,
                ),
            ),
        )
        val viewModel = createViewModel(backupService)

        viewModel.onImportBackup("content://backup.json")

        val backupState = viewModel.uiState.value.backupState
        assertThat(backupState).isInstanceOf(BackupUiState.Imported::class.java)
        val imported = backupState as BackupUiState.Imported
        assertThat(imported.rowCount).isEqualTo(5)
        assertThat(imported.securityResetFromBackup).isTrue()
    }

    @Test
    fun aRestoreWithoutASecurityResetSkipsTheNotice() = runTest(mainDispatcherRule.testDispatcher) {
        val backupService = FakeDataBackupService(
            importResult = AppResult.Success(BackupImportSummary(mapOf("products" to 3))),
        )
        val viewModel = createViewModel(backupService)

        viewModel.onImportBackup("content://backup.json")

        val imported = viewModel.uiState.value.backupState as BackupUiState.Imported
        assertThat(imported.securityResetFromBackup).isFalse()
    }
}
