package id.umkm.pos.pro.hardware.printer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.hardware.receipt.ReceiptPaperWidth
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class UsbOtgReceiptPrinter(
    context: Context,
    renderer: ReceiptRenderer,
    paperWidth: ReceiptPaperWidth = ReceiptPaperWidth.MILLIMETER_58,
    printMode: PrintMode = PrintMode.BITMAP,
    lifecycleScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : EscPosReceiptPrinter(renderer, paperWidth, printMode, Dispatchers.IO, lifecycleScope) {

    private val applicationContext = context.applicationContext
    private val usbManager: UsbManager? =
        applicationContext.getSystemService(Context.USB_SERVICE) as? UsbManager

    @Volatile
    private var deviceConnection: UsbDeviceConnection? = null

    @Volatile
    private var bulkOutEndpoint: UsbEndpoint? = null

    @Volatile
    private var deviceInterface: UsbInterface? = null

    @Volatile
    private var attachedDevice: UsbDevice? = null

    @Volatile
    private var detachReceiver: BroadcastReceiver? = null

    override suspend fun openTransport(printer: PrinterInfo): AppResult<Unit> {
        val manager = usbManager
            ?: return AppResult.Failure(AppError.Hardware("USB is not available on this device"))
        val device = findDevice(printer.address)
            ?: return AppResult.Failure(AppError.Hardware("USB printer was not found. Connect the printer and try again."))
        if (!manager.hasPermission(device)) {
            val isGranted = requestUsbPermission(device)
            if (!isGranted) {
                return AppResult.Failure(AppError.Hardware("USB access was denied. Allow USB access and try again."))
            }
        }
        val target = findBulkOutEndpoint(device)
            ?: return AppResult.Failure(AppError.Hardware("This USB device does not support printing."))
        val connection = manager.openDevice(device)
            ?: return AppResult.Failure(AppError.Hardware("Could not open the USB printer."))
        if (!connection.claimInterface(target.usbInterface, true)) {
            runCatching { connection.close() }
            return AppResult.Failure(AppError.Hardware("Could not claim the USB printer interface."))
        }
        deviceConnection = connection
        deviceInterface = target.usbInterface
        bulkOutEndpoint = target.endpoint
        attachedDevice = device
        registerDetachReceiver()
        return AppResult.Success(Unit)
    }

    override suspend fun writeTransport(bytes: ByteArray): AppResult<Unit> {
        val connection = deviceConnection
            ?: return AppResult.Failure(AppError.Hardware("Printer is not connected"))
        val endpoint = bulkOutEndpoint
            ?: return AppResult.Failure(AppError.Hardware("Printer is not connected"))
        var offset = 0
        while (offset < bytes.size) {
            val chunkSize = minOf(USB_CHUNK_BYTES, bytes.size - offset)
            val transferred = try {
                connection.bulkTransfer(endpoint, bytes, offset, chunkSize, USB_TIMEOUT_MILLIS)
            } catch (exception: IllegalArgumentException) {
                return AppResult.Failure(AppError.Hardware("USB printer stopped responding. Reconnect and try again."))
            }
            if (transferred < 0) {
                return AppResult.Failure(AppError.Hardware("USB printer stopped responding. Reconnect and try again."))
            }
            offset += transferred
        }
        return AppResult.Success(Unit)
    }

    override fun closeTransport() {
        unregisterDetachReceiver()
        val connection = deviceConnection
        val usbInterface = deviceInterface
        deviceConnection = null
        deviceInterface = null
        bulkOutEndpoint = null
        attachedDevice = null
        if (connection != null) {
            if (usbInterface != null) runCatching { connection.releaseInterface(usbInterface) }
            runCatching { connection.close() }
        }
    }

    private fun findDevice(address: String): UsbDevice? {
        val devices = usbManager?.deviceList?.values ?: return null
        devices.firstOrNull { it.deviceName == address }?.let { return it }
        val identifierParts = address.split(':')
        if (identifierParts.size == 2) {
            val vendorId = identifierParts[0].removePrefix("0x").toIntOrNull(radix = 16)
            val productId = identifierParts[1].removePrefix("0x").toIntOrNull(radix = 16)
            if (vendorId != null && productId != null) {
                return devices.firstOrNull { it.vendorId == vendorId && it.productId == productId }
            }
        }
        return null
    }

    private fun findBulkOutEndpoint(device: UsbDevice): UsbBulkEndpoint? {
        for (interfaceIndex in 0 until device.interfaceCount) {
            val candidateInterface = device.getInterface(interfaceIndex)
            for (endpointIndex in 0 until candidateInterface.endpointCount) {
                val candidateEndpoint = candidateInterface.getEndpoint(endpointIndex)
                val isBulk = candidateEndpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                val isOut = candidateEndpoint.direction == UsbConstants.USB_DIR_OUT
                if (isBulk && isOut) {
                    return UsbBulkEndpoint(usbInterface = candidateInterface, endpoint = candidateEndpoint)
                }
            }
        }
        return null
    }

    private suspend fun requestUsbPermission(device: UsbDevice): Boolean = suspendCancellableCoroutine { continuation ->
        val permissionAction = "${applicationContext.packageName}.USB_PERMISSION"
        val permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != permissionAction) return
                unregisterReceiverSafely(this)
                val isGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (continuation.isActive) continuation.resume(isGranted)
            }
        }
        ContextCompat.registerReceiver(
            applicationContext,
            permissionReceiver,
            IntentFilter(permissionAction),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        continuation.invokeOnCancellation { unregisterReceiverSafely(permissionReceiver) }
        val permissionIntent = PendingIntent.getBroadcast(
            applicationContext,
            USB_PERMISSION_REQUEST_CODE,
            Intent(permissionAction).setPackage(applicationContext.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val manager = usbManager
        if (manager == null) {
            unregisterReceiverSafely(permissionReceiver)
            if (continuation.isActive) continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        try {
            manager.requestPermission(device, permissionIntent)
        } catch (exception: SecurityException) {
            unregisterReceiverSafely(permissionReceiver)
            if (continuation.isActive) continuation.resume(false)
        }
    }

    private fun registerDetachReceiver() {
        if (detachReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent == null || intent.action != UsbManager.ACTION_USB_DEVICE_DETACHED) return
                val detachedDevice = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java) ?: return
                if (detachedDevice.deviceName != attachedDevice?.deviceName) return
                scope.launch {
                    closeTransport()
                    markTransportDisconnected()
                }
            }
        }
        ContextCompat.registerReceiver(
            applicationContext,
            receiver,
            IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        detachReceiver = receiver
    }

    private fun unregisterDetachReceiver() {
        val receiver = detachReceiver ?: return
        detachReceiver = null
        unregisterReceiverSafely(receiver)
    }

    private fun unregisterReceiverSafely(receiver: BroadcastReceiver) {
        runCatching { applicationContext.unregisterReceiver(receiver) }
    }

    private data class UsbBulkEndpoint(
        val usbInterface: UsbInterface,
        val endpoint: UsbEndpoint,
    )

    private companion object {
        const val USB_CHUNK_BYTES = 4096
        const val USB_TIMEOUT_MILLIS = 5000
        const val USB_PERMISSION_REQUEST_CODE = 7
    }
}
