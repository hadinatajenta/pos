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
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.file.BinarySink
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity
import id.umkm.pos.pro.data.local.entity.ProductEntity
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import id.umkm.pos.pro.data.local.entity.SettingsEntity
import id.umkm.pos.pro.data.local.entity.StaffEntity
import id.umkm.pos.pro.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
    fun exportThenImportRestoresProductsAndSettings() = runBlocking<Unit> {
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
    fun importRejectsAnInvalidFileAndKeepsExistingData() = runBlocking<Unit> {
        val productId = database.productDao().insertProduct(product(name = "Teh Manis", price = 5_000L))
        backupFile.writeText("this is not a backup")

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Failure::class.java)
        assertThat((imported as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(database.productDao().getProductById(productId)?.name).isEqualTo("Teh Manis")
    }

    @Test
    fun importRejectsABackupFromANewerSchema() = runBlocking<Unit> {
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
    fun importRejectsAFileWithUnknownColumns() = runBlocking<Unit> {
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
    fun theBackupCarriesNoPinMaterialAndARestoreTurnsThePinOff() = runBlocking<Unit> {
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
    fun aLegacyBackupThatStillCarriesTheSecurityTableIsIgnoredRatherThanImported() = runBlocking<Unit> {
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

    @Test
    fun exportThenImportPreservesCashierShiftsAndTransactionCashierNames() = runBlocking<Unit> {
        val shiftId = database.shiftDao().insertShift(
            CashierShiftEntity(
                cashierName = "Sari",
                openedAt = 1_700_000_000_000L,
                closedAt = 1_700_003_600_000L,
                startingCash = 100_000L,
                expectedEndingCash = 450_000L,
                actualEndingCash = 445_000L,
            ),
        )
        val transactionId = database.transactionDao().insertTransaction(
            transaction(invoiceNo = "INV-SHIFT-1", cashierName = "Sari"),
        )

        val exported = backupService.exportJson("shift-round-trip.json")

        assertThat(exported).isInstanceOf(AppResult.Success::class.java)
        database.openHelper.writableDatabase.delete("cashier_shifts", null, null)
        database.openHelper.writableDatabase.delete("transactions", null, null)

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Success::class.java)
        val restoredShift = database.shiftDao().getShift(shiftId)
        assertThat(restoredShift?.cashierName).isEqualTo("Sari")
        assertThat(restoredShift?.openedAt).isEqualTo(1_700_000_000_000L)
        assertThat(restoredShift?.closedAt).isEqualTo(1_700_003_600_000L)
        assertThat(restoredShift?.startingCash).isEqualTo(100_000L)
        assertThat(restoredShift?.expectedEndingCash).isEqualTo(450_000L)
        assertThat(restoredShift?.actualEndingCash).isEqualTo(445_000L)

        val restoredTransaction = database.transactionDao().getTransaction(transactionId)
        assertThat(restoredTransaction?.invoiceNo).isEqualTo("INV-SHIFT-1")
        assertThat(restoredTransaction?.cashierName).isEqualTo("Sari")
    }

    @Test
    fun importAcceptsATransactionRowWithoutACashierNameAndStoresNull() = runBlocking<Unit> {
        val tables = BackupTables.insertOrder
            .associateWith { emptyList<Map<String, JsonElement>>() }
            .toMutableMap()
        tables["transactions"] = listOf(legacyTransactionRow())
        val legacyPayload = BackupPayload(
            formatVersion = BackupTables.FORMAT_VERSION,
            schemaVersion = database.openHelper.readableDatabase.version,
            exportedAt = 0L,
            tables = tables,
        )
        backupFile.writeText(BackupJsonCodec.encode(legacyPayload))

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Success::class.java)
        val restoredTransaction = database.transactionDao().getTransaction(1L)
        assertThat(restoredTransaction?.invoiceNo).isEqualTo("INV-OLD-FORMAT")
        assertThat(restoredTransaction?.cashierName).isNull()
    }

    @Test
    fun exportThenImportPreservesStaffShiftAndTransactionAttribution() = runBlocking<Unit> {
        val sariId = database.staffDao().insertStaff(
            StaffEntity(name = "Sari", createdAt = 1_700_000_000_000L, updatedAt = 1_700_000_000_000L),
        )
        val budiId = database.staffDao().insertStaff(
            StaffEntity(name = "Budi", createdAt = 1_700_000_000_000L, updatedAt = 1_700_000_000_000L),
        )
        database.settingsDao().upsertSettings(settings(storeName = "Toko Staf"))
        database.staffDao().setActiveStaffId(sariId)
        val shiftId = database.shiftDao().insertShift(
            CashierShiftEntity(
                cashierName = "Sari",
                openedAt = 1_700_000_000_000L,
                closedAt = 1_700_003_600_000L,
                startingCash = 100_000L,
                expectedEndingCash = 450_000L,
                actualEndingCash = 445_000L,
                openedByStaffId = sariId,
                closedByStaffId = budiId,
                closedByName = "Budi",
            ),
        )
        val transactionId = database.transactionDao().insertTransaction(
            transaction(invoiceNo = "INV-STAFF-1", cashierName = "Sari").copy(
                shiftId = shiftId,
                staffId = sariId,
            ),
        )

        val exported = backupService.exportJson("staff-round-trip.json")

        assertThat(exported).isInstanceOf(AppResult.Success::class.java)
        database.openHelper.writableDatabase.delete("cashier_shifts", null, null)
        database.openHelper.writableDatabase.delete("transactions", null, null)
        database.openHelper.writableDatabase.delete("staffs", null, null)

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Success::class.java)
        val summary = (imported as AppResult.Success<BackupImportSummary>).value
        assertThat(summary.tableRowCounts[BackupTables.STAFF_TABLE]).isEqualTo(2)

        val restoredStaff = database.staffDao().observeStaff().first()
        assertThat(restoredStaff.map { staff -> staff.name }).containsExactly("Budi", "Sari")
        assertThat(restoredStaff.map { staff -> staff.id }).containsExactly(sariId, budiId)
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(sariId)
        assertThat(database.settingsDao().getSettings()?.activeStaffId).isEqualTo(sariId)

        val restoredShift = database.shiftDao().getShift(shiftId)
        assertThat(restoredShift?.cashierName).isEqualTo("Sari")
        assertThat(restoredShift?.openedByStaffId).isEqualTo(sariId)
        assertThat(restoredShift?.closedByStaffId).isEqualTo(budiId)
        assertThat(restoredShift?.closedByName).isEqualTo("Budi")

        val restoredTransaction = database.transactionDao().getTransaction(transactionId)
        assertThat(restoredTransaction?.cashierName).isEqualTo("Sari")
        assertThat(restoredTransaction?.shiftId).isEqualTo(shiftId)
        assertThat(restoredTransaction?.staffId).isEqualTo(sariId)
    }

    @Test
    fun importingALegacyBackupWithoutStaffsSeedsTheDefaultStaff() = runBlocking<Unit> {
        val tables = BackupTables.insertOrder
            .associateWith { emptyList<Map<String, JsonElement>>() }
            .toMutableMap()
        tables.remove(BackupTables.STAFF_TABLE)
        tables["transactions"] = listOf(legacyTransactionRow())
        tables["app_settings"] = listOf(legacySettingsRow())
        val legacyPayload = BackupPayload(
            formatVersion = BackupTables.FORMAT_VERSION,
            schemaVersion = 7,
            exportedAt = 0L,
            tables = tables,
        )
        backupFile.writeText(BackupJsonCodec.encode(legacyPayload))

        val imported = backupService.importJson(Uri.fromFile(backupFile).toString())

        assertThat(imported).isInstanceOf(AppResult.Success::class.java)
        val summary = (imported as AppResult.Success<BackupImportSummary>).value
        assertThat(summary.tableRowCounts[BackupTables.STAFF_TABLE]).isEqualTo(0)

        val seededStaff = database.staffDao().observeStaff().first().single()
        assertThat(seededStaff.name).isEqualTo("Kasir Utama")
        assertThat(seededStaff.isActive).isTrue()
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(seededStaff.id)
        assertThat(database.settingsDao().getSettings()?.activeStaffId).isEqualTo(seededStaff.id)
        assertThat(database.settingsDao().getSettings()?.storeName).isEqualTo("Toko Lama")

        val restoredTransaction = database.transactionDao().getTransaction(1L)
        assertThat(restoredTransaction?.invoiceNo).isEqualTo("INV-OLD-FORMAT")
        assertThat(restoredTransaction?.cashierName).isNull()
        assertThat(restoredTransaction?.staffId).isNull()
        assertThat(restoredTransaction?.shiftId).isNull()
    }

    private fun transaction(invoiceNo: String, cashierName: String?): TransactionEntity = TransactionEntity(
        invoiceNo = invoiceNo,
        createdAt = 1_700_000_000_000L,
        cashierId = null,
        customerId = null,
        subtotal = 150_000L,
        discountTotal = 0L,
        invoiceDiscount = 0L,
        taxTotal = 0L,
        rounding = 0L,
        grandTotal = 150_000L,
        totalHpp = 90_000L,
        netProfit = 60_000L,
        paymentMethod = PaymentMethod.CASH,
        paidAmount = 150_000L,
        changeAmount = 0L,
        remainingDebt = 0L,
        dueDate = null,
        status = TransactionStatus.COMPLETED,
        notes = null,
        cashierName = cashierName,
    )

    private fun legacyTransactionRow(): Map<String, JsonElement> = mapOf(
        "id" to JsonPrimitive(1L),
        "invoice_no" to JsonPrimitive("INV-OLD-FORMAT"),
        "created_at" to JsonPrimitive(1_700_000_000_000L),
        "cashier_id" to JsonNull,
        "customer_id" to JsonNull,
        "subtotal" to JsonPrimitive(150_000L),
        "discount_total" to JsonPrimitive(0L),
        "invoice_discount" to JsonPrimitive(0L),
        "tax_total" to JsonPrimitive(0L),
        "rounding" to JsonPrimitive(0L),
        "grand_total" to JsonPrimitive(150_000L),
        "total_hpp" to JsonPrimitive(90_000L),
        "net_profit" to JsonPrimitive(60_000L),
        "payment_method" to JsonPrimitive("CASH"),
        "paid_amount" to JsonPrimitive(150_000L),
        "change_amount" to JsonPrimitive(0L),
        "remaining_debt" to JsonPrimitive(0L),
        "due_date" to JsonNull,
        "status" to JsonPrimitive("COMPLETED"),
        "notes" to JsonNull,
    )

    private fun legacySettingsRow(): Map<String, JsonElement> = mapOf(
        "id" to JsonPrimitive(1L),
        "store_name" to JsonPrimitive("Toko Lama"),
        "cashier_name" to JsonPrimitive("Sari"),
        "tax_rate_percent" to JsonPrimitive(0L),
        "rounding_increment" to JsonPrimitive(0L),
        "is_pin_enabled" to JsonPrimitive(0L),
        "printer_address" to JsonNull,
        "printer_name" to JsonNull,
        "paper_width_mm" to JsonPrimitive(58L),
        "language_code" to JsonPrimitive("id"),
        "qris_image_path" to JsonNull,
        "qris_payload" to JsonNull,
    )

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
