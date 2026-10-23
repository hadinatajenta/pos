package id.umkm.pos.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import id.umkm.pos.pro.data.local.converter.RoomConverters
import id.umkm.pos.pro.data.local.dao.CustomerDao
import id.umkm.pos.pro.data.local.dao.DebtPaymentDao
import id.umkm.pos.pro.data.local.dao.InvoiceCounterDao
import id.umkm.pos.pro.data.local.dao.ProductDao
import id.umkm.pos.pro.data.local.dao.ReportDao
import id.umkm.pos.pro.data.local.dao.ReturnDao
import id.umkm.pos.pro.data.local.dao.SecurityAuditDao
import id.umkm.pos.pro.data.local.dao.SecurityDao
import id.umkm.pos.pro.data.local.dao.SettingsDao
import id.umkm.pos.pro.data.local.dao.ShiftDao
import id.umkm.pos.pro.data.local.dao.StaffDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity
import id.umkm.pos.pro.data.local.entity.CustomerEntity
import id.umkm.pos.pro.data.local.entity.DebtPaymentEntity
import id.umkm.pos.pro.data.local.entity.InvoiceCounterEntity
import id.umkm.pos.pro.data.local.entity.ProductEntity
import id.umkm.pos.pro.data.local.entity.ProductUnitEntity
import id.umkm.pos.pro.data.local.entity.ReturnEntity
import id.umkm.pos.pro.data.local.entity.ReturnLineEntity
import id.umkm.pos.pro.data.local.entity.SecurityAuditEntity
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import id.umkm.pos.pro.data.local.entity.SettingsEntity
import id.umkm.pos.pro.data.local.entity.StaffEntity
import id.umkm.pos.pro.data.local.entity.TransactionEntity
import id.umkm.pos.pro.data.local.entity.TransactionItemEntity

