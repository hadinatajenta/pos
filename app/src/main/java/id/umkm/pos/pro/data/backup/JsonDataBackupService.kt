package id.umkm.pos.pro.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.core.contract.BackupImportSummary
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.DataBackupService
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityAuditResult
import id.umkm.pos.pro.data.file.BinarySink
import id.umkm.pos.pro.data.local.UmkmDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.io.IOException

class JsonDataBackupService(
    private val context: Context,
    private val database: UmkmDatabase,
    private val sink: BinarySink,
    private val clock: Clock,
) : DataBackupService {

    override suspend fun exportJson(displayName: String): AppResult<BackupArtifact> {
        if (displayName.isBlank()) {
            return AppResult.Failure(AppError.Validation("backup file name must not be blank"))
        }
        val readableDatabase = database.openHelper.readableDatabase
        val payload = try {
            BackupPayload(
                formatVersion = BackupTables.FORMAT_VERSION,
                schemaVersion = readableDatabase.version,
                exportedAt = clock.nowMillis(),
                tables = BackupTables.insertOrder.associateWith { table -> readRows(readableDatabase, table) },
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return AppResult.Failure(AppError.Storage(error.message ?: "failed to read backup data"))
        }

        val byteCount = try {
            sink.write(displayName, BackupJsonCodec.encode(payload).toByteArray(Charsets.UTF_8))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return AppResult.Failure(AppError.Storage(error.message ?: "failed to write backup file"))
        }

        return AppResult.Success(BackupArtifact(path = displayName, byteCount = byteCount))
    }

    override suspend fun importJson(sourceUri: String): AppResult<BackupImportSummary> {
        if (sourceUri.isBlank()) {
            return AppResult.Failure(AppError.Validation("backup source must not be blank"))
        }
        val text = try {
            readSourceText(sourceUri)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return AppResult.Failure(AppError.Storage(error.message ?: "failed to read backup file"))
        }

        val payload = BackupJsonCodec.decodeOrNull(text)
            ?: return AppResult.Failure(AppError.Validation("backup file is not a valid backup"))
        val writableDatabase = database.openHelper.writableDatabase
        val validation = BackupJsonCodec.validate(payload, writableDatabase.version)
        if (validation != BackupValidationResult.VALID) {
            return AppResult.Failure(AppError.Validation("backup rejected: $validation"))
        }
        val columnMismatch = findColumnMismatch(writableDatabase, payload)
        if (columnMismatch != null) {
            return AppResult.Failure(AppError.Validation("backup rejected: unknown columns $columnMismatch"))
        }

        return try {
            var tableRowCounts: Map<String, Int> = emptyMap()
            database.withTransaction {
                BackupTables.deleteOrder.forEach { table ->
                    writableDatabase.delete(table, null, null)
                }
                tableRowCounts = BackupTables.insertOrder.associateWith { table ->
                    val rows = payload.tables[table].orEmpty()
                    rows.forEach { row ->
                        writableDatabase.insert(table, SQLiteDatabase.CONFLICT_NONE, toContentValues(row))
                    }
                    rows.size
                }
                resetSecurityAfterRestore(writableDatabase)
            }
            AppResult.Success(
                BackupImportSummary(
                    tableRowCounts = tableRowCounts,
                    securityResetFromBackup = true,
                    ignoredSecurityData = payload.tables.containsKey(BackupTables.SECURITY_TABLE),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppResult.Failure(AppError.Storage(error.message ?: "failed to restore backup data"))
        }
    }

    private fun resetSecurityAfterRestore(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM `${BackupTables.SECURITY_TABLE}`")
        database.execSQL(
            "INSERT INTO `${BackupTables.SECURITY_TABLE}` " +
                "(`id`, `pin_hash`, `recovery_answer_hash`, `is_pin_enabled`, `recovery_code_hash`, " +
                "`failed_attempts`, `lockout_until`, `session_role`, `session_unlocked_until`, " +
                "`pin_updated_at`) VALUES (1, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL)",
        )
        database.execSQL("UPDATE `app_settings` SET `is_pin_enabled` = 0 WHERE id = 1")
        database.execSQL(
            "INSERT INTO `${BackupTables.SECURITY_AUDIT_TABLE}` " +
                "(`occurred_at`, `action_id`, `result`, `actor_label`, `target_type`, `target_id`) " +
                "VALUES (?, ?, ?, NULL, NULL, NULL)",
            arrayOf(
                clock.nowMillis(),
                CriticalAction.RESTORE_BACKUP.id,
                SecurityAuditResult.RESTORE_RESET_PIN.storageKey,
            ),
        )
    }

    private suspend fun readSourceText(sourceUri: String): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(Uri.parse(sourceUri))
            ?: throw IOException("failed to open the backup file")
        stream.use { input -> input.readBytes().toString(Charsets.UTF_8) }
    }

    private fun readRows(database: SupportSQLiteDatabase, table: String): List<Map<String, JsonElement>> {
        val rows = mutableListOf<Map<String, JsonElement>>()
        database.query("SELECT * FROM `$table`").use { cursor ->
            val columnNames = cursor.columnNames
            while (cursor.moveToNext()) {
                val row = LinkedHashMap<String, JsonElement>(columnNames.size)
                columnNames.forEachIndexed { index, columnName ->
                    row[columnName] = readColumn(cursor, index, table, columnName)
                }
                rows += row
            }
        }
        return rows
    }

    private fun readColumn(
        cursor: Cursor,
        index: Int,
        table: String,
        columnName: String,
    ): JsonElement = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> JsonNull
        Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(cursor.getLong(index))
        Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(cursor.getDouble(index))
        Cursor.FIELD_TYPE_STRING -> JsonPrimitive(cursor.getString(index))
        else -> throw IOException("unsupported column type in $table.$columnName")
    }

    private fun toContentValues(row: Map<String, JsonElement>): ContentValues {
        val values = ContentValues(row.size)
        row.forEach { (column, element) ->
            when (element) {
                is JsonNull -> values.putNull(column)
                is JsonPrimitive -> when {
                    element.isString -> values.put(column, element.content)
                    element.booleanOrNull != null -> values.put(column, if (element.boolean) 1 else 0)
                    element.longOrNull != null -> values.put(column, element.long)
                    else -> values.put(column, element.double)
                }
                else -> throw IOException("unsupported value for column $column")
            }
        }
        return values
    }

    private fun findColumnMismatch(database: SupportSQLiteDatabase, payload: BackupPayload): String? {
        BackupTables.insertOrder.forEach { table ->
            val knownColumns = database.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                buildSet {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(nameIndex))
                    }
                }
            }
            payload.tables[table].orEmpty().forEach { row ->
                val unknownColumns = row.keys - knownColumns
                if (unknownColumns.isNotEmpty()) {
                    return "$table: ${unknownColumns.sorted().joinToString()}"
                }
            }
        }
        return null
    }
}
