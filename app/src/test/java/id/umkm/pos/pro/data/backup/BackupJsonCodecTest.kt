package id.umkm.pos.pro.data.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class BackupJsonCodecTest {

    private fun payload(
        formatVersion: Int = BackupTables.FORMAT_VERSION,
        schemaVersion: Int = 5,
        tableNames: List<String> = BackupTables.insertOrder,
    ): BackupPayload = BackupPayload(
        formatVersion = formatVersion,
        schemaVersion = schemaVersion,
        exportedAt = 1_700_000_000_000L,
        tables = tableNames.associateWith { table ->
            listOf(
                mapOf(
                    "id" to JsonPrimitive(1L),
                    "name" to JsonPrimitive("Kopi"),
                    "stock" to JsonPrimitive(2.5),
                    "notes" to JsonNull,
                ),
            ).takeIf { table == "products" }.orEmpty()
        },
    )

    @Test
    fun roundTripPreservesEveryTableAndValue() {
        val original = payload()

        val decoded = BackupJsonCodec.decodeOrNull(BackupJsonCodec.encode(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun encodedJsonKeepsNullAndNumericTypes() {
        val encoded = BackupJsonCodec.encode(payload())

        val decoded = BackupJsonCodec.decodeOrNull(encoded)
        val productRow = decoded?.tables?.get("products")?.first()

        assertThat(productRow?.get("id")).isEqualTo(JsonPrimitive(1L))
        assertThat(productRow?.get("stock")).isEqualTo(JsonPrimitive(2.5))
        assertThat(productRow?.get("notes")).isEqualTo(JsonNull)
    }

    @Test
    fun malformedTextDecodesToNull() {
        assertThat(BackupJsonCodec.decodeOrNull("not json at all")).isNull()
        assertThat(BackupJsonCodec.decodeOrNull("{\"formatVersion\":1}")).isNull()
    }

    @Test
    fun validateAcceptsACurrentBackup() {
        assertThat(BackupJsonCodec.validate(payload(), currentSchemaVersion = 5))
            .isEqualTo(BackupValidationResult.VALID)
    }

    @Test
    fun validateRejectsAnUnsupportedFormatVersion() {
        val result = BackupJsonCodec.validate(payload(formatVersion = 99), currentSchemaVersion = 5)

        assertThat(result).isEqualTo(BackupValidationResult.UNSUPPORTED_FORMAT)
    }

    @Test
    fun validateRejectsABackupFromANewerSchema() {
        val result = BackupJsonCodec.validate(payload(schemaVersion = 6), currentSchemaVersion = 5)

        assertThat(result).isEqualTo(BackupValidationResult.NEWER_SCHEMA)
    }

    @Test
    fun validateRejectsABackupThatMissesATable() {
        val incomplete = payload(tableNames = BackupTables.insertOrder - "returns")

        val result = BackupJsonCodec.validate(incomplete, currentSchemaVersion = 5)

        assertThat(result).isEqualTo(BackupValidationResult.INCOMPLETE)
    }

    @Test
    fun tableOrderIsConsistentAndDeletionIsReversed() {
        assertThat(BackupTables.insertOrder.first()).isEqualTo("products")
        assertThat(BackupTables.deleteOrder.first()).isEqualTo("invoice_counters")
        assertThat(BackupTables.deleteOrder).containsExactlyElementsIn(BackupTables.insertOrder.reversed())
        assertThat(BackupTables.isSupported("products")).isTrue()
        assertThat(BackupTables.isSupported("unknown_table")).isFalse()
    }
}
