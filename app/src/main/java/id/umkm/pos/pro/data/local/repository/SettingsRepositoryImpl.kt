package id.umkm.pos.pro.data.local.repository

import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.data.local.dao.SettingsDao
import id.umkm.pos.pro.data.local.mapper.SettingsMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val settingsDao: SettingsDao,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        settingsDao.observeSettings()
            .map { entity -> entity?.let(SettingsMapper::toModel) ?: SettingsMapper.defaultModel() }

    override suspend fun getSettings(): AppSettings =
        settingsDao.getSettings()?.let(SettingsMapper::toModel) ?: SettingsMapper.defaultModel()

    override suspend fun updateSettings(settings: AppSettings) {
        settingsDao.upsertSettings(SettingsMapper.toEntity(settings))
    }
}
