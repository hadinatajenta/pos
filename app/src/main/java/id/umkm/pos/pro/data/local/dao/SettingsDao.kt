package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.umkm.pos.pro.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)

    @Query("UPDATE app_settings SET is_pin_enabled = :isEnabled WHERE id = 1")
    suspend fun updatePinEnabled(isEnabled: Boolean)
}
