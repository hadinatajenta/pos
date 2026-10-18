package id.umkm.pos.pro.ui.settings

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.QrisImport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.ui.FakeSettingsRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
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
        assertThat(viewModel.uiState.value.message).isEqualTo("QRIS statis berhasil disimpan")
        assertThat(viewModel.uiState.value.isQrisConfigured).isTrue()
    }

    @Test
    fun unreadableQrisReportsAnErrorAndKeepsSettingsEmpty() = runTest(mainDispatcherRule.testDispatcher) {
        importResult = AppResult.Failure(AppError.Validation("unreadable"))
        val viewModel = createViewModel()

        viewModel.onQrisImagePicked("content://picked")

        assertThat(viewModel.uiState.value.errorMessage).isNotNull()
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
            assertThat(viewModel.uiState.value.message).isEqualTo("QRIS statis dihapus")
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
        assertThat(viewModel.uiState.value.message).isEqualTo("Pengaturan toko berhasil disimpan")
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

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Nama toko tidak boleh kosong.")
    }
}
