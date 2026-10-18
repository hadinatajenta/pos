package id.umkm.pos.pro.ui.settings

import id.umkm.pos.pro.core.contract.PrinterInfo

class SettingsActions(
    val onQrisImagePicked: (String) -> Unit,
    val onRemoveQris: () -> Unit,
    val onMessageConsumed: () -> Unit,
    val onSaveStoreSettings: (storeName: String, cashierName: String, taxRatePercent: Int, roundingIncrement: Long) -> Unit = { _, _, _, _ -> },
    val onSelectLanguage: (String) -> Unit = {},
    val onSelectPrinter: (PrinterInfo) -> Unit = {},
    val onSetPaperWidth: (Int) -> Unit = {},
    val onConnectPrinter: () -> Unit = {},
    val onDisconnectPrinter: () -> Unit = {},
    val onTestPrint: () -> Unit = {},
    val onOpenReceiptPreview: () -> Unit = {},
    val onDismissReceiptPreview: () -> Unit = {},
    val onRefreshPrinters: () -> Unit = {},
    val onExportBackup: () -> Unit = {},
    val onImportBackup: (String) -> Unit = {},
    val onDismissBackupState: () -> Unit = {},
) {
    companion object {
        fun from(viewModel: SettingsViewModel): SettingsActions = SettingsActions(
            onQrisImagePicked = viewModel::onQrisImagePicked,
            onRemoveQris = viewModel::onRemoveQris,
            onMessageConsumed = viewModel::onRemoveMessages,
            onSaveStoreSettings = viewModel::onSaveStoreSettings,
            onSelectLanguage = viewModel::onSelectLanguage,
            onSelectPrinter = viewModel::onSelectPrinter,
            onSetPaperWidth = viewModel::onSetPaperWidth,
            onConnectPrinter = viewModel::onConnectPrinter,
            onDisconnectPrinter = viewModel::onDisconnectPrinter,
            onTestPrint = viewModel::onTestPrint,
            onOpenReceiptPreview = viewModel::onOpenReceiptPreview,
            onDismissReceiptPreview = viewModel::onDismissReceiptPreview,
            onRefreshPrinters = viewModel::refreshPrinters,
            onExportBackup = viewModel::onExportBackup,
            onImportBackup = viewModel::onImportBackup,
            onDismissBackupState = viewModel::onDismissBackupState,
        )
    }
}
