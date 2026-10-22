package id.umkm.pos.pro.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.ui.LocalAppContainer

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    openStaffDirectory: Boolean = false,
    onOpenStaffDirectoryConsumed: () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    qrisService = container.qrisService,
                    dataBackupService = container.dataBackupService,
                    scannerPreferences = container.scannerPreferences,
                    scannerInput = container.scannerInput,
                    clock = container.clock,
                    receiptPrinter = container.receiptPrinter,
                    receiptRenderer = container.receiptRenderer,
                )

            }
        },
    )
    val state by viewModel.uiState.collectAsState()
    SettingsScreen(
        state = state,
        actions = SettingsActions.from(viewModel),
        modifier = modifier,
        onBack = onBack,
        openStaffDirectory = openStaffDirectory,
        onOpenStaffDirectoryConsumed = onOpenStaffDirectoryConsumed,
    )
}
