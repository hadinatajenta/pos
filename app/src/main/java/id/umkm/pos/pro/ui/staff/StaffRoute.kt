package id.umkm.pos.pro.ui.staff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.ui.LocalAppContainer

@Composable
fun StaffRoute(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val container = LocalAppContainer.current
    val viewModelFactory = remember(container) {
        viewModelFactory {
            initializer {
                StaffViewModel(staffRepository = container.staffRepository)
            }
        }
    }
    val viewModel: StaffViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.state.collectAsState()
    val actions = remember(viewModel) { StaffActions.from(viewModel) }
    StaffScreen(
        state = state,
        actions = actions,
        modifier = modifier,
        onBack = onBack,
    )
}
