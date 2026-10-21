package id.umkm.pos.pro.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.domain.security.PinChangeOutcome
import id.umkm.pos.pro.domain.security.SecurityService
import id.umkm.pos.pro.ui.common.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SecurityViewModel(
    private val securityService: SecurityService,
    private val clock: Clock,
) : ViewModel() {

    private val policy = securityService.policy()

    private val _uiState = MutableStateFlow(
        SecurityUiState(
            minPinLength = policy.minPinLength,
            maxPinLength = policy.maxPinLength,
        ),
    )
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private var sessionExpiryJob: Job? = null

    init {
        viewModelScope.launch {
            securityService.observeState().collect { state -> applySecurityState(state) }
        }
        refreshState()
        refreshAudit()
    }

    fun onStartEnablePin() = openStep(SecurityStep.ENABLE_PIN)

    fun onStartChangePin() = openStep(SecurityStep.CHANGE_PIN)

    fun onStartDisablePin() = openStep(SecurityStep.DISABLE_PIN)

    fun onStartRegenerateCode() = openStep(SecurityStep.REGENERATE_CODE)

    fun onCurrentPinChange(value: String) {
        _uiState.update {
            it.copy(
                currentPinInput = sanitizePin(value),
                currentPinError = null,
                errorMessage = null,
            )
        }
    }

    fun onNewPinChange(value: String) {
        _uiState.update {
            it.copy(
                newPinInput = sanitizePin(value),
                newPinError = null,
                confirmPinError = null,
            )
        }
    }

    fun onConfirmPinChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPinInput = sanitizePin(value),
                confirmPinError = null,
            )
        }
    }

    fun onRecoveryCodeAcknowledgedChange(isAcknowledged: Boolean) {
        _uiState.update {
            it.copy(
                isRecoveryCodeAcknowledged = isAcknowledged,
                warningMessage = if (isAcknowledged) null else it.warningMessage,
            )
        }
    }

    fun onCloseStep() {
        val state = _uiState.value
        when (state.step) {
            SecurityStep.OVERVIEW -> Unit

            SecurityStep.RECOVERY_CODE -> leaveRecoveryCode(state)

            else -> _uiState.update { it.copy(step = SecurityStep.OVERVIEW).cleared() }
        }
    }

    fun onSubmitEnablePin() {
        val state = _uiState.value
        val validation = validateNewPin(state.newPinInput, state.confirmPinInput)
        if (validation != null) {
            _uiState.update { it.copy(newPinError = validation.first, confirmPinError = validation.second) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, warningMessage = null, message = null) }
            when (val result = securityService.enablePin(state.newPinInput)) {
                is AppResult.Success -> {
                    refreshState()
                    refreshAudit()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            step = SecurityStep.RECOVERY_CODE,
                            recoveryCode = result.value,
                            isRecoveryCodeAcknowledged = false,
                            isRecoveryCodeLeaveConfirmed = false,
                        ).cleared().copy(message = UiText.from { strings -> strings.securityPinEnabledMessage })
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        newPinError = UiText.from { strings ->
                            strings.securityPinLengthError(policy.minPinLength, policy.maxPinLength)
                        },
                    )
                }
            }
        }
    }

    fun onSubmitChangePin() {
        val state = _uiState.value
        if (state.currentPinInput.isEmpty()) {
            _uiState.update { it.copy(currentPinError = currentPinRequiredText()) }
            return
        }
        val validation = validateNewPin(state.newPinInput, state.confirmPinInput)
        if (validation != null) {
            _uiState.update { it.copy(newPinError = validation.first, confirmPinError = validation.second) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, warningMessage = null, message = null) }
            when (val outcome = securityService.changePin(state.currentPinInput, state.newPinInput)) {
                PinChangeOutcome.Changed -> {
                    refreshState()
                    refreshAudit()
                    _uiState.update {
                        it.copy(step = SecurityStep.OVERVIEW, isBusy = false)
                            .cleared()
                            .copy(message = UiText.from { strings -> strings.securityPinChangedMessage })
                    }
                }

                is PinChangeOutcome.ChangedWithNewRecoveryCode -> {
                    refreshState()
                    refreshAudit()
                    _uiState.update {
                        it.copy(
                            step = SecurityStep.RECOVERY_CODE,
                            isBusy = false,
                            recoveryCode = outcome.recoveryCode,
                            isRecoveryCodeAcknowledged = false,
                            isRecoveryCodeLeaveConfirmed = false,
                        ).cleared().copy(message = UiText.from { strings -> strings.securityPinChangedMessage })
                    }
                }

                PinChangeOutcome.WrongCurrentPin -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        currentPinError = UiText.from { strings -> strings.securityWrongCurrentPin },
                    )
                }

                PinChangeOutcome.InvalidNewPin -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        newPinError = UiText.from { strings ->
                            strings.securityPinLengthError(policy.minPinLength, policy.maxPinLength)
                        },
                    )
                }

                PinChangeOutcome.NotConfigured -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        step = SecurityStep.OVERVIEW,
                        errorMessage = UiText.from { strings -> strings.securityPinNotConfigured },
                    ).cleared()
                }
            }
        }
    }

    fun onSubmitDisablePin() {
        val state = _uiState.value
        if (state.currentPinInput.isEmpty()) {
            _uiState.update { it.copy(currentPinError = currentPinRequiredText()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, warningMessage = null, message = null) }
            when (val result = securityService.disablePin(state.currentPinInput)) {
                is AppResult.Success -> {
                    refreshState()
                    refreshAudit()
                    _uiState.update {
                        it.copy(step = SecurityStep.OVERVIEW, isBusy = false)
                            .cleared()
                            .copy(message = UiText.from { strings -> strings.securityPinDisabledMessage })
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        currentPinError = UiText.from { strings -> strings.securityWrongCurrentPin },
                    )
                }
            }
        }
    }

    fun onSubmitRegenerateCode() {
        val state = _uiState.value
        if (state.currentPinInput.isEmpty()) {
            _uiState.update { it.copy(currentPinError = currentPinRequiredText()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, warningMessage = null, message = null) }
            when (val result = securityService.regenerateRecoveryCode(state.currentPinInput)) {
                is AppResult.Success -> {
                    refreshAudit()
                    _uiState.update {
                        it.copy(
                            step = SecurityStep.RECOVERY_CODE,
                            isBusy = false,
                            recoveryCode = result.value,
                            isRecoveryCodeAcknowledged = false,
                            isRecoveryCodeLeaveConfirmed = false,
                        ).cleared().copy(message = UiText.from { strings -> strings.securityRegeneratedMessage })
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        currentPinError = UiText.from { strings -> strings.securityWrongCurrentPin },
                    )
                }
            }
        }
    }

    fun onLockSession() {
        viewModelScope.launch {
            securityService.lockSession()
            refreshState()
            _uiState.update {
                it.copy(message = UiText.from { strings -> strings.securitySessionLocked })
            }
        }
    }

    fun onRefreshAudit() {
        viewModelScope.launch { refreshAudit() }
    }

    fun onDismissFeedback() {
        _uiState.update { it.copy(errorMessage = null, warningMessage = null, message = null) }
    }

    private fun leaveRecoveryCode(state: SecurityUiState) {
        if (!state.isRecoveryCodeAcknowledged && !state.isRecoveryCodeLeaveConfirmed) {
            _uiState.update {
                it.copy(
                    isRecoveryCodeLeaveConfirmed = true,
                    warningMessage = UiText.from { strings -> strings.securityRecoveryCodeUnsavedWarning },
                )
            }
            return
        }
        val warning = if (state.isRecoveryCodeAcknowledged) {
            null
        } else {
            UiText.from { strings -> strings.securityRecoveryCodeLostWarning }
        }
        _uiState.update {
            it.copy(
                step = SecurityStep.OVERVIEW,
                warningMessage = warning,
            ).cleared()
        }
    }

    private fun openStep(step: SecurityStep) {
        _uiState.update {
            it.copy(
                step = step,
                errorMessage = null,
                warningMessage = null,
                message = null,
                isBusy = false,
            ).cleared()
        }
    }

    private fun validateNewPin(newPin: String, confirmation: String): Pair<UiText?, UiText?>? {
        if (!policy.isValidPin(newPin)) {
            return UiText.from { strings ->
                strings.securityPinLengthError(policy.minPinLength, policy.maxPinLength)
            } to null
        }
        if (newPin != confirmation) {
            return null to UiText.from { strings -> strings.securityPinMismatch }
        }
        return null
    }

    private fun currentPinRequiredText(): UiText =
        UiText.from { strings -> strings.securityPinRequired }

    private fun sanitizePin(value: String): String =
        value.filter(Char::isDigit).take(policy.maxPinLength)

    private fun applySecurityState(state: SecurityState) {
        val now = clock.nowMillis()
        _uiState.update {
            it.copy(
                securityState = state,
                isSessionElevated = state.isPinEnabled && state.activeSessionRole(now) != null,
            )
        }
        scheduleSessionExpiry(state, now)
    }

    private fun scheduleSessionExpiry(state: SecurityState, nowMillis: Long) {
        sessionExpiryJob?.cancel()
        val unlockedUntil = state.sessionUnlockedUntilMillis ?: return
        val remainingMillis = unlockedUntil - nowMillis
        if (remainingMillis <= 0L) return
        sessionExpiryJob = viewModelScope.launch {
            delay(remainingMillis)
            _uiState.update { it.copy(isSessionElevated = false) }
        }
    }

    private fun refreshState() {
        viewModelScope.launch { applySecurityState(securityService.state()) }
    }

    private fun refreshAudit() {
        viewModelScope.launch {
            val entries = securityService.recentAudit(AUDIT_LIMIT)
            _uiState.update { it.copy(auditEntries = entries) }
        }
    }

    private fun SecurityUiState.cleared(): SecurityUiState = copy(
        currentPinInput = "",
        newPinInput = "",
        confirmPinInput = "",
        currentPinError = null,
        newPinError = null,
        confirmPinError = null,
        recoveryCode = if (step == SecurityStep.RECOVERY_CODE) recoveryCode else null,
        isRecoveryCodeAcknowledged = if (step == SecurityStep.RECOVERY_CODE) {
            isRecoveryCodeAcknowledged
        } else {
            false
        },
        isRecoveryCodeLeaveConfirmed = if (step == SecurityStep.RECOVERY_CODE) {
            isRecoveryCodeLeaveConfirmed
        } else {
            false
        },
    )

    companion object {
        const val AUDIT_LIMIT: Int = 50
    }
}
