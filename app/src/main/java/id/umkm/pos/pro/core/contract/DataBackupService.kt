package id.umkm.pos.pro.core.contract

data class BackupImportSummary(
    val tableRowCounts: Map<String, Int>,
    val securityResetFromBackup: Boolean = false,
    val ignoredSecurityData: Boolean = false,
) {
    val totalRows: Int get() = tableRowCounts.values.sum()
}

interface DataBackupService {
    suspend fun exportJson(displayName: String): AppResult<BackupArtifact>
    suspend fun importJson(sourceUri: String): AppResult<BackupImportSummary>
}
