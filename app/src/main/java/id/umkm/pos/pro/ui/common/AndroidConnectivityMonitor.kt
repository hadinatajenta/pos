package id.umkm.pos.pro.ui.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {

    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = connectivityManager ?: run {
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyOnline(manager))
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            }
        }
        trySend(isCurrentlyOnline(manager))
        val isRegistered = try {
            manager.registerNetworkCallback(request, callback)
            true
        } catch (securityException: SecurityException) {
            false
        } catch (runtimeException: RuntimeException) {
            false
        }
        if (!isRegistered) {
            trySend(true)
        }
        awaitClose {
            if (isRegistered) {
                runCatching { manager.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged().catch { emit(true) }

    private fun isCurrentlyOnline(manager: ConnectivityManager): Boolean {
        return try {
            val activeNetwork = manager.activeNetwork ?: return false
            manager.getNetworkCapabilities(activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } catch (securityException: SecurityException) {
            true
        }
    }
}

@Composable
fun rememberAndroidConnectivityMonitor(): ConnectivityMonitor {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidConnectivityMonitor(context) }
}
