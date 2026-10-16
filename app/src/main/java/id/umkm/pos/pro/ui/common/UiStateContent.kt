package id.umkm.pos.pro.ui.common

import androidx.compose.runtime.Composable
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.UiState

@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    onLoading: @Composable () -> Unit,
    onEmpty: @Composable () -> Unit,
    onError: @Composable (AppError) -> Unit,
    onSuccess: @Composable (T) -> Unit,
) {
    val data = state.successDataOrNull()
    when {
        data != null -> onSuccess(data)
        state is UiState.Error -> onError(state.error)
        state == UiState.Empty -> onEmpty()
        else -> onLoading()
    }
}
