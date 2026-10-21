package id.umkm.pos.pro.domain.security

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.PinHasher
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityAuditResult
import id.umkm.pos.pro.core.model.SecurityCredentials
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SecurityServiceTest {

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val ALL_ZERO_CODE = "00000-00000-00000-00000"
        const val ALL_ZERO_COMPACT = "00000000000000000000"
    }

    private val repository = FakeSecurityRepository()
    private val clock = MutableClock(NOW)
    private val service = SecurityService(
        securityRepository = repository,
        pinHasher = PrefixPinHasher,
        clock = clock,
        policy = PinPolicy(),
        recoveryCodeGenerator = RecoveryCodeGenerator(randomByte = { 0 }),
    )

    @Test
    fun nothingRequiresAPinWhileThePinIsNotConfigured() = runTest {
        CriticalAction.entries.forEach { action ->
            assertThat(service.requiresPin(action)).isFalse()
        }
        assertThat(service.isElevated(CriticalAction.RESTORE_BACKUP)).isTrue()
    }

    @Test
    fun enablingThePinStoresHashesAndReturnsTheRecoveryCodeOnce() = runTest {
        val result = service.enablePin("1234")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success<String>).value).isEqualTo(ALL_ZERO_CODE)
        assertThat(repository.credentials.pinHash).isEqualTo("h:1234")
        assertThat(repository.credentials.recoveryCodeHash).isEqualTo("h:$ALL_ZERO_COMPACT")
        assertThat(repository.state.isPinEnabled).isTrue()
        assertThat(repository.state.pinUpdatedAtMillis).isEqualTo(NOW)
        assertThat(repository.entries.map { it.result })
            .containsExactly(SecurityAuditResult.PIN_ENABLED)
    }

    @Test
    fun enablingThePinRejectsInputThatIsNotFourToEightDigits() = runTest {
        listOf("123", "123456789", "12a4", "", "12 4").forEach { invalid ->
            val result = service.enablePin(invalid)

            assertThat(result).isInstanceOf(AppResult.Failure::class.java)
            assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        }
        assertThat(repository.state.isPinEnabled).isFalse()
    }

    @Test
    fun ownerLevelActionsKeepAskingInsideAValidSession() = runTest {
        service.enablePin("1234")

        assertThat(service.requiresPin(CriticalAction.VIEW_PROFIT_AND_COST)).isFalse()
        assertThat(service.requiresPin(CriticalAction.CHANGE_PIN)).isTrue()
        assertThat(service.requiresPin(CriticalAction.RESTORE_BACKUP)).isTrue()
        assertThat(service.requiresPin(CriticalAction.EXPORT_BACKUP)).isTrue()
    }

    @Test
    fun theSessionExpiresAfterTheConfiguredWindow() = runTest {
        service.enablePin("1234")
        assertThat(service.requiresPin(CriticalAction.VIEW_PROFIT_AND_COST)).isFalse()

        clock.nowMillis = NOW + 5 * 60_000L + 1

        assertThat(service.requiresPin(CriticalAction.VIEW_PROFIT_AND_COST)).isTrue()
    }

    @Test
    fun lockSessionEndsTheElevationImmediately() = runTest {
        service.enablePin("1234")
        service.lockSession()

        assertThat(service.requiresPin(CriticalAction.VIEW_PROFIT_AND_COST)).isTrue()
        assertThat(repository.state.sessionRole).isNull()
        assertThat(repository.state.sessionUnlockedUntilMillis).isNull()
    }

    @Test
    fun aCorrectPinResetsTheFailedAttemptsAndStartsASession() = runTest {
        service.enablePin("1234")
        service.unlockWithPin("9999")
        assertThat(repository.state.failedAttempts).isEqualTo(1)

        val outcome = service.unlockWithPin("1234")

        assertThat(outcome).isEqualTo(UnlockOutcome.Granted(SecurityRole.OWNER, NOW + 5 * 60_000L))
        assertThat(repository.state.failedAttempts).isEqualTo(0)
        assertThat(repository.state.lockoutUntilMillis).isNull()
        assertThat(repository.state.sessionRole).isEqualTo(SecurityRole.OWNER)
    }

    @Test
    fun wrongPinCountsDownAndLocksTheGateOnTheFifthAttempt() = runTest {
        service.enablePin("1234")

        val first = service.unlockWithPin("9999")
        assertThat(first).isEqualTo(
            UnlockOutcome.WrongSecret(failedAttempts = 1, remainingAttempts = 4, retryDelayMillis = 0L),
        )

        val second = service.unlockWithPin("9999")
        assertThat(second).isEqualTo(
            UnlockOutcome.WrongSecret(failedAttempts = 2, remainingAttempts = 3, retryDelayMillis = 0L),
        )
        service.unlockWithPin("9999")
        val fourth = service.unlockWithPin("9999")
        assertThat(fourth).isEqualTo(
            UnlockOutcome.WrongSecret(failedAttempts = 4, remainingAttempts = 1, retryDelayMillis = 10_000L),
        )

        val fifth = service.unlockWithPin("9999")

        assertThat(fifth).isEqualTo(UnlockOutcome.Locked(NOW + 5 * 60_000L))
        assertThat(repository.state.lockoutUntilMillis).isEqualTo(NOW + 5 * 60_000L)
        assertThat(repository.entries.last().result).isEqualTo(SecurityAuditResult.LOCKED)
    }

    @Test
    fun aCorrectPinIsRejectedWhileTheGateIsLocked() = runTest {
        service.enablePin("1234")
        repeat(5) { service.unlockWithPin("9999") }

        val outcome = service.unlockWithPin("1234")

        assertThat(outcome).isEqualTo(UnlockOutcome.Locked(NOW + 5 * 60_000L))
        assertThat(repository.state.sessionRole).isNull()
    }

    @Test
    fun theGateOpensAgainAfterTheLockoutWindow() = runTest {
        service.enablePin("1234")
        repeat(5) { service.unlockWithPin("9999") }

        clock.nowMillis = NOW + 5 * 60_000L

        assertThat(service.unlockWithPin("1234"))
            .isInstanceOf(UnlockOutcome.Granted::class.java)
    }

    @Test
    fun tenFailuresEscalateToTheLongerLockout() = runTest {
        service.enablePin("1234")
        repeat(5) { service.unlockWithPin("9999") }
        assertThat(repository.state.failedAttempts).isEqualTo(5)
        assertThat(repository.state.lockoutUntilMillis).isEqualTo(NOW + 5 * 60_000L)

        clock.nowMillis = NOW + 5 * 60_000L
        assertThat(service.unlockWithPin("9999")).isEqualTo(UnlockOutcome.Locked(NOW + 10 * 60_000L))
        assertThat(repository.state.failedAttempts).isEqualTo(6)

        clock.nowMillis = NOW + 10 * 60_000L
        service.unlockWithPin("9999")
        clock.nowMillis = NOW + 15 * 60_000L
        service.unlockWithPin("9999")
        clock.nowMillis = NOW + 20 * 60_000L
        service.unlockWithPin("9999")
        assertThat(repository.state.failedAttempts).isEqualTo(9)

        clock.nowMillis = NOW + 25 * 60_000L
        val outcome = service.unlockWithPin("9999")

        assertThat(repository.state.failedAttempts).isEqualTo(10)
        assertThat(outcome).isEqualTo(UnlockOutcome.Locked(NOW + 25 * 60_000L + 15 * 60_000L))
    }

    @Test
    fun theRecoveryCodeTurnsThePinOffAndGrantsASession() = runTest {
        service.enablePin("1234")

        val outcome = service.unlockWithRecoveryCode("00000-00000-00000-00000")

        assertThat(outcome).isEqualTo(UnlockOutcome.Granted(SecurityRole.OWNER, sessionUntilMillis = null))
        assertThat(repository.state.isPinEnabled).isFalse()
        assertThat(repository.state.sessionRole).isNull()
        assertThat(repository.state.sessionUnlockedUntilMillis).isNull()
        assertThat(repository.credentials.pinHash).isNull()
        assertThat(repository.credentials.recoveryCodeHash).isNull()
        assertThat(repository.entries.map { it.result })
            .contains(SecurityAuditResult.RECOVERY_USED)
    }

    @Test
    fun theRecoveryCodeIsSingleUseBecauseItClearsThePin() = runTest {
        service.enablePin("1234")
        service.unlockWithRecoveryCode("00000-00000-00000-00000")

        assertThat(service.unlockWithRecoveryCode("00000-00000-00000-00000"))
            .isEqualTo(UnlockOutcome.NotConfigured)
    }

    @Test
    fun anUnknownRecoveryCodeIsRejectedAndCountsAsAFailure() = runTest {
        service.enablePin("1234")

        val outcome = service.unlockWithRecoveryCode("11111-11111-11111-11111")

        assertThat(outcome).isEqualTo(
            UnlockOutcome.WrongSecret(failedAttempts = 1, remainingAttempts = 4, retryDelayMillis = 0L),
        )
        assertThat(repository.state.isPinEnabled).isTrue()
    }

    @Test
    fun theRecoveryPathReportsWhenNoCodeIsStored() = runTest {
        repository.state = SecurityState(isPinEnabled = true)
        repository.credentials = SecurityCredentials(pinHash = "h:1234", recoveryCodeHash = null)

        assertThat(service.unlockWithRecoveryCode("00000-00000-00000-00000"))
            .isEqualTo(UnlockOutcome.RecoveryCodeUnavailable)
    }

    @Test
    fun unlockingReportsNotConfiguredWhenThePinIsOff() = runTest {
        assertThat(service.unlockWithPin("1234")).isEqualTo(UnlockOutcome.NotConfigured)
        assertThat(service.unlockWithRecoveryCode("00000-00000-00000-00000"))
            .isEqualTo(UnlockOutcome.NotConfigured)
    }

    @Test
    fun changePinRequiresTheCurrentPinAndKeepsTheExistingRecoveryCode() = runTest {
        service.enablePin("1234")

        assertThat(service.changePin(currentPin = "9999", newPin = "5678"))
            .isEqualTo(PinChangeOutcome.WrongCurrentPin)
        assertThat(service.changePin(currentPin = "1234", newPin = "5678"))
            .isEqualTo(PinChangeOutcome.Changed)
        assertThat(repository.credentials.pinHash).isEqualTo("h:5678")
        assertThat(repository.credentials.recoveryCodeHash).isEqualTo("h:$ALL_ZERO_COMPACT")
        assertThat(repository.entries.map { it.result })
            .contains(SecurityAuditResult.PIN_CHANGED)
    }

    @Test
    fun changePinRejectsInvalidInputBeforeCheckingTheCurrentPin() = runTest {
        service.enablePin("1234")

        assertThat(service.changePin(currentPin = "1234", newPin = "abc"))
            .isEqualTo(PinChangeOutcome.InvalidNewPin)
        assertThat(repository.credentials.pinHash).isEqualTo("h:1234")
    }

    @Test
    fun changePinIssuesANewRecoveryCodeWhenTheStoredOneIsMissing() = runTest {
        repository.state = SecurityState(isPinEnabled = true)
        repository.credentials = SecurityCredentials(pinHash = "h:1234", recoveryCodeHash = null)

        val outcome = service.changePin(currentPin = "1234", newPin = "4321")

        assertThat(outcome).isEqualTo(PinChangeOutcome.ChangedWithNewRecoveryCode(ALL_ZERO_CODE))
        assertThat(repository.credentials.recoveryCodeHash).isEqualTo("h:$ALL_ZERO_COMPACT")
    }

    @Test
    fun changePinReportsNotConfiguredWhenThePinIsOff() = runTest {
        assertThat(service.changePin(currentPin = "1234", newPin = "5678"))
            .isEqualTo(PinChangeOutcome.NotConfigured)
    }

    @Test
    fun disablePinRequiresTheCurrentPin() = runTest {
        service.enablePin("1234")

        assertThat(service.disablePin("9999")).isInstanceOf(AppResult.Failure::class.java)
        assertThat(repository.state.isPinEnabled).isTrue()

        assertThat(service.disablePin("1234")).isInstanceOf(AppResult.Success::class.java)
        assertThat(repository.state.isPinEnabled).isFalse()
        assertThat(repository.credentials.pinHash).isNull()
        assertThat(repository.entries.map { it.result })
            .contains(SecurityAuditResult.PIN_DISABLED)
    }

    @Test
    fun regeneratingTheRecoveryCodeKeepsThePinAndReplacesTheStoredHash() = runTest {
        val enabled = service.enablePin("1234")
        val firstCode = (enabled as AppResult.Success<String>).value

        val regenerated = service.regenerateRecoveryCode("1234")
        val secondCode = (regenerated as AppResult.Success<String>).value

        val rotating = SecurityService(
            securityRepository = repository,
            pinHasher = PrefixPinHasher,
            clock = clock,
            recoveryCodeGenerator = RecoveryCodeGenerator(randomByte = { 31 }),
        )
        val third = rotating.regenerateRecoveryCode("1234")

        assertThat(firstCode).isEqualTo(ALL_ZERO_CODE)
        assertThat(secondCode).isEqualTo(ALL_ZERO_CODE)
        assertThat((third as AppResult.Success<String>).value).isEqualTo("ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ")
        assertThat(repository.credentials.recoveryCodeHash).isEqualTo("h:" + "Z".repeat(20))
        assertThat(repository.credentials.pinHash).isEqualTo("h:1234")
        assertThat(repository.entries.map { it.result })
            .contains(SecurityAuditResult.RECOVERY_CODE_REGENERATED)
    }

    @Test
    fun regeneratingTheRecoveryCodeRequiresThePin() = runTest {
        service.enablePin("1234")

        assertThat(service.regenerateRecoveryCode("9999")).isInstanceOf(AppResult.Failure::class.java)
        assertThat(repository.entries.map { it.result })
            .doesNotContain(SecurityAuditResult.RECOVERY_CODE_REGENERATED)
    }

    @Test
    fun recordDecisionLogsBothGrantsAndDenialsAndTrimsTheLog() = runTest {
        val trimsBefore = repository.trimCalls

        service.recordDecision(
            action = CriticalAction.PROCESS_RETURN,
            granted = true,
            targetType = "transaction",
            targetId = "42",
        )
        service.recordDecision(
            action = CriticalAction.PROCESS_RETURN,
            granted = false,
            targetType = "transaction",
            targetId = "42",
        )

        assertThat(repository.entries.map { it.result })
            .containsExactly(SecurityAuditResult.GRANTED, SecurityAuditResult.DENIED)
            .inOrder()
        assertThat(repository.entries.first().actionId).isEqualTo("process_return")
        assertThat(repository.entries.first().targetType).isEqualTo("transaction")
        assertThat(repository.entries.first().targetId).isEqualTo("42")
        assertThat(repository.entries.first().occurredAtMillis).isEqualTo(NOW)
        assertThat(repository.trimCalls).isEqualTo(trimsBefore + 2)
    }

    @Test
    fun theAuditLogIsTrimmedToThePolicyRetentionAfterEveryWrite() = runTest {
        service.recordDecision(CriticalAction.RESTORE_BACKUP, granted = true)

        assertThat(repository.lastTrim).isEqualTo(PinPolicy().auditRetentionEntries)
    }

    @Test
    fun observeStateComesFromTheRepository() = runTest {
        assertThat(service.state().isPinEnabled).isFalse()

        service.enablePin("1234")

        assertThat(repository.state.isPinEnabled).isTrue()
        assertThat(service.policy().sessionUnlockMinutes).isEqualTo(5)
    }

    @Test
    fun recentAuditAsksTheRepositoryForTheNewestEntries() = runTest {
        service.recordDecision(CriticalAction.PROCESS_RETURN, granted = true)

        assertThat(service.recentAudit(limit = 10)).hasSize(1)
        assertThat(repository.lastRequestedAuditLimit).isEqualTo(10)
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
        var trimCalls: Int = 0
        var lastTrim: Int = 0
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

        override suspend fun trimAudit(keep: Int) {
            trimCalls += 1
            lastTrim = keep
        }
    }
}
