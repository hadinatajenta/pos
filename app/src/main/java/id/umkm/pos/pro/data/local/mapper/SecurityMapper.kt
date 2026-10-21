package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityAuditResult
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.data.local.entity.SecurityAuditEntity
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity

fun SecurityStoreEntity.toSecurityState(): SecurityState = SecurityState(
    isPinEnabled = isPinEnabled,
    failedAttempts = failedAttempts,
    lockoutUntilMillis = lockoutUntilMillis,
    sessionRole = SecurityRole.fromName(sessionRole),
    sessionUnlockedUntilMillis = sessionUnlockedUntilMillis,
    pinUpdatedAtMillis = pinUpdatedAtMillis,
    recoveryCodeConfigured = !recoveryCodeHash.isNullOrBlank(),
)

fun SecurityAuditEntity.toModel(): SecurityAuditEntry = SecurityAuditEntry(
    id = id,
    occurredAtMillis = occurredAtMillis,
    actionId = actionId,
    result = SecurityAuditResult.fromStorageKey(result) ?: SecurityAuditResult.DENIED,
    actorLabel = actorLabel,
    targetType = targetType,
    targetId = targetId,
)

fun SecurityAuditEntry.toEntity(): SecurityAuditEntity = SecurityAuditEntity(
    id = id,
    occurredAtMillis = occurredAtMillis,
    actionId = actionId,
    result = result.storageKey,
    actorLabel = actorLabel,
    targetType = targetType,
    targetId = targetId,
)
