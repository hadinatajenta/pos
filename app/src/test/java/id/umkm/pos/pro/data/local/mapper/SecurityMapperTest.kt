package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityAuditResult
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.data.local.entity.SecurityAuditEntity
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import org.junit.Test

class SecurityMapperTest {

    @Test
    fun storeEntityMapsEverySecurityFieldToTheModel() {
        val entity = SecurityStoreEntity(
            id = 1,
            pinHash = "pbkdf2\$sha256\$100000\$aa\$bb",
            recoveryAnswerHash = null,
            isPinEnabled = true,
            recoveryCodeHash = "pbkdf2\$sha256\$100000\$cc\$dd",
            failedAttempts = 3,
            lockoutUntilMillis = 1_700_000_000_000L,
            sessionRole = SecurityRole.OWNER.name,
            sessionUnlockedUntilMillis = 1_700_000_060_000L,
            pinUpdatedAtMillis = 1_690_000_000_000L,
        )

        val state = entity.toSecurityState()

        assertThat(state.isPinEnabled).isTrue()
        assertThat(state.failedAttempts).isEqualTo(3)
        assertThat(state.lockoutUntilMillis).isEqualTo(1_700_000_000_000L)
        assertThat(state.sessionRole).isEqualTo(SecurityRole.OWNER)
        assertThat(state.sessionUnlockedUntilMillis).isEqualTo(1_700_000_060_000L)
        assertThat(state.pinUpdatedAtMillis).isEqualTo(1_690_000_000_000L)
        assertThat(state.recoveryCodeConfigured).isTrue()
    }

    @Test
    fun anUnknownSessionRoleAndABlankRecoveryCodeDegradeSafely() {
        val entity = SecurityStoreEntity(
            id = 1,
            pinHash = null,
            recoveryAnswerHash = null,
            isPinEnabled = false,
            recoveryCodeHash = "",
            sessionRole = "SUPERVISOR",
        )

        val state = entity.toSecurityState()

        assertThat(state.sessionRole).isNull()
        assertThat(state.recoveryCodeConfigured).isFalse()
        assertThat(state.isPinEnabled).isFalse()
    }

    @Test
    fun anExpiredSessionIsNotReportedAsActive() {
        val state = SecurityStoreEntity(
            id = 1,
            pinHash = "hash",
            recoveryAnswerHash = null,
            isPinEnabled = true,
            sessionRole = SecurityRole.OWNER.name,
            sessionUnlockedUntilMillis = 1_000L,
        ).toSecurityState()

        assertThat(state.activeSessionRole(nowMillis = 999L)).isEqualTo(SecurityRole.OWNER)
        assertThat(state.activeSessionRole(nowMillis = 1_000L)).isNull()
        assertThat(state.activeSessionRole(nowMillis = 1_001L)).isNull()
    }

    @Test
    fun auditEntriesRoundTripThroughTheEntity() {
        val entry = SecurityAuditEntry(
            occurredAtMillis = 1_700_000_000_000L,
            actionId = "export_backup",
            result = SecurityAuditResult.DENIED,
            actorLabel = "owner",
            targetType = "backup",
            targetId = "7",
            id = 12L,
        )

        val entity = entry.toEntity()

        assertThat(entity.id).isEqualTo(12L)
        assertThat(entity.result).isEqualTo("denied")
        assertThat(entity.actionId).isEqualTo("export_backup")
        assertThat(entity.toModel()).isEqualTo(entry)
    }

    @Test
    fun auditEntitiesUseTheStoredKeyRatherThanTheEnumName() {
        val entity = SecurityAuditEntity(
            id = 1L,
            occurredAtMillis = 5L,
            actionId = "restore_backup",
            result = "restore_reset_pin",
            actorLabel = null,
            targetType = null,
            targetId = null,
        )

        assertThat(entity.toModel().result).isEqualTo(SecurityAuditResult.RESTORE_RESET_PIN)
    }

    @Test
    fun anUnknownStoredResultFallsBackToDenied() {
        val entity = SecurityAuditEntity(
            id = 1L,
            occurredAtMillis = 5L,
            actionId = "process_return",
            result = "from_a_future_version",
            actorLabel = null,
            targetType = null,
            targetId = null,
        )

        assertThat(entity.toModel().result).isEqualTo(SecurityAuditResult.DENIED)
    }
}
