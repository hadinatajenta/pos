package id.umkm.pos.pro.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.core.contract.BackupImportSummary
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.data.file.BinarySink
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.entity.ProductEntity
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import id.umkm.pos.pro.data.local.entity.SettingsEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class JsonDataBackupServiceTest {

    private lateinit var context: Context
    private lateinit var database: UmkmDatabase
    private lateinit var backupFile: File
    private lateinit var backupService: JsonDataBackupService

    private val fixedClock = object : Clock {
        override fun nowMillis(): Long = 1_700_000_000_000L
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, UmkmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupFile = File(context.cacheDir, "backup-round-trip.json")
        backupService = JsonDataBackupService(
            context = context,
            database = database,
            sink = FileBackupSink(backupFile),
            clock = fixedClock,
        )
    }

    @After
    fun tearDown() {
        database.close()
        backupFile.delete()
    }

    @Test
    fun exportThenImportRestoresProductsAndSettings() = runBlocking {
        val productId = database.productDao().insertProduct(product(name = "Kopi Susu", price = 15_000L))
        database.settingsDao().upsertSettings(settings(storeName = "Toko Berkah"))

        val exported = backupService.exportJson("round-trip.json")

        assertThat(exported).isInstanceOf(AppResult.Success::class.java)
        assertThat(backupFile.exists()).isTrue()

        database.productDao().deleteProduct(productId)
        database.settingsDao().upsertSettings(settings(storeName = "Toko Kosong"))

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Success::class.java)
        val summary = (imported as AppResult.Success<BackupImportSummary>).value
        assertThat(summary.totalRows).isAtLeast(2)
        assertThat(database.productDao().getProductById(productId)?.name).isEqualTo("Kopi Susu")
        assertThat(database.settingsDao().getSettings()?.storeName).isEqualTo("Toko Berkah")
    }

    @Test
    fun importRejectsAnInvalidFileAndKeepsExistingData() = runBlocking {
        val productId = database.productDao().insertProduct(product(name = "Teh Manis", price = 5_000L))
        backupFile.writeText("this is not a backup")

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Failure::class.java)
        assertThat((imported as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(database.productDao().getProductById(productId)?.name).isEqualTo("Teh Manis")
    }

    @Test
    fun importRejectsABackupFromANewerSchema() = runBlocking {
        val productId = database.productDao().insertProduct(product(name = "Gula", price = 12_000L))
        val futurePayload = BackupPayload(
            formatVersion = BackupTables.FORMAT_VERSION,
            schemaVersion = database.openHelper.readableDatabase.version + 1,
            exportedAt = 0L,
            tables = BackupTables.insertOrder.associateWith { table ->
                listOf(mapOf("id" to JsonPrimitive(1L))).takeIf { table == "products" }.orEmpty()
            },
        )
        backupFile.writeText(BackupJsonCodec.encode(futurePayload))

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Failure::class.java)
        assertThat((imported as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(database.productDao().getProductById(productId)?.name).isEqualTo("Gula")
    }

    @Test
    fun importRejectsAFileWithUnknownColumns() = runBlocking {
        val payload = BackupPayload(
            formatVersion = BackupTables.FORMAT_VERSION,
            schemaVersion = database.openHelper.readableDatabase.version,
            exportedAt = 0L,
            tables = BackupTables.insertOrder.associateWith { table ->
                listOf(mapOf("id" to JsonPrimitive(1L), "surprise_column" to JsonPrimitive("x")))
                    .takeIf { table == "products" }.orEmpty()
            },
        )
        backupFile.writeText(BackupJsonCodec.encode(payload))

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Failure::class.java)
        assertThat((imported as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun theBackupCarriesNoPinMaterialAndARestoreTurnsThePinOff() = runBlocking {
        database.securityDao().upsertSecurityStore(
            SecurityStoreEntity(
                pinHash = "pbkdf2\$sha256\$100000\$aa\$bb",
                recoveryAnswerHash = null,
                isPinEnabled = true,
                recoveryCodeHash = "pbkdf2\$sha256\$100000\$cc\$dd",
            ),
        )
        database.settingsDao().upsertSettings(settings(storeName = "Toko Berkah").copy(isPinEnabled = true))

        val exported = backupService.exportJson("pin-backup.json")

        assertThat(exported).isInstanceOf(AppResult.Success::class.java)
        val payloadText = backupFile.readText()
        assertThat(payloadText).doesNotContain("security_store")
        assertThat(payloadText).doesNotContain("security_audit_log")
        assertThat(payloadText).doesNotContain("pin_hash")
        assertThat(payloadText).doesNotContain("recovery_code_hash")

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Success::class.java)
        assertThat((imported as AppResult.Success<BackupImportSummary>).value.securityResetFromBackup)
            .isTrue()
        assertThat(imported.value.ignoredSecurityData).isFalse()

        val store = database.securityDao().getSecurityStore()
        assertThat(store?.isPinEnabled).isFalse()
        assertThat(store?.pinHash).isNull()
        assertThat(store?.recoveryCodeHash).isNull()
        assertThat(store?.sessionRole).isNull()
        assertThat(database.settingsDao().getSettings()?.isPinEnabled).isFalse()
        assertThat(database.securityAuditDao().recent(10).map { entry -> entry.result })
            .contains("restore_reset_pin")
    }

    @Test
    fun aLegacyBackupThatStillCarriesTheSecurityTableIsIgnoredRatherThanImported() = runBlocking {
        database.securityDao().upsertSecurityStore(
            SecurityStoreEntity(
                pinHash = "current-device-hash",
                recoveryAnswerHash = null,
                isPinEnabled = true,
            ),
        )
        val tables = BackupTables.insertOrder
            .associateWith { emptyList<Map<String, JsonPrimitive>>() }
            .toMutableMap()
        tables[BackupTables.SECURITY_TABLE] = listOf(mapOf("id" to JsonPrimitive(1L)))
        val legacyPayload = BackupPayload(
            formatVersion = BackupTables.FORMAT_VERSION,
            schemaVersion = database.openHelper.readableDatabase.version,
            exportedAt = 0L,
            tables = tables,
        )
        backupFile.writeText(BackupJsonCodec.encode(legacyPayload))

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Success::class.java)
        val summary = (imported as AppResult.Success<BackupImportSummary>).value
        assertThat(summary.ignoredSecurityData).isTrue()
        assertThat(database.securityDao().getSecurityStore()?.isPinEnabled).isFalse()
        assertThat(database.securityDao().getSecurityStore()?.pinHash).isNull()
    }

    private fun product(name: String, price: Long): ProductEntity = ProductEntity(
        sku = "SKU-$name",
        barcode = null,
        name = name,
        category = "Minuman",
        baseUnit = "pcs",
        baseHpp = price / 2,
        basePrice = price,
        stockBaseQty = 10.0,
        minStockAlert = 2.0,
        isActive = true,
    )

    private fun settings(storeName: String): SettingsEntity = SettingsEntity(
        storeName = storeName,
        cashierName = null,
        taxRatePercent = 0,
        roundingIncrement = 0L,
        isPinEnabled = false,
        printerAddress = null,
        qrisImagePath = null,
        qrisPayload = null,
    )

    private class FileBackupSink(private val target: File) : BinarySink {
        override suspend fun write(targetPath: String, content: ByteArray): Long {
            target.writeBytes(content)
            return content.size.toLong()
        }
    }
}
