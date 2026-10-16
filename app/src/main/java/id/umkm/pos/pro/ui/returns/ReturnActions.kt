package id.umkm.pos.pro.ui.returns

class ReturnActions(
    val onReturnQtyChange: (Long, String) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onRetry: () -> Unit,
    val onSubmit: () -> Unit,
) {
    companion object {
        fun from(viewModel: ReturnViewModel): ReturnActions = ReturnActions(
            onReturnQtyChange = viewModel::onReturnQtyChange,
            onNoteChange = viewModel::onNoteChange,
            onRetry = viewModel::onRetry,
            onSubmit = viewModel::onSubmit,
        )
    }
}
