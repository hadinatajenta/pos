package id.umkm.pos.pro.hardware.printer

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.hardware.receipt.EscPosCommandBuilder
import id.umkm.pos.pro.hardware.receipt.ReceiptLayout
import id.umkm.pos.pro.hardware.receipt.ReceiptPaperWidth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

abstract class EscPosReceiptPrinter(
    private val renderer: ReceiptRenderer,
    paperWidth: ReceiptPaperWidth,
    private val printMode: PrintMode,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ownerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : ReceiptPrinter, AutoCloseable {

    var paperWidth: ReceiptPaperWidth = paperWidth
        protected set

    override fun setPaperWidth(paperWidthMm: Int) {
        this.paperWidth = ReceiptPaperWidth.fromMm(paperWidthMm)
    }

    private val connectionStateFlow = MutableStateFlow(PrinterConnection.DISCONNECTED)

    override val connectionState: Flow<PrinterConnection> = connectionStateFlow.asStateFlow()


    protected val scope: CoroutineScope get() = ownerScope

    protected abstract suspend fun openTransport(printer: PrinterInfo): AppResult<Unit>

    protected abstract suspend fun writeTransport(bytes: ByteArray): AppResult<Unit>

    protected abstract fun closeTransport()

    override suspend fun connect(printer: PrinterInfo): AppResult<Unit> = withContext(ioDispatcher) {
        if (connectionStateFlow.value == PrinterConnection.CONNECTED) return@withContext AppResult.Success(Unit)
        connectionStateFlow.value = PrinterConnection.CONNECTING
        val outcome = guard { openTransport(printer) }
        if (outcome is AppResult.Failure) {
            safeClose()
            connectionStateFlow.value = PrinterConnection.DISCONNECTED
        } else {
            connectionStateFlow.value = PrinterConnection.CONNECTED
        }
        outcome
    }

    override suspend fun disconnect() = withContext(ioDispatcher) {
        safeClose()
        connectionStateFlow.value = PrinterConnection.DISCONNECTED
    }

    override suspend fun print(receipt: ReceiptDocument): AppResult<Unit> = withContext(ioDispatcher) {
        if (connectionStateFlow.value != PrinterConnection.CONNECTED) {
            return@withContext AppResult.Failure(AppError.Hardware("Printer is not connected"))
        }
        val payload = buildPayload(receipt)
        val outcome = guard { writeTransport(payload) }
        if (outcome is AppResult.Failure) {
            safeClose()
            connectionStateFlow.value = PrinterConnection.DISCONNECTED
        }
        outcome
    }

    protected fun markTransportDisconnected() {
        connectionStateFlow.value = PrinterConnection.DISCONNECTED
    }

    override fun close() {
        safeClose()
        ownerScope.cancel()
    }

    private fun buildPayload(receipt: ReceiptDocument): ByteArray = when (printMode) {
        PrintMode.TEXT -> EscPosCommandBuilder.buildDocument(ReceiptLayout.layoutLines(receipt, paperWidth.characters))
        PrintMode.BITMAP -> EscPosCommandBuilder.buildRasterDocument(renderer.renderBitmap(receipt, paperWidth.pixels))
    }

    private suspend fun <T> guard(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(AppError.Hardware(throwable.message ?: "Printer communication failed"))
        }

    private fun safeClose() {
        try {
            closeTransport()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            connectionStateFlow.value = PrinterConnection.DISCONNECTED
        }
    }
}
