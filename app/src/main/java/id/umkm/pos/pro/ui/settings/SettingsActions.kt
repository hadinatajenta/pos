package id.umkm.pos.pro.ui.settings

class SettingsActions(
    val onQrisImagePicked: (String) -> Unit,
    val onRemoveQris: () -> Unit,
    val onMessageConsumed: () -> Unit,
    val onSaveStoreSettings: (storeName: String, cashierName: String, taxRatePercent: Int, roundingIncrement: Long) -> Unit = { _, _, _, _ -> },
) {
    companion object {
        fun from(viewModel: SettingsViewModel): SettingsActions = SettingsActions(
            onQrisImagePicked = viewModel::onQrisImagePicked,
            onRemoveQris = viewModel::onRemoveQris,
            onMessageConsumed = viewModel::onRemoveMessages,
            onSaveStoreSettings = viewModel::onSaveStoreSettings,
        )
    }
}
