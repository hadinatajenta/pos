package id.umkm.pos.pro.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ConnectivityMonitor {
    val isOnline: Flow<Boolean>
}

object AlwaysOnlineConnectivityMonitor : ConnectivityMonitor {
    override val isOnline: Flow<Boolean> = flowOf(true)
}

val LocalConnectivityMonitor = staticCompositionLocalOf<ConnectivityMonitor> {
    AlwaysOnlineConnectivityMonitor
}
