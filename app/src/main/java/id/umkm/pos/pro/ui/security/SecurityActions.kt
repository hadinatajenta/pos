package id.umkm.pos.pro.ui.security

class SecurityActions(
    val onStartEnablePin: () -> Unit,
    val onStartChangePin: () -> Unit,
    val onStartDisablePin: () -> Unit,
    val onStartRegenerateCode: () -> Unit,
    val onCurrentPinChange: (String) -> Unit,
    val onNewPinChange: (String) -> Unit,
    val onConfirmPinChange: (String) -> Unit,
    val onSubmitEnablePin: () -> Unit,
    val onSubmitChangePin: () -> Unit,
    val onSubmitDisablePin: () -> Unit,
    val onSubmitRegenerateCode: () -> Unit,
    val onRecoveryCodeAcknowledgedChange: (Boolean) -> Unit,
    val onCloseStep: () -> Unit,
    val onLockSession: () -> Unit,
    val onRefreshAudit: () -> Unit,
    val onDismissFeedback: () -> Unit,
) {
    companion object {
        fun from(viewModel: SecurityViewModel): SecurityActions = SecurityActions(
            onStartEnablePin = viewModel::onStartEnablePin,
            onStartChangePin = viewModel::onStartChangePin,
            onStartDisablePin = viewModel::onStartDisablePin,
            onStartRegenerateCode = viewModel::onStartRegenerateCode,
            onCurrentPinChange = viewModel::onCurrentPinChange,
            onNewPinChange = viewModel::onNewPinChange,
            onConfirmPinChange = viewModel::onConfirmPinChange,
            onSubmitEnablePin = viewModel::onSubmitEnablePin,
            onSubmitChangePin = viewModel::onSubmitChangePin,
            onSubmitDisablePin = viewModel::onSubmitDisablePin,
            onSubmitRegenerateCode = viewModel::onSubmitRegenerateCode,
            onRecoveryCodeAcknowledgedChange = viewModel::onRecoveryCodeAcknowledgedChange,
            onCloseStep = viewModel::onCloseStep,
            onLockSession = viewModel::onLockSession,
            onRefreshAudit = viewModel::onRefreshAudit,
            onDismissFeedback = viewModel::onDismissFeedback,
        )
    }
}
