package id.umkm.pos.pro.ui.common

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.UiState

@Suppress("UNCHECKED_CAST")
fun <T> UiState<T>.successDataOrNull(): T? = (this as? UiState.Success<T>)?.data

@Suppress("UNCHECKED_CAST")
fun <T, R> UiState<List<T>>.mapList(transform: (T) -> R): UiState<List<R>> = when (this) {
    UiState.Empty -> UiState.Empty
    UiState.Loading -> UiState.Loading
    is UiState.Error -> UiState.Error(error)
    else -> {
        val items = (this as UiState.Success<List<T>>).data
        if (items.isEmpty()) UiState.Empty else UiState.Success(items.map(transform))
    }
}

fun Throwable.toAppError(): AppError = AppError.Unexpected(
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again.",
)
