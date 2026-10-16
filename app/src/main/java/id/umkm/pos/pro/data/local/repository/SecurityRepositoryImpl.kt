package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.SecurityDao
import id.umkm.pos.pro.data.local.dao.SettingsDao
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import id.umkm.pos.pro.data.local.util.matchesHash

class SecurityRepositoryImpl(
    private val database: UmkmDatabase,
    private val securityDao: SecurityDao,
    private val settingsDao: SettingsDao,
) : SecurityRepository {

    override suspend fun isPinEnabled(): Boolean =
        securityDao.getSecurityStore()?.isPinEnabled ?: false

    override suspend fun setPin(pinHash: String, recoveryAnswerHash: String) {
        database.withTransaction {
            securityDao.upsertSecurityStore(
                SecurityStoreEntity(
                    id = SecurityStoreEntity.SINGLETON_ID,
                    pinHash = pinHash,
                    recoveryAnswerHash = recoveryAnswerHash,
                    isPinEnabled = true,
                ),
            )
            settingsDao.updatePinEnabled(true)
        }
    }

    override suspend fun clearPin() {
        database.withTransaction {
            securityDao.upsertSecurityStore(
                SecurityStoreEntity(
                    id = SecurityStoreEntity.SINGLETON_ID,
                    pinHash = null,
                    recoveryAnswerHash = null,
                    isPinEnabled = false,
                ),
            )
            settingsDao.updatePinEnabled(false)
        }
    }

    override suspend fun verifyPin(pinHash: String): Boolean {
        val storedPinHash = securityDao.getSecurityStore()?.pinHash ?: return false
        return storedPinHash.matchesHash(pinHash)
    }

    override suspend fun verifyRecoveryAnswer(recoveryAnswerHash: String): Boolean {
        val storedAnswerHash = securityDao.getSecurityStore()?.recoveryAnswerHash ?: return false
        return storedAnswerHash.matchesHash(recoveryAnswerHash)
    }
}