@Database(
    entities = [
        ProductEntity::class,
        ProductUnitEntity::class,
        CustomerEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        DebtPaymentEntity::class,
        CashierShiftEntity::class,
        SecurityStoreEntity::class,
        SecurityAuditEntity::class,
        SettingsEntity::class,
        StaffEntity::class,
        ReturnEntity::class,
        ReturnLineEntity::class,
        InvoiceCounterEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class UmkmDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun customerDao(): CustomerDao

    abstract fun transactionDao(): TransactionDao

    abstract fun debtPaymentDao(): DebtPaymentDao

    abstract fun shiftDao(): ShiftDao

    abstract fun staffDao(): StaffDao

    abstract fun securityDao(): SecurityDao

    abstract fun securityAuditDao(): SecurityAuditDao

    abstract fun settingsDao(): SettingsDao

    abstract fun reportDao(): ReportDao

    abstract fun returnDao(): ReturnDao

    abstract fun invoiceCounterDao(): InvoiceCounterDao

    companion object {
        const val DATABASE_NAME: String = "umkm_pos.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transaction_items ADD COLUMN conversion_factor REAL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `invoice_counters` " +
                        "(`day_key` TEXT NOT NULL, `last_sequence` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`day_key`))",
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN image_path TEXT")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN qris_payload TEXT")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN printer_name TEXT")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN paper_width_mm INTEGER NOT NULL DEFAULT 58")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN language_code TEXT NOT NULL DEFAULT 'id'")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE security_store ADD COLUMN recovery_code_hash TEXT")
                db.execSQL(
                    "ALTER TABLE security_store ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE security_store ADD COLUMN lockout_until INTEGER")
                db.execSQL("ALTER TABLE security_store ADD COLUMN session_role TEXT")
                db.execSQL("ALTER TABLE security_store ADD COLUMN session_unlocked_until INTEGER")
                db.execSQL("ALTER TABLE security_store ADD COLUMN pin_updated_at INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `security_audit_log` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`occurred_at` INTEGER NOT NULL, `action_id` TEXT NOT NULL, " +
                        "`result` TEXT NOT NULL, `actor_label` TEXT, `target_type` TEXT, " +
                        "`target_id` TEXT)",
                )
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN cashier_name TEXT")
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `staffs` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL COLLATE NOCASE, " +
                        "`role` TEXT NOT NULL, `is_active` INTEGER NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_staffs_name` ON `staffs` (`name`)",
                )
                db.execSQL("ALTER TABLE app_settings ADD COLUMN active_staff_id INTEGER")
                db.execSQL("ALTER TABLE cashier_shifts ADD COLUMN opened_by_staff_id INTEGER")
                db.execSQL("ALTER TABLE cashier_shifts ADD COLUMN closed_by_staff_id INTEGER")
                db.execSQL("ALTER TABLE cashier_shifts ADD COLUMN closed_by_name TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN shift_id INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN staff_id INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_shift_id` " +
                        "ON `transactions` (`shift_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_staff_id` " +
                        "ON `transactions` (`staff_id`)",
                )
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT INTO staffs (name, role, is_active, created_at, updated_at) " +
                        "SELECT COALESCE(NULLIF(TRIM(cashier_name), ''), 'Kasir Utama'), " +
                        "'CASHIER', 1, $now, $now FROM app_settings WHERE id = 1",
                )
                db.execSQL(
                    "UPDATE app_settings SET active_staff_id = " +
                        "(SELECT id FROM staffs ORDER BY id LIMIT 1) WHERE id = 1",
                )
                db.execSQL(
                    "UPDATE cashier_shifts SET opened_by_staff_id = " +
                        "(SELECT id FROM staffs ORDER BY id LIMIT 1) " +
                        "WHERE closed_at IS NULL AND opened_by_staff_id IS NULL",
                )
                db.execSQL(
                    "UPDATE transactions SET staff_id = " +
                        "(SELECT id FROM staffs s WHERE s.name = transactions.cashier_name COLLATE NOCASE) " +
                        "WHERE staff_id IS NULL AND cashier_name IS NOT NULL",
                )
            }
        }

        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transaction_items ADD COLUMN product_name_snapshot TEXT")
                db.execSQL("ALTER TABLE transaction_items ADD COLUMN unit_label_snapshot TEXT")
                db.execSQL(
                    "UPDATE transaction_items SET product_name_snapshot = " +
                        "(SELECT products.name FROM products WHERE products.id = transaction_items.product_id) " +
                        "WHERE product_name_snapshot IS NULL",
                )
                db.execSQL(
                    "UPDATE transaction_items SET unit_label_snapshot = COALESCE(" +
                        "variant_name, " +
                        "(SELECT products.base_unit FROM products WHERE products.id = transaction_items.product_id)" +
                        ") WHERE unit_label_snapshot IS NULL",
                )
            }
        }

        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN icon TEXT")
            }
        }

        fun build(context: Context): UmkmDatabase =
            Room.databaseBuilder(context.applicationContext, UmkmDatabase::class.java, DATABASE_NAME)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                )
                .addCallback(SingletonRowsCallback)
                .build()

        private object SingletonRowsCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO app_settings " +
                        "(id, store_name, cashier_name, tax_rate_percent, rounding_increment, " +
                        "is_pin_enabled, printer_address, printer_name, paper_width_mm, language_code, qris_image_path, qris_payload) " +
                        "VALUES (1, '', NULL, 0, 0, 0, NULL, NULL, 58, 'id', NULL, NULL)",
                )
                db.execSQL(
                    "INSERT INTO security_store " +
                        "(id, pin_hash, recovery_answer_hash, is_pin_enabled, recovery_code_hash, " +
                        "failed_attempts, lockout_until, session_role, session_unlocked_until, " +
                        "pin_updated_at) " +
                        "VALUES (1, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL, NULL)",
                )
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT INTO staffs (name, role, is_active, created_at, updated_at) " +
                        "VALUES ('Kasir Utama', 'CASHIER', 1, $now, $now)",
                )
                db.execSQL(
                    "UPDATE app_settings SET active_staff_id = " +
                        "(SELECT id FROM staffs ORDER BY id LIMIT 1) WHERE id = 1",
                )
            }
        }
    }
}

