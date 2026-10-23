package id.umkm.pos.pro.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UmkmDatabaseMigrationTest {

    private lateinit var context: Context

    @get:Rule
    val migrationHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        UmkmDatabase::class.java,
    )

    @Before
    fun removeStaleTestDatabase() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE_NAME)
        context.deleteDatabase(TEST_DATABASE_NAME_2_3)
        context.deleteDatabase(TEST_DATABASE_NAME_3_4)
        context.deleteDatabase(TEST_DATABASE_NAME_4_5)
        context.deleteDatabase(TEST_DATABASE_NAME_5_6)
        context.deleteDatabase(TEST_DATABASE_NAME_6_7)
        context.deleteDatabase(TEST_DATABASE_NAME_7_8)
        context.deleteDatabase(TEST_DATABASE_NAME_8_9)
        context.deleteDatabase(TEST_DATABASE_NAME_9_10)
    }


    @Test
    fun migration1To2KeepsLegacyItemsAndAddsNullableFactorAndCounterTable() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME, 1).use { versionOne ->
            versionOne.execSQL(INSERT_LEGACY_TRANSACTION)
            versionOne.execSQL(INSERT_LEGACY_ITEM)
        }

        val versionTwo = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME,
            2,
            true,
            UmkmDatabase.MIGRATION_1_2,
        )

        assertTableRowCount(versionTwo, "transactions", 1)
        assertTableRowCount(versionTwo, "transaction_items", 1)
        assertTableRowCount(versionTwo, "invoice_counters", 0)

        versionTwo.query("SELECT conversion_factor FROM transaction_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertTrue(cursor.isNull(0))
        }
        versionTwo.close()
    }

    @Test
    fun migration2To3KeepsProductsAndAddsNullableImagePath() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_2_3, 2).use { versionTwo ->
            versionTwo.execSQL(INSERT_VERSION_TWO_PRODUCT)
        }

        val versionThree = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_2_3,
            3,
            true,
            UmkmDatabase.MIGRATION_2_3,
        )

        assertTableRowCount(versionThree, "products", 1)

        versionThree.query("SELECT sku, name, base_price, image_path FROM products").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("SKU-LEGACY-1", cursor.getString(0))
            assertEquals("Kopi Susu", cursor.getString(1))
            assertEquals(15_000L, cursor.getLong(2))
            assertTrue(cursor.isNull(3))
        }
        versionThree.close()
    }

    @Test
    fun migration3To4KeepsSettingsAndAddsNullableQrisPayload() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_3_4, 3).use { versionThree ->
            versionThree.execSQL(INSERT_VERSION_THREE_SETTINGS)
        }

        val versionFour = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_3_4,
            4,
            true,
            UmkmDatabase.MIGRATION_3_4,
        )

        versionFour.query(
            "SELECT store_name, qris_image_path, qris_payload FROM app_settings",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("Toko Lama", cursor.getString(0))
            assertEquals("/data/qris.jpg", cursor.getString(1))
            assertTrue(cursor.isNull(2))
        }
        versionFour.close()
    }

    @Test
    fun migration4To5KeepsSettingsAndAddsPrinterAndLanguageColumns() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_4_5, 4).use { versionFour ->
            versionFour.execSQL(INSERT_VERSION_FOUR_SETTINGS)
        }

        val versionFive = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_4_5,
            5,
            true,
            UmkmDatabase.MIGRATION_4_5,
        )

        versionFive.query(
            "SELECT store_name, printer_name, paper_width_mm, language_code FROM app_settings",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("Toko Versi 4", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertEquals(58, cursor.getInt(2))
            assertEquals("id", cursor.getString(3))
        }
        versionFive.close()
    }

    @Test
    fun migration5To6KeepsTheSecurityRowAndAddsPinStateColumnsAndTheAuditTable() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_5_6, 5).use { versionFive ->
            versionFive.execSQL(INSERT_VERSION_FIVE_SECURITY_STORE)
            versionFive.execSQL(INSERT_VERSION_TWO_PRODUCT)
        }

        val versionSix = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_5_6,
            6,
            true,
            UmkmDatabase.MIGRATION_5_6,
        )

        versionSix.query(
            "SELECT pin_hash, is_pin_enabled, failed_attempts, lockout_until, " +
                "recovery_code_hash, session_role, session_unlocked_until, pin_updated_at " +
                "FROM security_store",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("legacy-pin-hash", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertTrue(cursor.isNull(5))
            assertTrue(cursor.isNull(6))
            assertTrue(cursor.isNull(7))
        }

        assertTableRowCount(versionSix, "security_audit_log", 0)
        versionSix.execSQL(INSERT_SECURITY_AUDIT_ROW)
        assertTableRowCount(versionSix, "security_audit_log", 1)
        assertTableRowCount(versionSix, "products", 1)
        versionSix.close()
    }

    @Test
    fun migration6To7KeepsTransactionsAndAddsANullableCashierName() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_6_7, 6).use { versionSix ->
            versionSix.execSQL(INSERT_VERSION_SIX_TRANSACTION)
        }

        val versionSeven = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_6_7,
            7,
            true,
            UmkmDatabase.MIGRATION_6_7,
        )

        assertTableRowCount(versionSeven, "transactions", 1)
        versionSeven.query("SELECT invoice_no, cashier_name FROM transactions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("INV-V6-1", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        versionSeven.close()
    }

    @Test
    fun migration7To8SeedsStaffAndLinksExistingShiftsAndTransactions() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_7_8, 7).use { versionSeven ->
            versionSeven.execSQL(INSERT_VERSION_SEVEN_SETTINGS)
            versionSeven.execSQL(INSERT_VERSION_SEVEN_OPEN_SHIFT)
            versionSeven.execSQL(INSERT_VERSION_SEVEN_CLOSED_SHIFT)
            versionSeven.execSQL(INSERT_VERSION_SEVEN_TRANSACTION)
            versionSeven.execSQL(INSERT_VERSION_SIX_TRANSACTION)
        }

        val versionEight = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_7_8,
            8,
            true,
            UmkmDatabase.MIGRATION_7_8,
        )

        assertTableRowCount(versionEight, "staffs", 1)
        var staffId = 0L
        versionEight.query("SELECT id, name, role, is_active FROM staffs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            staffId = cursor.getLong(0)
            assertEquals("Sari", cursor.getString(1))
            assertEquals("CASHIER", cursor.getString(2))
            assertEquals(1, cursor.getInt(3))
        }
        assertTrue(staffId > 0L)

        versionEight.query(
            "SELECT store_name, cashier_name, active_staff_id FROM app_settings",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Toko Tujuh", cursor.getString(0))
            assertEquals("Sari", cursor.getString(1))
            assertEquals(staffId, cursor.getLong(2))
        }

        assertTableRowCount(versionEight, "cashier_shifts", 2)
        versionEight.query(
            "SELECT id, cashier_name, closed_at, actual_ending_cash, " +
                "opened_by_staff_id, closed_by_staff_id, closed_by_name " +
                "FROM cashier_shifts ORDER BY id",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Sari", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertEquals(staffId, cursor.getLong(4))
            assertTrue(cursor.isNull(5))
            assertTrue(cursor.isNull(6))

            assertTrue(cursor.moveToNext())
            assertEquals("Sari", cursor.getString(1))
            assertEquals(1_700_003_600_000L, cursor.getLong(2))
            assertEquals(445_000L, cursor.getLong(3))
            assertTrue(cursor.isNull(4))
            assertTrue(cursor.isNull(5))
            assertTrue(cursor.isNull(6))
        }

        assertTableRowCount(versionEight, "transactions", 2)
        versionEight.query(
            "SELECT invoice_no, cashier_name, shift_id, staff_id " +
                "FROM transactions ORDER BY id",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("INV-V7-1", cursor.getString(0))
            assertEquals("Sari", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertEquals(staffId, cursor.getLong(3))

            assertTrue(cursor.moveToNext())
            assertEquals("INV-V6-1", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
        versionEight.close()
    }

    @Test
    fun migration8To9BackfillsProductSnapshotsAndKeepsDanglingRowsNull() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_8_9, 8).use { versionEight ->
            versionEight.execSQL(INSERT_VERSION_EIGHT_PRODUCT)
            versionEight.execSQL(INSERT_VERSION_EIGHT_TRANSACTION)
            versionEight.execSQL(INSERT_VERSION_EIGHT_LIVE_ITEM)
            versionEight.execSQL(INSERT_VERSION_EIGHT_DANGLING_ITEM)
            versionEight.execSQL(INSERT_VERSION_EIGHT_VARIANT_ITEM)
        }

        val versionNine = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_8_9,
            9,
            true,
            UmkmDatabase.MIGRATION_8_9,
        )

        assertTableRowCount(versionNine, "transaction_items", 3)
        versionNine.query(
            "SELECT product_id, product_name_snapshot, unit_label_snapshot " +
                "FROM transaction_items ORDER BY id",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(7L, cursor.getLong(0))
            assertEquals("Kopi Susu", cursor.getString(1))
            assertEquals("pcs", cursor.getString(2))

            assertTrue(cursor.moveToNext())
            assertEquals(999L, cursor.getLong(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))

            assertTrue(cursor.moveToNext())
            assertEquals(7L, cursor.getLong(0))
            assertEquals("Kopi Susu", cursor.getString(1))
            assertEquals("dus", cursor.getString(2))
        }
        versionNine.close()
    }

    @Test
    fun migration9To10KeepsProductsAndAddsANullableIcon() {
        migrationHelper.createDatabase(TEST_DATABASE_NAME_9_10, 9).use { versionNine ->
            versionNine.execSQL(INSERT_VERSION_NINE_PRODUCT)
        }

        val versionTen = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME_9_10,
            10,
            true,
            UmkmDatabase.MIGRATION_9_10,
        )

        assertTableRowCount(versionTen, "products", 1)
        versionTen.query("SELECT sku, name, base_price, icon FROM products").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("SKU-V9-9", cursor.getString(0))
            assertEquals("Teh Manis", cursor.getString(1))
            assertEquals(8_000L, cursor.getLong(2))
            assertTrue(cursor.isNull(3))
        }
        versionTen.close()
    }

    private fun assertTableRowCount(
        database: SupportSQLiteDatabase,
        table: String,
        expected: Int,
    ) {
        database.query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "umkm-migration-1-2.db"

        const val TEST_DATABASE_NAME_2_3 = "umkm-migration-2-3.db"

        const val TEST_DATABASE_NAME_3_4 = "umkm-migration-3-4.db"

        const val TEST_DATABASE_NAME_4_5 = "umkm-migration-4-5.db"

        const val TEST_DATABASE_NAME_5_6 = "umkm-migration-5-6.db"

        const val TEST_DATABASE_NAME_6_7 = "umkm-migration-6-7.db"

        const val TEST_DATABASE_NAME_7_8 = "umkm-migration-7-8.db"

        const val TEST_DATABASE_NAME_8_9 = "umkm-migration-8-9.db"

        const val TEST_DATABASE_NAME_9_10 = "umkm-migration-9-10.db"

        const val INSERT_VERSION_NINE_PRODUCT =
            "INSERT INTO products " +
                "(id, sku, barcode, name, category, base_unit, base_hpp, base_price, " +
                "stock_base_qty, min_stock_alert, is_active, image_path) " +
                "VALUES (9, 'SKU-V9-9', NULL, 'Teh Manis', 'Minuman', 'pcs', 4000, 8000, " +
                "30.0, 6.0, 1, NULL)"

        const val INSERT_VERSION_EIGHT_PRODUCT =
            "INSERT INTO products " +
                "(id, sku, barcode, name, category, base_unit, base_hpp, base_price, " +
                "stock_base_qty, min_stock_alert, is_active, image_path) " +
                "VALUES (7, 'SKU-V8-7', NULL, 'Kopi Susu', NULL, 'pcs', 5000, 15000, " +
                "100.0, 5.0, 1, NULL)"

        const val INSERT_VERSION_EIGHT_TRANSACTION =
            "INSERT INTO transactions " +
                "(id, invoice_no, created_at, cashier_id, customer_id, subtotal, discount_total, " +
                "invoice_discount, tax_total, rounding, grand_total, total_hpp, net_profit, " +
                "payment_method, paid_amount, change_amount, remaining_debt, due_date, status, notes) " +
                "VALUES (1, 'INV-V8-1', 1700000000000, NULL, NULL, 6000, 0, 0, 0, 0, 6000, " +
                "3000, 3000, 'CASH', 6000, 0, 0, NULL, 'COMPLETED', NULL)"

        const val INSERT_VERSION_EIGHT_LIVE_ITEM =
            "INSERT INTO transaction_items " +
                "(id, transaction_id, product_id, variant_name, qty, unit_hpp, applied_unit_price, " +
                "is_nego_price, is_wholesale_applied, item_discount, subtotal) " +
                "VALUES (1, 1, 7, NULL, 2.0, 5000, 15000, 0, 0, 0, 30000)"

        const val INSERT_VERSION_EIGHT_DANGLING_ITEM =
            "INSERT INTO transaction_items " +
                "(id, transaction_id, product_id, variant_name, qty, unit_hpp, applied_unit_price, " +
                "is_nego_price, is_wholesale_applied, item_discount, subtotal) " +
                "VALUES (2, 1, 999, NULL, 1.0, 1000, 2000, 0, 0, 0, 2000)"

        const val INSERT_VERSION_EIGHT_VARIANT_ITEM =
            "INSERT INTO transaction_items " +
                "(id, transaction_id, product_id, variant_name, qty, unit_hpp, applied_unit_price, " +
                "is_nego_price, is_wholesale_applied, item_discount, subtotal) " +
                "VALUES (3, 1, 7, 'dus', 1.0, 200000, 240000, 0, 0, 0, 240000)"

        const val INSERT_VERSION_SEVEN_SETTINGS =
            "INSERT INTO app_settings " +
                "(id, store_name, cashier_name, tax_rate_percent, rounding_increment, " +
                "is_pin_enabled, printer_address, printer_name, paper_width_mm, " +
                "language_code, qris_image_path, qris_payload) " +
                "VALUES (1, 'Toko Tujuh', 'Sari', 0, 0, 0, NULL, NULL, 58, 'id', NULL, NULL)"

        const val INSERT_VERSION_SEVEN_OPEN_SHIFT =
            "INSERT INTO cashier_shifts " +
                "(cashier_name, opened_at, closed_at, starting_cash, " +
                "expected_ending_cash, actual_ending_cash) " +
                "VALUES ('Sari', 1700007200000, NULL, 50000, NULL, NULL)"

        const val INSERT_VERSION_SEVEN_CLOSED_SHIFT =
            "INSERT INTO cashier_shifts " +
                "(cashier_name, opened_at, closed_at, starting_cash, " +
                "expected_ending_cash, actual_ending_cash) " +
                "VALUES ('Sari', 1700000000000, 1700003600000, 100000, 450000, 445000)"

        const val INSERT_VERSION_SEVEN_TRANSACTION =
            "INSERT INTO transactions " +
                "(invoice_no, created_at, cashier_id, customer_id, subtotal, discount_total, " +
                "invoice_discount, tax_total, rounding, grand_total, total_hpp, net_profit, " +
                "payment_method, paid_amount, change_amount, remaining_debt, due_date, status, notes, " +
                "cashier_name) " +
                "VALUES ('INV-V7-1', 1700007300000, NULL, NULL, 6000, 0, 0, 0, 0, 6000, " +
                "3000, 3000, 'CASH', 6000, 0, 0, NULL, 'COMPLETED', NULL, 'Sari')"

        const val INSERT_VERSION_SIX_TRANSACTION =
            "INSERT INTO transactions " +
                "(invoice_no, created_at, cashier_id, customer_id, subtotal, discount_total, " +
                "invoice_discount, tax_total, rounding, grand_total, total_hpp, net_profit, " +
                "payment_method, paid_amount, change_amount, remaining_debt, due_date, status, notes) " +
                "VALUES ('INV-V6-1', 1700000000000, NULL, NULL, 6000, 0, 0, 0, 0, 6000, " +
                "3000, 3000, 'CASH', 6000, 0, 0, NULL, 'COMPLETED', NULL)"

        const val INSERT_VERSION_FIVE_SECURITY_STORE =
            "INSERT INTO security_store " +
                "(id, pin_hash, recovery_answer_hash, is_pin_enabled) " +
                "VALUES (1, 'legacy-pin-hash', NULL, 0)"

        const val INSERT_SECURITY_AUDIT_ROW =
            "INSERT INTO security_audit_log " +
                "(occurred_at, action_id, result, actor_label, target_type, target_id) " +
                "VALUES (1700000000000, 'restore_backup', 'restore_reset_pin', NULL, NULL, NULL)"

        const val INSERT_VERSION_THREE_SETTINGS =
            "INSERT INTO app_settings " +
                "(id, store_name, cashier_name, tax_rate_percent, rounding_increment, " +
                "is_pin_enabled, printer_address, qris_image_path) " +
                "VALUES (1, 'Toko Lama', NULL, 0, 0, 0, NULL, '/data/qris.jpg')"

        const val INSERT_VERSION_FOUR_SETTINGS =
            "INSERT INTO app_settings " +
                "(id, store_name, cashier_name, tax_rate_percent, rounding_increment, " +
                "is_pin_enabled, printer_address, qris_image_path, qris_payload) " +
                "VALUES (1, 'Toko Versi 4', NULL, 0, 0, 0, '00:11:22:33:44:55', NULL, NULL)"

        const val INSERT_LEGACY_TRANSACTION =

            "INSERT INTO transactions " +
                "(invoice_no, created_at, cashier_id, customer_id, subtotal, discount_total, " +
                "invoice_discount, tax_total, rounding, grand_total, total_hpp, net_profit, " +
                "payment_method, paid_amount, change_amount, remaining_debt, due_date, status, notes) " +
                "VALUES ('INV-LEGACY-1', 1700000000000, NULL, NULL, 6000, 0, 0, 0, 0, 6000, " +
                "3000, 3000, 'CASH', 6000, 0, 0, NULL, 'COMPLETED', NULL)"

        const val INSERT_LEGACY_ITEM =
            "INSERT INTO transaction_items " +
                "(transaction_id, product_id, variant_name, qty, unit_hpp, applied_unit_price, " +
                "is_nego_price, is_wholesale_applied, item_discount, subtotal) " +
                "VALUES (1, 7, NULL, 3.0, 1000, 2000, 0, 0, 0, 6000)"

        const val INSERT_VERSION_TWO_PRODUCT =
            "INSERT INTO products " +
                "(sku, barcode, name, category, base_unit, base_hpp, base_price, " +
                "stock_base_qty, min_stock_alert, is_active) " +
                "VALUES ('SKU-LEGACY-1', NULL, 'Kopi Susu', NULL, 'pcs', 5000, 15000, " +
                "100.0, 5.0, 1)"
    }
}
