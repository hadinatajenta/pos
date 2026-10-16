package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_store")
data class SecurityStoreEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "pin_hash") val pinHash: String?,
    @ColumnInfo(name = "recovery_answer_hash") val recoveryAnswerHash: String?,
    @ColumnInfo(name = "is_pin_enabled") val isPinEnabled: Boolean,
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}
