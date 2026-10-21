package id.umkm.pos.pro.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.heightIn
import id.umkm.pos.pro.ui.components.ExportDoneRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.data.file.DownloadsFileSink
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.openDownloadedFile
import id.umkm.pos.pro.ui.common.shareDownloadedFile
import id.umkm.pos.pro.ui.components.ExportFailedRow
import id.umkm.pos.pro.ui.components.ExportRunningRow
import id.umkm.pos.pro.ui.components.ExportedFileCard
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val BACKUP_DOCUMENT_TYPES = arrayOf(
    "application/json",
    "application/octet-stream",
    "text/plain",
)

@Composable
internal fun SettingsBackupScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val context = LocalContext.current
    val gate = rememberSecurityGate()
    var isConfirmingImport by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { sourceUri ->
        if (sourceUri != null) {
            actions.onImportBackup(sourceUri.toString())
        }
    }
    val isBusy = state.backupState is BackupUiState.Exporting || state.backupState is BackupUiState.Importing

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.settingsBackupTitle,
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PosSpacing.md,
                    end = PosSpacing.md,
                    top = PosSpacing.sm,
                    bottom = PosSizes.listBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            BackupStatus(
                backupState = state.backupState,
                onOpen = { fileName -> openDownloadedFile(context, fileName) },
                onShare = { fileName -> shareDownloadedFile(context, fileName) },
                onDismiss = actions.onDismissBackupState,
            )

            PosCard(modifier = Modifier.fillMaxWidth()) {
                BackupSectionHeading(
                    iconRes = R.drawable.ic_download,
                    title = strings.settingsBackupExportSection,
                )
                Text(
                    text = strings.settingsBackupExportHint,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryActionButton(
                    text = strings.settingsBackupExportButton,
                    iconRes = R.drawable.ic_download,
                    loading = state.backupState is BackupUiState.Exporting,
                    enabled = !isBusy,
                    onClick = {
                        gate.request(CriticalAction.EXPORT_BACKUP, strings.settingsBackupExportButton) {
                            actions.onExportBackup()
                        }
                    },
                )
            }

            PosCard(modifier = Modifier.fillMaxWidth()) {
                BackupSectionHeading(
                    iconRes = R.drawable.ic_cloud_upload,
                    title = strings.settingsBackupImportSection,
                )
                Text(
                    text = strings.settingsBackupImportHint,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { isConfirmingImport = true },
                    enabled = !isBusy,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LocalPosSemanticPalette.current.warning,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(
                        text = strings.settingsBackupImportButton,
                        style = LocalAppTextStyles.current.buttonLabel,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    if (isConfirmingImport) {
        AlertDialog(
            onDismissRequest = { isConfirmingImport = false },
            title = { Text(strings.settingsBackupConfirmTitle) },
            text = { Text(strings.settingsBackupConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirmingImport = false
                        gate.request(CriticalAction.RESTORE_BACKUP, strings.settingsBackupImportButton) {
                            importLauncher.launch(BACKUP_DOCUMENT_TYPES)
                        }
                    },
                ) {
                    Text(strings.settingsBackupConfirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingImport = false }) {
                    Text(strings.actionCancel)
                }
            },
        )
    }
}

@Composable
private fun BackupStatus(
    backupState: BackupUiState,
    onOpen: (String) -> Boolean,
    onShare: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    val strings = LocalPosStrings.current
    when (backupState) {
        BackupUiState.Idle -> Unit
        BackupUiState.Exporting -> ExportRunningRow(message = strings.settingsBackupExporting)
        BackupUiState.Importing -> ExportRunningRow(message = strings.settingsBackupImporting)

        is BackupUiState.Exported -> ExportedFileCard(
            fileName = backupState.fileName,
            destinationLabel = strings.exportDestination(DownloadsFileSink.DESTINATION_LABEL),
            onOpen = { onOpen(backupState.fileName) },
            onShare = { onShare(backupState.fileName) },
            onDismiss = onDismiss,
        )

        is BackupUiState.Imported -> Column(
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            ExportDoneRow(
                title = strings.settingsBackupImportedTitle,
                detail = strings.settingsBackupImportedRows(backupState.rowCount),
                onDismiss = onDismiss,
            )
            if (backupState.securityResetFromBackup) {
                SecurityResetNotice()
            }
        }

        is BackupUiState.Failed -> ExportFailedRow(
            message = backupErrorMessage(backupState.error, strings),
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun SecurityResetNotice() {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = palette.warning,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Text(
            text = strings.securityRestoreResetNotice,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BackupSectionHeading(
    @DrawableRes iconRes: Int,
    title: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Text(
            text = title,
            style = LocalAppTextStyles.current.sectionHeading,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun backupErrorMessage(error: AppError, strings: PosStrings): String = when (error) {
    is AppError.Validation -> strings.settingsBackupErrorInvalid
    is AppError.Storage -> strings.settingsBackupErrorStorage
    else -> strings.settingsBackupErrorUnexpected
}
