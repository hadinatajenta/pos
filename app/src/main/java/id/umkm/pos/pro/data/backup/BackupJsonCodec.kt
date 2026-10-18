package id.umkm.pos.pro.data.backup

import kotlinx.serialization.json.Json

enum class BackupValidationResult {
    VALID,
    MALFORMED,
    UNSUPPORTED_FORMAT,
    NEWER_SCHEMA,
    INCOMPLETE,
}

object BackupJsonCodec {

    private val json = Json { prettyPrint = true }

    fun encode(payload: BackupPayload): String = json.encodeToString(BackupPayload.serializer(), payload)

    fun decodeOrNull(text: String): BackupPayload? =
        try {
            json.decodeFromString(BackupPayload.serializer(), text)
        } catch (error: Exception) {
            null
        }

    fun validate(payload: BackupPayload, currentSchemaVersion: Int): BackupValidationResult = when {
        payload.formatVersion != BackupTables.FORMAT_VERSION -> BackupValidationResult.UNSUPPORTED_FORMAT
        payload.schemaVersion > currentSchemaVersion -> BackupValidationResult.NEWER_SCHEMA
        !payload.tables.keys.containsAll(BackupTables.insertOrder) -> BackupValidationResult.INCOMPLETE
        else -> BackupValidationResult.VALID
    }
}
