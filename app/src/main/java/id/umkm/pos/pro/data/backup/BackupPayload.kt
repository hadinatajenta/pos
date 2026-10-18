package id.umkm.pos.pro.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BackupPayload(
    val formatVersion: Int,
    val schemaVersion: Int,
    val exportedAt: Long,
    val tables: Map<String, List<Map<String, JsonElement>>>,
)
