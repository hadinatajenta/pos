package id.umkm.pos.pro.data.local

import android.content.Context
import id.umkm.pos.pro.core.contract.ScannerPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val SCANNER_PREFERENCES = "umkm_pos_scanner"
internal const val HID_SCANNER_ENABLED_KEY = "hid_scanner_enabled"

class ScannerPreferenceStore(context: Context) : ScannerPreferences {

    private val preferences =
        context.applicationContext.getSharedPreferences(SCANNER_PREFERENCES, Context.MODE_PRIVATE)

    override fun observeHidScannerEnabled(): Flow<Boolean> = callbackFlow {
        trySend(isHidScannerEnabled())
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == HID_SCANNER_ENABLED_KEY) {
                trySend(isHidScannerEnabled())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override suspend fun setHidScannerEnabled(isEnabled: Boolean) {
        preferences.edit().putBoolean(HID_SCANNER_ENABLED_KEY, isEnabled).apply()
    }

    private fun isHidScannerEnabled(): Boolean =
        preferences.getBoolean(HID_SCANNER_ENABLED_KEY, true)
}
