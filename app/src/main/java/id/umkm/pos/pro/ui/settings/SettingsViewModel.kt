package id.umkm.pos.pro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.core.contract.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val qrisService: QrisService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { current -> current.copy(settings = settings) }
            }
        }
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
                            message = "QRIS statis berhasil disimpan",
                            errorMessage = null,
                        )
                    }
                }

                is AppResult.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isImportingQris = false,
                            errorMessage = "Gambar QRIS tidak terbaca. Pastikan foto QRIS terlihat utuh dan jelas.",
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
                current.copy(message = "QRIS statis dihapus", errorMessage = null)
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
                    errorMessage = "Nama toko tidak boleh kosong.",
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
                    message = "Pengaturan toko berhasil disimpan",
                    errorMessage = null,
                )
            }
        }
    }

    fun onMessageConsumed() {
        _uiState.update { current -> current.copy(message = null, errorMessage = null) }
    }

    internal fun onRemoveMessages() = onMessageConsumed()
}
