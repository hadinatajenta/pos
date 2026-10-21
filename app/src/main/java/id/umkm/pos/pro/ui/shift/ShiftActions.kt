package id.umkm.pos.pro.ui.shift

class ShiftActions(
    val onCashierNameChange: (String) -> Unit,
    val onStartingCashChange: (String) -> Unit,
    val onActualEndingCashChange: (String) -> Unit,
    val onNextCashierNameChange: (String) -> Unit,
    val onNextStartingCashChange: (String) -> Unit,
    val onVarianceAcknowledgedChange: (Boolean) -> Unit,
    val onOpenShift: () -> Unit,
    val onCloseShift: () -> Unit,
    val onSwitchShift: () -> Unit,
    val onDismissForm: () -> Unit,
    val onDismissHistory: () -> Unit,
    val onRetryHistory: () -> Unit,
    val onMessageConsumed: () -> Unit,
) {
    companion object {
        fun from(viewModel: ShiftViewModel): ShiftActions = ShiftActions(
            onCashierNameChange = viewModel::onCashierNameChange,
            onStartingCashChange = viewModel::onStartingCashChange,
            onActualEndingCashChange = viewModel::onActualEndingCashChange,
            onNextCashierNameChange = viewModel::onNextCashierNameChange,
            onNextStartingCashChange = viewModel::onNextStartingCashChange,
            onVarianceAcknowledgedChange = viewModel::onVarianceAcknowledgedChange,
            onOpenShift = viewModel::onOpenShift,
            onCloseShift = viewModel::onCloseShift,
            onSwitchShift = viewModel::onSwitchShift,
            onDismissForm = viewModel::onDismissForm,
            onDismissHistory = viewModel::onDismissHistory,
            onRetryHistory = viewModel::onRetryHistory,
            onMessageConsumed = viewModel::onMessageConsumed,
        )
    }
}
