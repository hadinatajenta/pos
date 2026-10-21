package id.umkm.pos.pro.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.ui.LocalAppContainer

@Composable
fun SecurityRoute(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: SecurityViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SecurityViewModel(
                    securityService = container.securityService,
                    clock = container.clock,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.onRefreshAudit()
    }
    SecurityScreen(
        state = state,
        actions = SecurityActions.from(viewModel),
        onBack = onBack,
        modifier = modifier,
    )
}
