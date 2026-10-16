package id.umkm.pos.pro.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.ui.LocalAppContainer

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    onOpenCashier: () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    reportRepository = container.reportRepository,
                    transactionRepository = container.transactionRepository,
                    productRepository = container.productRepository,
                    settingsRepository = container.settingsRepository,
                    clock = container.clock,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsState()
    DashboardScreen(
        state = state,
        onPresetSelected = viewModel::selectPreset,
        onOpenCashier = onOpenCashier,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}
