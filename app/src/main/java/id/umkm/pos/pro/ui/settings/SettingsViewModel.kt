package id.umkm.pos.pro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.DataBackupService
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.core.contract.PrinterInfo
import id.umkm.pos.pro.core.contract.PrinterTransport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.core.contract.ReceiptPrinter
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.model.AppLanguage
import id.umkm.pos.pro.data.backup.BackupFileNames
import id.umkm.pos.pro.hardware.receipt.AndroidReceiptRenderer
import id.umkm.pos.pro.hardware.receipt.ReceiptPaperWidth
import id.umkm.pos.pro.hardware.receipt.SampleReceiptBuilder
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.stringsFor
import id.umkm.pos.pro.ui.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val qrisService: QrisService,
    private val dataBackupService: DataBackupService,
    private val clock: Clock,
    private val receiptPrinter: ReceiptPrinter? = null,
    private val receiptRenderer: ReceiptRenderer? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { current -> current.copy(settings = settings) }
                receiptPrinter?.setPaperWidth(settings.paperWidthMm)
            }
        }
        if (receiptPrinter != null) {
            viewModelScope.launch {
                receiptPrinter.connectionState.collect { connection ->
                    _uiState.update { current -> current.copy(printerConnection = connection) }
                }
            }
            refreshPrinters()
        }
    }

    fun refreshPrinters() {
        val printers = receiptPrinter?.getAvailablePrinters() ?: emptyList()
        _uiState.update { it.copy(availablePrinters = printers) }
    }

    fun onSelectLanguage(languageCode: String) {
        val currentSettings = _uiState.value.settings ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(currentSettings.copy(languageCode = languageCode))
            val message = UiText.from { strings ->
                when (languageCode) {
                    "zh" -> strings.settingsLanguageSwitchedChinese
                    "en" -> strings.settingsLanguageSwitchedEnglish
                    else -> strings.settingsLanguageSwitchedIndonesian
                }
            }
            _uiState.update { it.copy(message = message, errorMessage = null) }
        }
    }

    fun onSelectPrinter(printer: PrinterInfo) {
        val currentSettings = _uiState.value.settings ?: return
        val autoWidth = if (printer.address.contains("80")) 80 else currentSettings.paperWidthMm
        viewModelScope.launch {
            settingsRepository.updateSettings(
                currentSettings.copy(
                    printerAddress = printer.address,
                    printerName = printer.name,
                    paperWidthMm = autoWidth,
                ),
            )
            receiptPrinter?.setPaperWidth(autoWidth)
            _uiState.update {
                it.copy(
                    message = UiText.from { strings -> strings.settingsPrinterSelectedMessage(printer.name) },
                    errorMessage = null,
                )
            }
        }
    }

    fun onSetPaperWidth(widthMm: Int) {
        val currentSettings = _uiState.value.settings ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(currentSettings.copy(paperWidthMm = widthMm))
            receiptPrinter?.setPaperWidth(widthMm)
            _uiState.update {
                it.copy(
                    message = UiText.from { strings -> strings.settingsPaperWidthChanged(widthMm) },
                    errorMessage = null,
                )
            }
        }
    }

    fun onConnectPrinter() {
        val currentSettings = _uiState.value.settings ?: return
        val address = currentSettings.printerAddress
        if (address.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = UiText.from { strings -> strings.settingsPrinterPickFirst },
                    message = null,
                )
            }
            return
        }
        val printer = PrinterInfo(
            name = currentSettings.printerName ?: "Bluetooth Printer",
            address = address,
            transport = PrinterTransport.BLUETOOTH_RFCOMM,
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    message = UiText.from { strings -> strings.settingsPrinterConnectingMessage(printer.name) },
                    errorMessage = null,
                )
            }
            when (val result = receiptPrinter?.connect(printer)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            message = UiText.from { strings -> strings.settingsPrinterConnectedMessage(printer.name) },
                            errorMessage = null,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(errorMessage = result.error.toUiText(), message = null)
                    }
                }
                null -> Unit
            }
        }
    }

    fun onDisconnectPrinter() {
        viewModelScope.launch {
            receiptPrinter?.disconnect()
            _uiState.update {
                it.copy(
                    message = UiText.from { strings -> strings.settingsPrinterDisconnectedMessage },
                    errorMessage = null,
                )
            }
        }
    }

    fun onTestPrint() {
        val currentSettings = _uiState.value.settings ?: return
        val printer = receiptPrinter
        val renderer = receiptRenderer
        val sampleDoc = SampleReceiptBuilder.build(
            storeName = currentSettings.storeName,
            strings = stringsFor(AppLanguage.fromCode(currentSettings.languageCode)),
            paperWidthMm = currentSettings.paperWidthMm,
            qrisImagePath = currentSettings.qrisImagePath,
        )
        val widthPx = if (currentSettings.paperWidthMm >= 80) {
            ReceiptPaperWidth.MILLIMETER_80.pixels
        } else {
            ReceiptPaperWidth.MILLIMETER_58.pixels
        }

        val previewBitmap = if (renderer is AndroidReceiptRenderer) {
            renderer.renderPreviewBitmap(sampleDoc, widthPx)
        } else null

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPrintingTest = true,
                    receiptPreviewBitmap = previewBitmap,
                    isPreviewDialogOpen = true,
                    message = null,
                    errorMessage = null,
                )
            }

            if (printer != null) {
                if (_uiState.value.printerConnection != PrinterConnection.CONNECTED &&
                    !currentSettings.printerAddress.isNullOrBlank()
                ) {
                    val pInfo = PrinterInfo(
                        name = currentSettings.printerName ?: "Printer",
                        address = currentSettings.printerAddress,
                        transport = PrinterTransport.BLUETOOTH_RFCOMM,
                    )
                    printer.connect(pInfo)
                }

                when (val result = printer.print(sampleDoc)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isPrintingTest = false,
                                message = UiText.from { strings -> strings.settingsPrinterTestPrinted },
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isPrintingTest = false,
                                errorMessage = UiText.from { strings -> strings.settingsPrinterWarning },
                            )
                        }
                    }
                }
            } else {
                _uiState.update { it.copy(isPrintingTest = false) }
            }
        }
    }

    fun onOpenReceiptPreview() {
        val currentSettings = _uiState.value.settings ?: return
        val renderer = receiptRenderer
        val sampleDoc = SampleReceiptBuilder.build(
            storeName = currentSettings.storeName,
            strings = stringsFor(AppLanguage.fromCode(currentSettings.languageCode)),
            paperWidthMm = currentSettings.paperWidthMm,
            qrisImagePath = currentSettings.qrisImagePath,
        )
        val widthPx = if (currentSettings.paperWidthMm >= 80) {
            ReceiptPaperWidth.MILLIMETER_80.pixels
        } else {
            ReceiptPaperWidth.MILLIMETER_58.pixels
        }
        val previewBitmap = if (renderer is AndroidReceiptRenderer) {
            renderer.renderPreviewBitmap(sampleDoc, widthPx)
        } else null

        _uiState.update {
            it.copy(
                receiptPreviewBitmap = previewBitmap,
                isPreviewDialogOpen = true,
            )
        }
    }

    fun onDismissReceiptPreview() {
        _uiState.update { it.copy(isPreviewDialogOpen = false) }
    }

    fun onQrisImagePicked(sourceUri: String) {
        val currentSettings = _uiState.value.settings ?: return
        if (_uiState.value.isImportingQris) return
        _uiState.update { current ->
            current.copy(isImportingQris = true, message = null, errorMessage = null)
        }
        viewModelScope.launch {
            when (val importResult = qrisService.importStaticQris(sourceUri)) {
                is AppResult.Success -> {
                    settingsRepository.updateSettings(
                        currentSettings.copy(
                            qrisImagePath = importResult.value.imagePath,
                            qrisPayload = importResult.value.payload,
                        ),
                    )
                    if (currentSettings.qrisImagePath != importResult.value.imagePath) {
                        qrisService.deleteQrisImage(currentSettings.qrisImagePath)
                    }
                    _uiState.update { current ->
                        current.copy(
                            isImportingQris = false,
                            message = UiText.from { strings -> strings.settingsQrisSavedMessage },
                            errorMessage = null,
                        )
                    }
                }

                is AppResult.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isImportingQris = false,
                            errorMessage = UiText.from { strings -> strings.settingsQrisImageInvalid },
                        )
                    }
                }
            }
        }
    }

    fun onRemoveQris() {
        val currentSettings = _uiState.value.settings ?: return
        if (_uiState.value.isImportingQris) return
        viewModelScope.launch {
            settingsRepository.updateSettings(
                currentSettings.copy(qrisImagePath = null, qrisPayload = null),
            )
            qrisService.deleteQrisImage(currentSettings.qrisImagePath)
            _uiState.update { current ->
                current.copy(
                    message = UiText.from { strings -> strings.settingsQrisRemovedMessage },
                    errorMessage = null,
                )
            }
        }
    }

    fun onSaveStoreSettings(
        storeName: String,
        cashierName: String,
        taxRatePercent: Int,
        roundingIncrement: Long,
    ) {
        val currentSettings = _uiState.value.settings ?: return
        val trimmedStoreName = storeName.trim()
        if (trimmedStoreName.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    message = null,
                    errorMessage = UiText.from { strings -> strings.settingsStoreNameRequired },
                )
            }
            return
        }
        val safeTax = taxRatePercent.coerceIn(0, 100)
        val safeRounding = roundingIncrement.coerceAtLeast(0L)
        viewModelScope.launch {
            settingsRepository.updateSettings(
                currentSettings.copy(
                    storeName = trimmedStoreName,
                    cashierName = cashierName.trim().ifEmpty { null },
                    taxRatePercent = safeTax,
                    roundingIncrement = safeRounding,
                ),
            )
            _uiState.update { current ->
                current.copy(
                    message = UiText.from { strings -> strings.settingsStoreSavedMessage },
                    errorMessage = null,
                )
            }
        }
    }

    fun onMessageConsumed() {
        _uiState.update { current -> current.copy(message = null, errorMessage = null) }
    }

    fun onExportBackup() {
        viewModelScope.launch {
            _uiState.update { current -> current.copy(backupState = BackupUiState.Exporting) }
            val fileName = BackupFileNames.forTimestamp(clock.nowMillis())
            when (val result = dataBackupService.exportJson(fileName)) {
                is AppResult.Success -> _uiState.update { current ->
                    current.copy(backupState = BackupUiState.Exported(result.value.path))
                }

                is AppResult.Failure -> _uiState.update { current ->
                    current.copy(backupState = BackupUiState.Failed(result.error))
                }
            }
        }
    }

    fun onImportBackup(sourceUri: String) {
        viewModelScope.launch {
            _uiState.update { current -> current.copy(backupState = BackupUiState.Importing) }
            when (val result = dataBackupService.importJson(sourceUri)) {
                is AppResult.Success -> _uiState.update { current ->
                    current.copy(backupState = BackupUiState.Imported(result.value.totalRows))
                }

                is AppResult.Failure -> _uiState.update { current ->
                    current.copy(backupState = BackupUiState.Failed(result.error))
                }
            }
        }
    }

    fun onDismissBackupState() {
        _uiState.update { current -> current.copy(backupState = BackupUiState.Idle) }
    }

    internal fun onRemoveMessages() = onMessageConsumed()
}
