package id.umkm.pos.pro.ui

import androidx.compose.runtime.staticCompositionLocalOf
import id.umkm.pos.pro.core.contract.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided")
}
