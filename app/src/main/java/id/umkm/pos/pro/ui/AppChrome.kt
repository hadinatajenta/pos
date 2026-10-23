package id.umkm.pos.pro.ui

import androidx.compose.runtime.staticCompositionLocalOf

val LocalFullScreenContent = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
