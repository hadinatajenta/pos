package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.SecurityRepository
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityCredentials
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.SecurityAuditDao
import id.umkm.pos.pro.data.local.dao.SecurityDao
import id.umkm.pos.pro.data.local.dao.SettingsDao
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import id.umkm.pos.pro.data.local.mapper.toEntity
import id.umkm.pos.pro.data.local.mapper.toModel
import id.umkm.pos.pro.data.local.mapper.toSecurityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecurityRepositoryImpl(
    private val database: UmkmDatabase,
    private val securityDao: SecurityDao,
    private val securityAuditDao: SecurityAuditDao,
    private val settingsDao: SettingsDao,
) : SecurityRepository {

    override fun observeState(): Flow<SecurityState> =
        securityDao.observeSecurityStore().map { store ->
            store?.toSecurityState() ?: SecurityState()
        }

    override suspend fun getState(): SecurityState =
        securityDao.getSecurityStore()?.toSecurityState() ?: SecurityState()

    override suspend fun getCredentials(): SecurityCredentials {
        val store = securityDao.getSecurityStore() ?: return SecurityCredentials(null, null)
        return SecurityCredentials(
            pinHash = store.pinHash,
            recoveryCodeHash = store.recoveryCodeHash,
        )
    }

    override suspend fun saveCredentials(
        pinHash: String,
        recoveryCodeHash: String,
        pinUpdatedAtMillis: Long,
        audit: SecurityAuditEntry?,
    ) {
        database.withTransaction {
            securityDao.upsertSecurityStore(
                currentStore().copy(
                    pinHash = pinHash,
                    recoveryCodeHash = recoveryCodeHash,
                    isPinEnabled = true,
                    pinUpdatedAtMillis = pinUpdatedAtMillis,
                    failedAttempts = 0,
                    lockoutUntilMillis = null,
                ),
            )
            settingsDao.updatePinEnabled(true)
            insertAudit(audit)
        }
    }

    override suspend fun clearCredentials(audit: SecurityAuditEntry?) {
        database.withTransaction {
            securityDao.upsertSecurityStore(SecurityStoreEntity.empty())
            settingsDao.updatePinEnabled(false)
            insertAudit(audit)
        }
    }

    override suspend fun recordAttempt(
        failedAttempts: Int,
        lockoutUntilMillis: Long?,
        audit: SecurityAuditEntry?,
    ) {
        database.withTransaction {
            securityDao.upsertSecurityStore(
                currentStore().copy(
                    failedAttempts = failedAttempts,
                    lockoutUntilMillis = lockoutUntilMillis,
                ),
            )
            insertAudit(audit)
        }
    }

    override suspend fun updateSession(role: SecurityRole?, unlockedUntilMillis: Long?) {
        database.withTransaction {
            securityDao.upsertSecurityStore(
                currentStore().copy(
                    sessionRole = role?.name,
                    sessionUnlockedUntilMillis = unlockedUntilMillis,
                ),
            )
        }
    }

    override suspend fun appendAudit(entry: SecurityAuditEntry) {
        securityAuditDao.insert(entry.toEntity())
    }

    override suspend fun recentAudit(limit: Int): List<SecurityAuditEntry> =
        securityAuditDao.recent(limit).map { it.toModel() }

    override suspend fun trimAudit(keep: Int) {
        securityAuditDao.trimTo(keep)
    }

    private suspend fun currentStore(): SecurityStoreEntity =
        securityDao.getSecurityStore() ?: SecurityStoreEntity.empty()

    private suspend fun insertAudit(entry: SecurityAuditEntry?) {
        if (entry != null) securityAuditDao.insert(entry.toEntity())
    }
}
