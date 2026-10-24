package id.umkm.pos.pro.ui.shift

import androidx.compose.runtime.staticCompositionLocalOf

interface ShiftLauncher {
    fun openShift(cartItemCount: Int = 0, onClearCart: () -> Unit = {})

    fun openQuickShift(onOpened: () -> Unit = {})

    fun openHistory()
}

object NoOpShiftLauncher : ShiftLauncher {
    override fun openShift(cartItemCount: Int, onClearCart: () -> Unit) = Unit

    override fun openQuickShift(onOpened: () -> Unit) = Unit

    override fun openHistory() = Unit
}

val LocalShiftLauncher = staticCompositionLocalOf<ShiftLauncher> { NoOpShiftLauncher }
