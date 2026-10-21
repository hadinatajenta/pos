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
    @ColumnInfo(name = "recovery_code_hash") val recoveryCodeHash: String? = null,
    @ColumnInfo(name = "failed_attempts") val failedAttempts: Int = 0,
    @ColumnInfo(name = "lockout_until") val lockoutUntilMillis: Long? = null,
    @ColumnInfo(name = "session_role") val sessionRole: String? = null,
    @ColumnInfo(name = "session_unlocked_until") val sessionUnlockedUntilMillis: Long? = null,
    @ColumnInfo(name = "pin_updated_at") val pinUpdatedAtMillis: Long? = null,
) {
    companion object {
        const val SINGLETON_ID: Int = 1

        fun empty(): SecurityStoreEntity = SecurityStoreEntity(
            id = SINGLETON_ID,
            pinHash = null,
            recoveryAnswerHash = null,
            isPinEnabled = false,
        )
    }
}
