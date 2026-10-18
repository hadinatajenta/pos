package id.umkm.pos.pro.ui.common

import id.umkm.pos.pro.core.contract.AppError

fun AppError.toUiText(): UiText = UiText.from { strings ->
    when (this) {
        is AppError.Validation -> strings.errorValidation
        is AppError.NotFound -> strings.errorNotFound
        is AppError.Storage -> strings.errorStorage
        is AppError.Hardware -> strings.errorHardware
        is AppError.Network -> strings.errorNetwork
        is AppError.Unexpected -> strings.errorUnexpected
    }
}

fun AppError?.toUiTextOrNull(): UiText? = this?.toUiText()
