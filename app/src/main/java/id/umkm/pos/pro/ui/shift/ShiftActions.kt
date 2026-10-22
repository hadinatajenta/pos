package id.umkm.pos.pro.ui.shift

class ShiftActions(
    val onSelectStaff: (Long) -> Unit,
    val onStartingCashChange: (String) -> Unit,
    val onActualEndingCashChange: (String) -> Unit,
    val onSelectNextStaff: (Long) -> Unit,
    val onNextStartingCashChange: (String) -> Unit,
    val onVarianceAcknowledgedChange: (Boolean) -> Unit,
    val onOpenShift: () -> Unit,
    val onCloseShift: () -> Unit,
    val onSwitchShift: () -> Unit,
    val onSwitchOperator: (Long) -> Unit,
    val onDismissForm: () -> Unit,
    val onDismissHistory: () -> Unit,
    val onRetryHistory: () -> Unit,
    val onMessageConsumed: () -> Unit,
) {
    companion object {
        fun from(viewModel: ShiftViewModel): ShiftActions = ShiftActions(
            onSelectStaff = viewModel::onSelectStaff,
            onStartingCashChange = viewModel::onStartingCashChange,
            onActualEndingCashChange = viewModel::onActualEndingCashChange,
            onSelectNextStaff = viewModel::onSelectNextStaff,
            onNextStartingCashChange = viewModel::onNextStartingCashChange,
            onVarianceAcknowledgedChange = viewModel::onVarianceAcknowledgedChange,
            onOpenShift = viewModel::onOpenShift,
            onCloseShift = viewModel::onCloseShift,
            onSwitchShift = viewModel::onSwitchShift,
            onSwitchOperator = viewModel::onSwitchOperator,
            onDismissForm = viewModel::onDismissForm,
            onDismissHistory = viewModel::onDismissHistory,
            onRetryHistory = viewModel::onRetryHistory,
            onMessageConsumed = viewModel::onMessageConsumed,
        )
    }
}
