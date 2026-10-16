package id.umkm.pos.pro.core.contract

import kotlinx.coroutines.flow.Flow

enum class PrinterConnection {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

enum class PrinterTransport {
    BLUETOOTH_RFCOMM,
    USB_OTG,
}

data class PrinterInfo(
    val name: String,
    val address: String,
    val transport: PrinterTransport,
)

enum class ReceiptAlignment {
    START,
    CENTER,
    END,
}

data class ReceiptLine(
    val text: String,
    val alignment: ReceiptAlignment = ReceiptAlignment.START,
    val isBold: Boolean = false,
)

data class ReceiptDocument(
    val storeName: String,
    val lines: List<ReceiptLine>,
    val qrisImagePath: String? = null,
)

enum class ScanSource {
    CAMERA,
    HARDWARE_HID,
}

data class ScanEvent(
    val code: String,
    val source: ScanSource,
    val timestampMillis: Long,
)

interface ReceiptPrinter {
    val connectionState: Flow<PrinterConnection>
    suspend fun connect(printer: PrinterInfo): AppResult<Unit>
    suspend fun disconnect()
    suspend fun print(receipt: ReceiptDocument): AppResult<Unit>
}

interface ReceiptRenderer {
    fun renderText(receipt: ReceiptDocument, widthChars: Int): String
    fun renderBitmap(receipt: ReceiptDocument, widthPx: Int): ByteArray
    fun renderPng(receipt: ReceiptDocument, widthPx: Int): ByteArray
}

interface ScannerInput {
    val scanEvents: Flow<ScanEvent>
}
