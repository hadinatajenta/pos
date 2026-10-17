package id.umkm.pos.pro.ui.settings

import id.umkm.pos.pro.core.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings? = null,
    val isImportingQris: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val isQrisConfigured: Boolean get() = settings?.qrisPayload != null
}
