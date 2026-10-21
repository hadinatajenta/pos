package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {

    @Query("SELECT * FROM security_store WHERE id = 1")
    suspend fun getSecurityStore(): SecurityStoreEntity?

    @Query("SELECT * FROM security_store WHERE id = 1")
    fun observeSecurityStore(): Flow<SecurityStoreEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSecurityStore(store: SecurityStoreEntity)
}
