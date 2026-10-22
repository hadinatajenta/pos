package id.umkm.pos.pro.ui.staff

import id.umkm.pos.pro.core.model.Staff

class StaffActions(
    val onOpenAddEditor: () -> Unit,
    val onOpenRenameEditor: (Staff) -> Unit,
    val onNameChange: (String) -> Unit,
    val onSaveEditor: () -> Unit,
    val onDismissEditor: () -> Unit,
    val onRequestDeactivate: (Staff) -> Unit,
    val onConfirmDeactivate: () -> Unit,
    val onDismissDeactivate: () -> Unit,
    val onActivateStaff: (Staff) -> Unit,
    val onMessageConsumed: () -> Unit,
) {
    companion object {
        fun from(viewModel: StaffViewModel): StaffActions = StaffActions(
            onOpenAddEditor = viewModel::onOpenAddEditor,
            onOpenRenameEditor = viewModel::onOpenRenameEditor,
            onNameChange = viewModel::onNameChange,
            onSaveEditor = viewModel::onSaveEditor,
            onDismissEditor = viewModel::onDismissEditor,
            onRequestDeactivate = viewModel::onRequestDeactivate,
            onConfirmDeactivate = viewModel::onConfirmDeactivate,
            onDismissDeactivate = viewModel::onDismissDeactivate,
            onActivateStaff = viewModel::onActivateStaff,
            onMessageConsumed = viewModel::onMessageConsumed,
        )
    }
}
