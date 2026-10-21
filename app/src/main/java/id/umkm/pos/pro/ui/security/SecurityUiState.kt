package id.umkm.pos.pro.ui.security

import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.ui.common.UiText

enum class SecurityStep {
    OVERVIEW,
    ENABLE_PIN,
    CHANGE_PIN,
    DISABLE_PIN,
    REGENERATE_CODE,
    RECOVERY_CODE,
}

data class SecurityUiState(
    val securityState: SecurityState = SecurityState(),
    val step: SecurityStep = SecurityStep.OVERVIEW,
    val isSessionElevated: Boolean = false,
    val currentPinInput: String = "",
    val newPinInput: String = "",
    val confirmPinInput: String = "",
    val currentPinError: UiText? = null,
    val newPinError: UiText? = null,
    val confirmPinError: UiText? = null,
    val recoveryCode: String? = null,
    val isRecoveryCodeAcknowledged: Boolean = false,
    val isRecoveryCodeLeaveConfirmed: Boolean = false,
    val isBusy: Boolean = false,
    val errorMessage: UiText? = null,
    val warningMessage: UiText? = null,
    val message: UiText? = null,
    val auditEntries: List<SecurityAuditEntry> = emptyList(),
    val minPinLength: Int = 4,
    val maxPinLength: Int = 8,
) {
    val isPinEnabled: Boolean get() = securityState.isPinEnabled

    val pinUpdatedAtMillis: Long? get() = securityState.pinUpdatedAtMillis

    val isRecoveryCodeConfigured: Boolean get() = securityState.recoveryCodeConfigured
}
