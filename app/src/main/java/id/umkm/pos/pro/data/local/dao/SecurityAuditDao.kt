package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.umkm.pos.pro.data.local.entity.SecurityAuditEntity

@Dao
interface SecurityAuditDao {

    @Insert
    suspend fun insert(entry: SecurityAuditEntity)

    @Query("SELECT * FROM security_audit_log ORDER BY occurred_at DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SecurityAuditEntity>

    @Query(
        "DELETE FROM security_audit_log WHERE id NOT IN " +
            "(SELECT id FROM security_audit_log ORDER BY occurred_at DESC, id DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)
}
