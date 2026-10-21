package id.umkm.pos.pro.ui.settings

import id.umkm.pos.pro.core.contract.AppError

sealed interface BackupUiState {

    data object Idle : BackupUiState

    data object Exporting : BackupUiState

    data class Exported(val fileName: String) : BackupUiState

    data object Importing : BackupUiState

    data class Imported(
        val rowCount: Int,
        val securityResetFromBackup: Boolean = false,
    ) : BackupUiState

    data class Failed(val error: AppError) : BackupUiState
}
