package id.umkm.pos.pro.domain.security

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.PinHasher
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityAuditResult
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import kotlinx.coroutines.flow.Flow

sealed interface UnlockOutcome {
    data class Granted(
        val role: SecurityRole,
        val sessionUntilMillis: Long?,
    ) : UnlockOutcome

    data class WrongSecret(
        val failedAttempts: Int,
        val remainingAttempts: Int,
        val retryDelayMillis: Long,
    ) : UnlockOutcome

    data class Locked(val untilMillis: Long) : UnlockOutcome

    data object NotConfigured : UnlockOutcome

    data object RecoveryCodeUnavailable : UnlockOutcome
}

sealed interface PinChangeOutcome {
    data object Changed : PinChangeOutcome

    data class ChangedWithNewRecoveryCode(val recoveryCode: String) : PinChangeOutcome

    data object WrongCurrentPin : PinChangeOutcome

    data object InvalidNewPin : PinChangeOutcome

    data object NotConfigured : PinChangeOutcome
}

class SecurityService(
    private val securityRepository: SecurityRepository,
    private val pinHasher: PinHasher,
    private val clock: Clock,
    private val policy: PinPolicy = PinPolicy(),
    private val recoveryCodeGenerator: RecoveryCodeGenerator = RecoveryCodeGenerator(),
) {

    fun observeState(): Flow<SecurityState> = securityRepository.observeState()

    suspend fun state(): SecurityState = securityRepository.getState()

    suspend fun requiresPin(action: CriticalAction): Boolean {
        val state = securityRepository.getState()
        if (!state.isPinEnabled) return false
        if (policy.alwaysPrompts(action)) return true
        val role = state.activeSessionRole(clock.nowMillis()) ?: return true
        return !role.covers(policy.requiredRole(action))
    }

    suspend fun isElevated(action: CriticalAction): Boolean = !requiresPin(action)

    suspend fun unlockWithPin(pin: String, action: CriticalAction? = null): UnlockOutcome {
        val now = clock.nowMillis()
        val state = securityRepository.getState()
        if (!state.isPinEnabled) return UnlockOutcome.NotConfigured
        if (policy.isLocked(now, state.lockoutUntilMillis)) {
            return UnlockOutcome.Locked(state.lockoutUntilMillis ?: now)
        }
        val storedHash = securityRepository.getCredentials().pinHash
            ?: return UnlockOutcome.NotConfigured

        if (pinHasher.verify(pin, storedHash)) {
            val until = policy.sessionExpiryMillis(now)
            securityRepository.updateSession(SecurityRole.OWNER, until)
            securityRepository.recordAttempt(
                failedAttempts = 0,
                lockoutUntilMillis = null,
                audit = audit(action ?: CriticalAction.CHANGE_PIN, SecurityAuditResult.GRANTED, now),
            )
            trimAudit()
            return UnlockOutcome.Granted(SecurityRole.OWNER, until)
        }

        return recordRejection(state.failedAttempts, now, SecurityAuditResult.DENIED, action)
    }

    suspend fun unlockWithRecoveryCode(
        code: String,
        action: CriticalAction? = null,
    ): UnlockOutcome {
        val now = clock.nowMillis()
        val state = securityRepository.getState()
        if (!state.isPinEnabled) return UnlockOutcome.NotConfigured
        if (policy.isLocked(now, state.lockoutUntilMillis)) {
            return UnlockOutcome.Locked(state.lockoutUntilMillis ?: now)
        }
        val storedHash = securityRepository.getCredentials().recoveryCodeHash
            ?: return UnlockOutcome.RecoveryCodeUnavailable

        if (!pinHasher.verify(RecoveryCode.normalize(code), storedHash)) {
            return recordRejection(state.failedAttempts, now, SecurityAuditResult.DENIED, action)
        }

        securityRepository.clearCredentials(
            audit = audit(
                action ?: CriticalAction.CHANGE_PIN,
                SecurityAuditResult.RECOVERY_USED,
                now,
            ),
        )
        trimAudit()
        return UnlockOutcome.Granted(SecurityRole.OWNER, sessionUntilMillis = null)
    }

    suspend fun enablePin(pin: String): AppResult<String> {
        if (!policy.isValidPin(pin)) return AppResult.Failure(invalidPinError())
        val now = clock.nowMillis()
        val recoveryCode = recoveryCodeGenerator.generate()
        securityRepository.saveCredentials(
            pinHash = pinHasher.hash(pin),
            recoveryCodeHash = pinHasher.hash(recoveryCode.compact),
            pinUpdatedAtMillis = now,
            audit = audit(CriticalAction.CHANGE_PIN, SecurityAuditResult.PIN_ENABLED, now),
        )
        securityRepository.updateSession(SecurityRole.OWNER, policy.sessionExpiryMillis(now))
        trimAudit()
        return AppResult.Success(recoveryCode.formatted)
    }

    suspend fun changePin(currentPin: String, newPin: String): PinChangeOutcome {
        if (!policy.isValidPin(newPin)) return PinChangeOutcome.InvalidNewPin
        val state = securityRepository.getState()
        if (!state.isPinEnabled) return PinChangeOutcome.NotConfigured
        val now = clock.nowMillis()
        val credentials = securityRepository.getCredentials()
        val storedHash = credentials.pinHash ?: return PinChangeOutcome.NotConfigured
        if (!pinHasher.verify(currentPin, storedHash)) return PinChangeOutcome.WrongCurrentPin

        val existingRecoveryHash = credentials.recoveryCodeHash
        if (existingRecoveryHash != null) {
            securityRepository.saveCredentials(
                pinHash = pinHasher.hash(newPin),
                recoveryCodeHash = existingRecoveryHash,
                pinUpdatedAtMillis = now,
                audit = audit(CriticalAction.CHANGE_PIN, SecurityAuditResult.PIN_CHANGED, now),
            )
            trimAudit()
            return PinChangeOutcome.Changed
        }

        val recoveryCode = recoveryCodeGenerator.generate()
        securityRepository.saveCredentials(
            pinHash = pinHasher.hash(newPin),
            recoveryCodeHash = pinHasher.hash(recoveryCode.compact),
            pinUpdatedAtMillis = now,
            audit = audit(CriticalAction.CHANGE_PIN, SecurityAuditResult.PIN_CHANGED, now),
        )
        trimAudit()
        return PinChangeOutcome.ChangedWithNewRecoveryCode(recoveryCode.formatted)
    }

    suspend fun disablePin(pin: String): AppResult<Unit> {
        val state = securityRepository.getState()
        if (!state.isPinEnabled) return AppResult.Success(Unit)
        val storedHash = securityRepository.getCredentials().pinHash
            ?: return AppResult.Success(Unit)
        if (!pinHasher.verify(pin, storedHash)) {
            return AppResult.Failure(AppError.Validation("PIN is incorrect"))
        }
        val now = clock.nowMillis()
        securityRepository.clearCredentials(
            audit = audit(CriticalAction.CHANGE_PIN, SecurityAuditResult.PIN_DISABLED, now),
        )
        trimAudit()
        return AppResult.Success(Unit)
    }

    suspend fun regenerateRecoveryCode(pin: String): AppResult<String> {
        val state = securityRepository.getState()
        if (!state.isPinEnabled) return AppResult.Failure(AppError.Validation("PIN is not enabled"))
        val credentials = securityRepository.getCredentials()
        val storedHash = credentials.pinHash
            ?: return AppResult.Failure(AppError.Validation("PIN is not configured"))
        if (!pinHasher.verify(pin, storedHash)) {
            return AppResult.Failure(AppError.Validation("PIN is incorrect"))
        }
        val now = clock.nowMillis()
        val recoveryCode = recoveryCodeGenerator.generate()
        securityRepository.saveCredentials(
            pinHash = storedHash,
            recoveryCodeHash = pinHasher.hash(recoveryCode.compact),
            pinUpdatedAtMillis = state.pinUpdatedAtMillis ?: now,
            audit = audit(
                CriticalAction.CHANGE_PIN,
                SecurityAuditResult.RECOVERY_CODE_REGENERATED,
                now,
            ),
        )
        trimAudit()
        return AppResult.Success(recoveryCode.formatted)
    }

    suspend fun lockSession() {
        securityRepository.updateSession(null, null)
    }

    suspend fun recordDecision(
        action: CriticalAction,
        granted: Boolean,
        actorLabel: String? = null,
        targetType: String? = null,
        targetId: String? = null,
    ) {
        securityRepository.appendAudit(
            SecurityAuditEntry(
                occurredAtMillis = clock.nowMillis(),
                actionId = action.id,
                result = if (granted) SecurityAuditResult.GRANTED else SecurityAuditResult.DENIED,
                actorLabel = actorLabel,
                targetType = targetType,
                targetId = targetId,
            ),
        )
        trimAudit()
    }

    suspend fun recentAudit(limit: Int = DEFAULT_AUDIT_VIEW_LIMIT) =
        securityRepository.recentAudit(limit)

    fun policy(): PinPolicy = policy

    private suspend fun recordRejection(
        failedAttempts: Int,
        nowMillis: Long,
        result: SecurityAuditResult,
        action: CriticalAction?,
    ): UnlockOutcome {
        val attemptCount = failedAttempts + 1
        val lockoutUntil = policy.lockoutUntilAfterFailure(nowMillis, attemptCount)
        securityRepository.recordAttempt(
            failedAttempts = attemptCount,
            lockoutUntilMillis = lockoutUntil,
            audit = audit(
                action ?: CriticalAction.CHANGE_PIN,
                if (lockoutUntil != null) SecurityAuditResult.LOCKED else result,
                nowMillis,
            ),
        )
        trimAudit()
        return if (lockoutUntil != null) {
            securityRepository.updateSession(null, null)
            UnlockOutcome.Locked(lockoutUntil)
        } else {
            UnlockOutcome.WrongSecret(
                failedAttempts = attemptCount,
                remainingAttempts = policy.remainingAttempts(attemptCount),
                retryDelayMillis = policy.retryDelayMillis(attemptCount),
            )
        }
    }

    private suspend fun trimAudit() {
        securityRepository.trimAudit(policy.auditRetentionEntries)
    }

    private fun audit(
        action: CriticalAction,
        result: SecurityAuditResult,
        nowMillis: Long,
    ): SecurityAuditEntry = SecurityAuditEntry(
        occurredAtMillis = nowMillis,
        actionId = action.id,
        result = result,
    )

    private fun invalidPinError(): AppError = AppError.Validation(
        "PIN must be ${policy.minPinLength} to ${policy.maxPinLength} digits",
    )

    companion object {
        const val DEFAULT_AUDIT_VIEW_LIMIT: Int = 50
    }
}
