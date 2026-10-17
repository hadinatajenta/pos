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

        const val INSERT_VERSION_THREE_SETTINGS =
            "INSERT INTO app_settings " +
                "(id, store_name, cashier_name, tax_rate_percent, rounding_increment, " +
                "is_pin_enabled, printer_address, qris_image_path) " +
                "VALUES (1, 'Toko Lama', NULL, 0, 0, 0, NULL, '/data/qris.jpg')"

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
