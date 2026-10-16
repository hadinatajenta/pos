package id.umkm.pos.pro.hardware.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.hardware.receipt.ReceiptPaperWidth
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BluetoothReceiptPrinter(
    context: Context,
    renderer: ReceiptRenderer,
    paperWidth: ReceiptPaperWidth = ReceiptPaperWidth.MILLIMETER_58,
    printMode: PrintMode = PrintMode.BITMAP,
    lifecycleScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : EscPosReceiptPrinter(renderer, paperWidth, printMode, Dispatchers.IO, lifecycleScope) {

    private val applicationContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var monitorJob: Job? = null

    override suspend fun openTransport(printer: PrinterInfo): AppResult<Unit> {
        val bluetoothAdapter = adapter
            ?: return AppResult.Failure(AppError.Hardware("Bluetooth is not available on this device"))
        if (!bluetoothAdapter.isEnabled) {
            return AppResult.Failure(AppError.Hardware("Bluetooth is turned off. Turn it on, then reconnect the printer."))
        }
        val targetDevice = try {
            bluetoothAdapter.getRemoteDevice(printer.address)
        } catch (exception: IllegalArgumentException) {
            return AppResult.Failure(AppError.Hardware("The saved printer address is not valid. Choose the printer again."))
        } catch (exception: SecurityException) {
            return AppResult.Failure(AppError.Hardware("Bluetooth permission is required. Allow Bluetooth access and try again."))
        }
        try {
            if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        } catch (exception: SecurityException) {
            return AppResult.Failure(AppError.Hardware("Bluetooth permission is required. Allow Bluetooth access and try again."))
        }
        val newSocket = try {
            targetDevice.createRfcommSocketToServiceRecord(SPP_SERVICE_UUID)
        } catch (exception: IOException) {
            return AppResult.Failure(AppError.Hardware("Could not open a Bluetooth channel to ${printer.name}. Choose the printer again."))
        } catch (exception: SecurityException) {
            return AppResult.Failure(AppError.Hardware("Bluetooth permission is required. Allow Bluetooth access and try again."))
        }
        try {
            newSocket.connect()
        } catch (exception: IOException) {
            runCatching { newSocket.close() }
            return AppResult.Failure(AppError.Hardware("Could not connect to ${printer.name}. Make sure the printer is on and in range."))
        } catch (exception: SecurityException) {
            runCatching { newSocket.close() }
            return AppResult.Failure(AppError.Hardware("Bluetooth permission is required. Allow Bluetooth access and try again."))
        }
        socket = newSocket
        startDisconnectMonitor(newSocket)
        return AppResult.Success(Unit)
    }

    override suspend fun writeTransport(bytes: ByteArray): AppResult<Unit> {
        val activeSocket = socket
            ?: return AppResult.Failure(AppError.Hardware("Printer is not connected"))
        return try {
            val output = activeSocket.outputStream
            output.write(bytes)
            output.flush()
            AppResult.Success(Unit)
        } catch (exception: IOException) {
            AppResult.Failure(AppError.Hardware("Lost connection to the printer. Reconnect and try again."))
        } catch (exception: SecurityException) {
            AppResult.Failure(AppError.Hardware("Bluetooth permission is required to print."))
        }
    }

    override fun closeTransport() {
        monitorJob?.cancel()
        monitorJob = null
        val activeSocket = socket
        socket = null
        if (activeSocket != null) runCatching { activeSocket.close() }
    }

    private fun startDisconnectMonitor(activeSocket: BluetoothSocket) {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            try {
                val input = activeSocket.inputStream
                val buffer = ByteArray(INPUT_BUFFER_BYTES)
                var bytesRead = input.read(buffer)
                while (isActive && bytesRead >= 0) {
                    bytesRead = input.read(buffer)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: IOException) {
                handleTransportLoss(activeSocket)
                return@launch
            }
            handleTransportLoss(activeSocket)
        }
    }

    private fun handleTransportLoss(activeSocket: BluetoothSocket) {
        if (socket !== activeSocket) return
        socket = null
        runCatching { activeSocket.close() }
        markTransportDisconnected()
    }

    private companion object {
        const val INPUT_BUFFER_BYTES = 256
        val SPP_SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
