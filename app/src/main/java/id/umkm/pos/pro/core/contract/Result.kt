package id.umkm.pos.pro.core.contract

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    val message: String

    data class Validation(override val message: String) : AppError
    data class NotFound(override val message: String) : AppError
    data class Storage(override val message: String) : AppError
    data class Hardware(override val message: String) : AppError
    data class Network(override val message: String) : AppError
    data class Unexpected(override val message: String) : AppError
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}
