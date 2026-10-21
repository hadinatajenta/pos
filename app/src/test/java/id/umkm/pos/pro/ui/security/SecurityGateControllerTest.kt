package id.umkm.pos.pro.ui.security

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.PinHasher
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityCredentials
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.domain.security.PinPolicy
import id.umkm.pos.pro.domain.security.RecoveryCodeGenerator
import id.umkm.pos.pro.domain.security.SecurityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SecurityGateControllerTest {

    private companion object {
        const val NOW = 1_700_000_000_000L
    }

    private val repository = FakeSecurityRepository()
    private val clock = MutableClock(NOW)
    private val service = SecurityService(
        securityRepository = repository,
        pinHasher = PrefixHasher,
        clock = clock,
        policy = PinPolicy(),
        recoveryCodeGenerator = RecoveryCodeGenerator(randomByte = { 0 }),
    )
    private val controller = SecurityGateController(
        securityService = service,
        scope = CoroutineScope(Dispatchers.Unconfined),
        clock = clock,
    )

    @Test
    fun withNoPinConfiguredEveryActionRunsImmediately() = runTest {
        var granted = 0

        CriticalAction.entries.forEach { action ->
            controller.request(action, label = "action") { granted += 1 }
        }

        assertThat(granted).isEqualTo(CriticalAction.entries.size)
        assertThat(controller.pendingAction).isNull()
    }

    @Test
    fun anUnlockedSessionLetsManagerActionsThroughWithoutAPrompt() = runTest {
        service.enablePin("1234")
        controller.refreshState()
        var granted = 0

        controller.request(CriticalAction.VIEW_PROFIT_AND_COST, label = "profit") { granted += 1 }

        assertThat(granted).isEqualTo(1)
        assertThat(controller.isUnlocked(CriticalAction.VIEW_PROFIT_AND_COST)).isTrue()
    }

    @Test
    fun ownerActionsAskEvenInsideAValidSession() = runTest {
        service.enablePin("1234")
        controller.refreshState()
        var granted = 0

        controller.request(CriticalAction.CHANGE_PIN, label = "pin") { granted += 1 }

        assertThat(granted).isEqualTo(0)
        assertThat(controller.pendingAction).isEqualTo(CriticalAction.CHANGE_PIN)
        assertThat(controller.pendingLabel).isEqualTo("pin")
        assertThat(controller.isUnlocked(CriticalAction.EXPORT_BACKUP)).isFalse()
    }

    @Test
    fun anExpiredSessionAsksAgain() = runTest {
        service.enablePin("1234")
        controller.refreshState()
        assertThat(controller.isUnlocked(CriticalAction.VIEW_PROFIT_AND_COST)).isTrue()

        clock.nowMillis = NOW + 5 * 60_000L + 1

        assertThat(controller.isUnlocked(CriticalAction.VIEW_PROFIT_AND_COST)).isFalse()
        assertThat(controller.isUnlocked(CriticalAction.PROCESS_RETURN)).isFalse()
    }

    @Test
    fun theCorrectPinGrantsThePendingAction() = runTest {
        service.enablePin("1234")
        service.lockSession()
        controller.refreshState()
        var granted = 0
        controller.request(CriticalAction.ITEM_DISCOUNT_ABOVE_THRESHOLD, label = "discount") {
            granted += 1
        }

        controller.submitPin("1234")

        assertThat(granted).isEqualTo(1)
        assertThat(controller.pendingAction).isNull()
        assertThat(controller.feedback).isNull()
        assertThat(controller.state.sessionRole).isEqualTo(SecurityRole.OWNER)
    }

    @Test
    fun aWrongPinReportsTheRemainingAttemptsAndDoesNotGrant() = runTest {
        service.enablePin("1234")
        service.lockSession()
        controller.refreshState()
        var granted = 0
        controller.request(CriticalAction.VIEW_PROFIT_AND_COST, label = "profit") { granted += 1 }

        controller.submitPin("9999")

        assertThat(granted).isEqualTo(0)
        assertThat(controller.pendingAction).isEqualTo(CriticalAction.VIEW_PROFIT_AND_COST)
        assertThat(controller.feedback).isEqualTo(
            GateFeedback.WrongPin(remainingAttempts = 4, retryDelaySeconds = 0),
        )
    }

    @Test
    fun aSecondAttemptInsideTheRetryDelayNeverReachesTheService() = runTest {
        service.enablePin("1234")
        service.lockSession()
        repeat(2) { service.unlockWithPin("9999") }
        controller.refreshState()
        controller.request(CriticalAction.VIEW_PROFIT_AND_COST, label = "profit") {}

        controller.submitPin("9999")
        assertThat(controller.feedback).isEqualTo(
            GateFeedback.WrongPin(remainingAttempts = 2, retryDelaySeconds = 5),
        )

        controller.submitPin("9999")
        assertThat(controller.feedback).isEqualTo(GateFeedback.RetryLater(seconds = 5))
        assertThat(repository.state.failedAttempts).isEqualTo(3)
    }

    @Test
    fun aLockoutIsReportedInMinutesEvenWithTheCorrectPin() = runTest {
        service.enablePin("1234")
        repeat(5) { service.unlockWithPin("9999") }
        controller.refreshState()
        var granted = 0
        controller.request(CriticalAction.CHANGE_PIN, label = "pin") { granted += 1 }

        controller.submitPin("1234")

        assertThat(granted).isEqualTo(0)
        assertThat(controller.feedback).isEqualTo(GateFeedback.Locked(minutes = 5))
    }

    @Test
    fun theRecoveryCodeGrantsAndTurnsThePinOff() = runTest {
        service.enablePin("1234")
        service.lockSession()
        controller.refreshState()
        var granted = 0
        controller.request(CriticalAction.RESTORE_BACKUP, label = "restore") { granted += 1 }

        controller.submitRecoveryCode("00000-00000-00000-00000")

        assertThat(granted).isEqualTo(1)
        assertThat(controller.pendingAction).isNull()
        assertThat(repository.state.isPinEnabled).isFalse()
        assertThat(controller.state.isPinEnabled).isFalse()
    }

    @Test
    fun aWrongRecoveryCodeIsReported() = runTest {
        service.enablePin("1234")
        service.lockSession()
        controller.refreshState()
        var granted = 0
        controller.request(CriticalAction.RESTORE_BACKUP, label = "restore") { granted += 1 }

        controller.submitRecoveryCode("11111-11111-11111-11111")

        assertThat(granted).isEqualTo(0)
        assertThat(controller.feedback).isEqualTo(GateFeedback.RecoveryInvalid)
        assertThat(controller.pendingAction).isEqualTo(CriticalAction.RESTORE_BACKUP)
    }

    @Test
    fun cancellingDropsThePendingActionWithoutGranting() = runTest {
        service.enablePin("1234")
        service.lockSession()
        controller.refreshState()
        var granted = 0
        controller.request(CriticalAction.DELETE_PRODUCT, label = "delete") { granted += 1 }

        controller.cancel()

        assertThat(granted).isEqualTo(0)
        assertThat(controller.pendingAction).isNull()
        assertThat(controller.feedback).isNull()
    }

    private class MutableClock(var nowMillis: Long) : Clock {
        override fun nowMillis(): Long = nowMillis
    }

    private object PrefixHasher : PinHasher {
        override fun hash(secret: String): String = "h:$secret"

        override fun verify(secret: String, encodedHash: String): Boolean = encodedHash == hash(secret)
    }

    private class FakeSecurityRepository : SecurityRepository {
        var state: SecurityState = SecurityState()
        var credentials: SecurityCredentials = SecurityCredentials(null, null)

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
        }

        override suspend fun clearCredentials(audit: SecurityAuditEntry?) {
            credentials = SecurityCredentials(null, null)
            state = SecurityState()
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
        }

        override suspend fun updateSession(role: SecurityRole?, unlockedUntilMillis: Long?) {
            state = state.copy(
                sessionRole = role,
                sessionUnlockedUntilMillis = unlockedUntilMillis,
            )
        }

        override suspend fun appendAudit(entry: SecurityAuditEntry) = Unit

        override suspend fun recentAudit(limit: Int): List<SecurityAuditEntry> = emptyList()

        override suspend fun trimAudit(keep: Int) = Unit
    }
}
