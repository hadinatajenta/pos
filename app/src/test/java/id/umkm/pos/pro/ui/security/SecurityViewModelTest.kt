package id.umkm.pos.pro.ui.security

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.PinHasher
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityAuditResult
import id.umkm.pos.pro.core.model.SecurityCredentials
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.domain.security.PinPolicy
import id.umkm.pos.pro.domain.security.RecoveryCodeGenerator
import id.umkm.pos.pro.domain.security.SecurityService
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SecurityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSecurityRepository()

    private val clock = MutableClock(NOW)

    private val service = SecurityService(
        securityRepository = repository,
        pinHasher = PrefixPinHasher,
        clock = clock,
        policy = PinPolicy(),
        recoveryCodeGenerator = RecoveryCodeGenerator(randomByte = { 0 }),
    )

    private fun createViewModel(): SecurityViewModel = SecurityViewModel(service, clock)

    private fun enableThroughTheUi(viewModel: SecurityViewModel) {
        viewModel.onStartEnablePin()
        viewModel.onNewPinChange("1234")
        viewModel.onConfirmPinChange("1234")
        viewModel.onSubmitEnablePin()
    }

    @Test
    fun enablingThePinShowsTheRecoveryCodeOnce() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        enableThroughTheUi(viewModel)

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.RECOVERY_CODE)
        assertThat(state.recoveryCode).isEqualTo(ALL_ZERO_CODE)
        assertThat(state.isRecoveryCodeAcknowledged).isFalse()
        assertThat(state.isPinEnabled).isTrue()
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("PIN aktif.")
        assertThat(repository.credentials.pinHash).isEqualTo("h:1234")
    }

    @Test
    fun enablingThePinRejectsAPinOutsideTheFourToEightDigitPolicy() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onStartEnablePin()
            viewModel.onNewPinChange("123")
            viewModel.onConfirmPinChange("123")
            viewModel.onSubmitEnablePin()

            val state = viewModel.uiState.value
            assertThat(state.step).isEqualTo(SecurityStep.ENABLE_PIN)
            assertThat(state.newPinError?.resolve(IdStrings)).isEqualTo("PIN harus 4–8 digit angka.")
            assertThat(state.recoveryCode).isNull()
            assertThat(repository.state.isPinEnabled).isFalse()
        }

    @Test
    fun enablingThePinRejectsAMismatchedConfirmation() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onStartEnablePin()
        viewModel.onNewPinChange("1234")
        viewModel.onConfirmPinChange("1235")
        viewModel.onSubmitEnablePin()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.ENABLE_PIN)
        assertThat(state.newPinError).isNull()
        assertThat(state.confirmPinError?.resolve(IdStrings))
            .isEqualTo("PIN baru dan ulangannya tidak sama.")
        assertThat(repository.state.isPinEnabled).isFalse()
    }

    @Test
    fun pinFieldsKeepOnlyDigitsUpToThePolicyMaximum() {
        val viewModel = createViewModel()

        viewModel.onStartEnablePin()
        viewModel.onNewPinChange("12a34-567890")

        assertThat(viewModel.uiState.value.newPinInput).isEqualTo("12345678")
    }

    @Test
    fun leavingTheRecoveryCodeScreenWarnsBeforeDroppingTheCode() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            enableThroughTheUi(viewModel)

            viewModel.onCloseStep()

            assertThat(viewModel.uiState.value.step).isEqualTo(SecurityStep.RECOVERY_CODE)
            assertThat(viewModel.uiState.value.warningMessage?.resolve(IdStrings))
                .isEqualTo(
                    "Belum dikonfirmasi. Cetak atau centang \"Saya sudah menyimpan kode ini\" sebelum keluar.",
                )

            viewModel.onCloseStep()

            val state = viewModel.uiState.value
            assertThat(state.step).isEqualTo(SecurityStep.OVERVIEW)
            assertThat(state.recoveryCode).isNull()
            assertThat(state.warningMessage?.resolve(IdStrings))
                .isEqualTo(
                    "PIN sudah aktif, tetapi kode pemulihan belum dikonfirmasi tersimpan. " +
                        "Buat ulang kode dari layar ini jika kode tersebut hilang.",
                )
        }

    @Test
    fun confirmingTheRecoveryCodeReturnsToTheOverview() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        enableThroughTheUi(viewModel)

        viewModel.onRecoveryCodeAcknowledgedChange(true)
        viewModel.onCloseStep()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.OVERVIEW)
        assertThat(state.recoveryCode).isNull()
        assertThat(state.warningMessage).isNull()
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("PIN aktif.")
    }

    @Test
    fun changingThePinReportsAWrongCurrentPin() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        val viewModel = createViewModel()

        viewModel.onStartChangePin()
        viewModel.onCurrentPinChange("9999")
        viewModel.onNewPinChange("5678")
        viewModel.onConfirmPinChange("5678")
        viewModel.onSubmitChangePin()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.CHANGE_PIN)
        assertThat(state.currentPinError?.resolve(IdStrings)).isEqualTo("PIN saat ini salah.")
        assertThat(repository.credentials.pinHash).isEqualTo("h:1234")
    }

    @Test
    fun changingThePinKeepsTheStoredRecoveryCode() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        val viewModel = createViewModel()

        viewModel.onStartChangePin()
        viewModel.onCurrentPinChange("1234")
        viewModel.onNewPinChange("5678")
        viewModel.onConfirmPinChange("5678")
        viewModel.onSubmitChangePin()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.OVERVIEW)
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("PIN berhasil diubah.")
        assertThat(repository.credentials.pinHash).isEqualTo("h:5678")
        assertThat(repository.credentials.recoveryCodeHash).isEqualTo("h:$ALL_ZERO_COMPACT")
    }

    @Test
    fun changingThePinShowsANewRecoveryCodeWhenNoneWasStored() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.state = SecurityState(isPinEnabled = true, pinUpdatedAtMillis = NOW)
            repository.credentials = SecurityCredentials(pinHash = "h:1234", recoveryCodeHash = null)
            val viewModel = createViewModel()

            viewModel.onStartChangePin()
            viewModel.onCurrentPinChange("1234")
            viewModel.onNewPinChange("5678")
            viewModel.onConfirmPinChange("5678")
            viewModel.onSubmitChangePin()

            val state = viewModel.uiState.value
            assertThat(state.step).isEqualTo(SecurityStep.RECOVERY_CODE)
            assertThat(state.recoveryCode).isEqualTo(ALL_ZERO_CODE)
            assertThat(state.isRecoveryCodeAcknowledged).isFalse()
        }

    @Test
    fun disablingThePinRequiresTheCurrentPin() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        val viewModel = createViewModel()

        viewModel.onStartDisablePin()
        viewModel.onCurrentPinChange("9999")
        viewModel.onSubmitDisablePin()

        assertThat(viewModel.uiState.value.currentPinError?.resolve(IdStrings))
            .isEqualTo("PIN saat ini salah.")
        assertThat(repository.state.isPinEnabled).isTrue()

        viewModel.onCurrentPinChange("1234")
        viewModel.onSubmitDisablePin()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.OVERVIEW)
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("PIN dimatikan.")
        assertThat(state.isPinEnabled).isFalse()
        assertThat(repository.credentials.pinHash).isNull()
    }

    @Test
    fun disablingThePinAsksForThePinWhenTheFieldIsEmpty() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        val viewModel = createViewModel()

        viewModel.onStartDisablePin()
        viewModel.onSubmitDisablePin()

        assertThat(viewModel.uiState.value.currentPinError?.resolve(IdStrings))
            .isEqualTo("Masukkan PIN saat ini.")
        assertThat(repository.state.isPinEnabled).isTrue()
    }

    @Test
    fun regeneratingTheRecoveryCodeShowsTheNewCodeOnce() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        val viewModel = createViewModel()

        viewModel.onStartRegenerateCode()
        viewModel.onCurrentPinChange("1234")
        viewModel.onSubmitRegenerateCode()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.RECOVERY_CODE)
        assertThat(state.recoveryCode).isEqualTo(ALL_ZERO_CODE)
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("Kode pemulihan baru dibuat.")
        assertThat(repository.credentials.pinHash).isEqualTo("h:1234")
    }

    @Test
    fun regeneratingTheRecoveryCodeRejectsAWrongPin() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        val viewModel = createViewModel()

        viewModel.onStartRegenerateCode()
        viewModel.onCurrentPinChange("0000")
        viewModel.onSubmitRegenerateCode()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(SecurityStep.REGENERATE_CODE)
        assertThat(state.currentPinError?.resolve(IdStrings)).isEqualTo("PIN saat ini salah.")
        assertThat(state.recoveryCode).isNull()
    }

    @Test
    fun lockingTheSessionClearsTheElevatedFlag() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.isSessionElevated).isTrue()

        viewModel.onLockSession()

        val state = viewModel.uiState.value
        assertThat(state.isSessionElevated).isFalse()
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("Sesi dikunci.")
        assertThat(repository.state.sessionRole).isNull()
    }

    @Test
    fun theAuditHistoryReadsTheFiftyNewestEntries() = runTest(mainDispatcherRule.testDispatcher) {
        service.enablePin("1234")
        repository.appendAudit(
            SecurityAuditEntry(
                occurredAtMillis = NOW + 1_000L,
                actionId = "export_backup",
                result = SecurityAuditResult.DENIED,
            ),
        )

        val viewModel = createViewModel()

        assertThat(repository.lastRequestedAuditLimit).isEqualTo(50)
        assertThat(viewModel.uiState.value.auditEntries).hasSize(2)
        assertThat(viewModel.uiState.value.auditEntries.first().actionId).isEqualTo("change_pin")
    }

    @Test
    fun theAuditHistoryIsEmptyOnAFreshStore() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.auditEntries).isEmpty()
        assertThat(viewModel.uiState.value.isPinEnabled).isFalse()
    }

    @Test
    fun messagesFollowTheActiveLanguage() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        enableThroughTheUi(viewModel)

        val message = viewModel.uiState.value.message
        assertThat(message?.resolve(EnStrings)).isEqualTo("PIN is on.")
        assertThat(message?.resolve(ZhStrings)).isEqualTo("PIN 已启用。")
    }

    @Test
    fun dismissingFeedbackClearsEveryBanner() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        enableThroughTheUi(viewModel)
        viewModel.onCloseStep()
        assertThat(viewModel.uiState.value.warningMessage).isNotNull()

        viewModel.onDismissFeedback()

        val state = viewModel.uiState.value
        assertThat(state.warningMessage).isNull()
        assertThat(state.message).isNull()
        assertThat(state.errorMessage).isNull()
    }

    private class MutableClock(var nowMillis: Long) : Clock {
        override fun nowMillis(): Long = nowMillis
    }

    private object PrefixPinHasher : PinHasher {
        override fun hash(secret: String): String = "h:$secret"

        override fun verify(secret: String, encodedHash: String): Boolean = encodedHash == hash(secret)
    }

    private class FakeSecurityRepository : SecurityRepository {
        var state: SecurityState = SecurityState()
        var credentials: SecurityCredentials = SecurityCredentials(null, null)
        val entries = mutableListOf<SecurityAuditEntry>()
        var lastRequestedAuditLimit: Int = 0

        override fun observeState(): Flow<SecurityState> = flowOf(state)

        override suspend fun getState(): SecurityState = state

        override suspend fun getCredentials(): SecurityCredentials = credentials

        override suspend fun saveCredentials(
            pinHash: String,
            recoveryCodeHash: String,
            pinUpdatedAtMillis: Long,
            audit: SecurityAuditEntry?,
        ) {
            credentials = SecurityCredentials(pinHash, recoveryCodeHash)
            state = state.copy(
                isPinEnabled = true,
                failedAttempts = 0,
                lockoutUntilMillis = null,
                pinUpdatedAtMillis = pinUpdatedAtMillis,
                recoveryCodeConfigured = true,
            )
            audit?.let { entries += it }
        }

        override suspend fun clearCredentials(audit: SecurityAuditEntry?) {
            credentials = SecurityCredentials(null, null)
            state = SecurityState()
            audit?.let { entries += it }
        }

        override suspend fun recordAttempt(
            failedAttempts: Int,
            lockoutUntilMillis: Long?,
            audit: SecurityAuditEntry?,
        ) {
            state = state.copy(
                failedAttempts = failedAttempts,
                lockoutUntilMillis = lockoutUntilMillis,
            )
            audit?.let { entries += it }
        }

        override suspend fun updateSession(role: SecurityRole?, unlockedUntilMillis: Long?) {
            state = state.copy(
                sessionRole = role,
                sessionUnlockedUntilMillis = unlockedUntilMillis,
            )
        }

        override suspend fun appendAudit(entry: SecurityAuditEntry) {
            entries += entry
        }

        override suspend fun recentAudit(limit: Int): List<SecurityAuditEntry> {
            lastRequestedAuditLimit = limit
            return entries.takeLast(limit)
        }

        override suspend fun trimAudit(keep: Int) = Unit
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val ALL_ZERO_CODE = "00000-00000-00000-00000"
        const val ALL_ZERO_COMPACT = "00000000000000000000"
    }
}
