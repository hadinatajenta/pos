package id.umkm.pos.pro.ui.settings

import android.graphics.Bitmap
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.ui.common.UiText

data class SettingsUiState(
    val settings: AppSettings? = null,
    val isImportingQris: Boolean = false,
    val message: UiText? = null,
    val errorMessage: UiText? = null,
    val availablePrinters: List<PrinterInfo> = emptyList(),
    val printerConnection: PrinterConnection = PrinterConnection.DISCONNECTED,
    val isPrintingTest: Boolean = false,
    val receiptPreviewBitmap: Bitmap? = null,
    val isPreviewDialogOpen: Boolean = false,
) {
    val isQrisConfigured: Boolean get() = settings?.qrisPayload != null
}

