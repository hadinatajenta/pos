package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_audit_log")
data class SecurityAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "occurred_at") val occurredAtMillis: Long,
    @ColumnInfo(name = "action_id") val actionId: String,
    @ColumnInfo(name = "result") val result: String,
    @ColumnInfo(name = "actor_label") val actorLabel: String?,
    @ColumnInfo(name = "target_type") val targetType: String?,
    @ColumnInfo(name = "target_id") val targetId: String?,
)
