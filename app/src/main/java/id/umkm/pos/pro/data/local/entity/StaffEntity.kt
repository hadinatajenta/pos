package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.umkm.pos.pro.core.model.SecurityRole

@Entity(
    tableName = "staffs",
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "role") val role: SecurityRole = SecurityRole.CASHIER,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
