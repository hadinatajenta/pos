package id.umkm.pos.pro.core.contract

import kotlinx.coroutines.flow.Flow

interface ScannerPreferences {
    fun observeHidScannerEnabled(): Flow<Boolean>
    suspend fun setHidScannerEnabled(isEnabled: Boolean)
}
