package id.umkm.pos.pro.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.domain.security.PinPolicy
import id.umkm.pos.pro.domain.security.SecurityService
import id.umkm.pos.pro.domain.security.UnlockOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface SecurityGate {
    fun isUnlocked(action: CriticalAction): Boolean

    fun request(action: CriticalAction, label: String, onGranted: () -> Unit)

    fun policy(): PinPolicy
}

object OpenSecurityGate : SecurityGate {
    override fun isUnlocked(action: CriticalAction): Boolean = true

    override fun request(action: CriticalAction, label: String, onGranted: () -> Unit) =
        onGranted()

    override fun policy(): PinPolicy = PinPolicy()
}

sealed interface GateFeedback {
    data class WrongPin(val remainingAttempts: Int, val retryDelaySeconds: Int) : GateFeedback

    data class RetryLater(val seconds: Int) : GateFeedback

    data class Locked(val minutes: Int) : GateFeedback

    data object RecoveryInvalid : GateFeedback

    data object RecoveryUnavailable : GateFeedback

    data object Unavailable : GateFeedback
}

class SecurityGateController(
    private val securityService: SecurityService,
    private val scope: CoroutineScope,
    private val clock: Clock,
) : SecurityGate {

    var state: SecurityState by mutableStateOf(SecurityState())
        private set

    var pendingAction: CriticalAction? by mutableStateOf(null)
        private set

    var pendingLabel: String by mutableStateOf("")
        private set

    var feedback: GateFeedback? by mutableStateOf(null)
        private set

    private var pendingGrant: (() -> Unit)? = null
    private var retryAtMillis: Long = 0L

    override fun policy(): PinPolicy = securityService.policy()

    override fun isUnlocked(action: CriticalAction): Boolean {
        if (!state.isPinEnabled) return true
        if (policy().alwaysPrompts(action)) return false
        val role = state.activeSessionRole(clock.nowMillis()) ?: return false
        return role.covers(policy().requiredRole(action))
    }

    override fun request(action: CriticalAction, label: String, onGranted: () -> Unit) {
        if (isUnlocked(action)) {
            onGranted()
            return
        }
        pendingAction = action
        pendingLabel = label
        pendingGrant = onGranted
        feedback = null
    }

    fun clearFeedback() {
        feedback = null
    }

    fun cancel() {
        val abandoned = pendingAction
        clearPending()
        if (abandoned != null) {
            scope.launch { securityService.recordDecision(abandoned, granted = false) }
        }
    }

    fun submitPin(pin: String) {
        val now = clock.nowMillis()
        val remainingDelay = retryAtMillis - now
        if (remainingDelay > 0L) {
            feedback = GateFeedback.RetryLater(seconds = ceilSeconds(remainingDelay))
            return
        }
        val action = pendingAction
        scope.launch {
            when (val outcome = securityService.unlockWithPin(pin, action)) {
                is UnlockOutcome.Granted -> completeGrant()

                is UnlockOutcome.WrongSecret -> {
                    val delaySeconds = (outcome.retryDelayMillis / 1_000L).toInt()
                    retryAtMillis = if (outcome.retryDelayMillis > 0L) {
                        now + outcome.retryDelayMillis
                    } else {
                        0L
                    }
                    feedback = GateFeedback.WrongPin(
                        remainingAttempts = outcome.remainingAttempts,
                        retryDelaySeconds = delaySeconds,
                    )
                }

                is UnlockOutcome.Locked -> feedback = GateFeedback.Locked(
                    minutes = ceilMinutes(outcome.untilMillis - clock.nowMillis()),
                )

                UnlockOutcome.NotConfigured -> {
                    refreshState()
                    feedback = GateFeedback.Unavailable
                }

                UnlockOutcome.RecoveryCodeUnavailable ->
                    feedback = GateFeedback.RecoveryUnavailable
            }
        }
    }

    fun submitRecoveryCode(code: String) {
        val action = pendingAction
        scope.launch {
            when (val outcome = securityService.unlockWithRecoveryCode(code, action)) {
                is UnlockOutcome.Granted -> completeGrant()

                is UnlockOutcome.WrongSecret -> feedback = GateFeedback.RecoveryInvalid

                is UnlockOutcome.Locked -> feedback = GateFeedback.Locked(
                    minutes = ceilMinutes(outcome.untilMillis - clock.nowMillis()),
                )

                UnlockOutcome.NotConfigured -> {
                    refreshState()
                    feedback = GateFeedback.Unavailable
                }

                UnlockOutcome.RecoveryCodeUnavailable ->
                    feedback = GateFeedback.RecoveryUnavailable
            }
        }
    }

    suspend fun refreshState() {
        state = securityService.state()
    }

    suspend fun start() {
        securityService.observeState().collect { latest -> state = latest }
    }

    private suspend fun completeGrant() {
        val grant = pendingGrant
        clearPending()
        refreshState()
        grant?.invoke()
    }

    private fun clearPending() {
        pendingAction = null
        pendingLabel = ""
        pendingGrant = null
        feedback = null
        retryAtMillis = 0L
    }

    private fun ceilSeconds(millis: Long): Int =
        ((millis.coerceAtLeast(0L) + 999L) / 1_000L).toInt()

    private fun ceilMinutes(millis: Long): Int =
        ((millis.coerceAtLeast(0L) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
}

val LocalSecurityGate = staticCompositionLocalOf<SecurityGate> { OpenSecurityGate }

@Composable
fun rememberSecurityGate(): SecurityGate = LocalSecurityGate.current

@Composable
fun rememberSecurityGateController(
    securityService: SecurityService,
    clock: Clock,
): SecurityGateController {
    val scope = rememberCoroutineScope()
    val controller = remember(securityService, clock) {
        SecurityGateController(securityService, scope, clock)
    }
    LaunchedEffect(controller) {
        controller.start()
    }
    return controller
}
