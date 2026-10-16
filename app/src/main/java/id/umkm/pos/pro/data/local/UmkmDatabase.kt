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
import id.umkm.pos.pro.data.local.dao.SecurityDao
import id.umkm.pos.pro.data.local.dao.SettingsDao
import id.umkm.pos.pro.data.local.dao.ShiftDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity
import id.umkm.pos.pro.data.local.entity.CustomerEntity
import id.umkm.pos.pro.data.local.entity.DebtPaymentEntity
import id.umkm.pos.pro.data.local.entity.InvoiceCounterEntity
import id.umkm.pos.pro.data.local.entity.ProductEntity
import id.umkm.pos.pro.data.local.entity.ProductUnitEntity
import id.umkm.pos.pro.data.local.entity.ReturnEntity
import id.umkm.pos.pro.data.local.entity.ReturnLineEntity
import id.umkm.pos.pro.data.local.entity.SecurityStoreEntity
import id.umkm.pos.pro.data.local.entity.SettingsEntity
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
        SettingsEntity::class,
        ReturnEntity::class,
        ReturnLineEntity::class,
        InvoiceCounterEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class UmkmDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun customerDao(): CustomerDao

    abstract fun transactionDao(): TransactionDao

    abstract fun debtPaymentDao(): DebtPaymentDao

    abstract fun shiftDao(): ShiftDao

    abstract fun securityDao(): SecurityDao

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

        fun build(context: Context): UmkmDatabase =
            Room.databaseBuilder(context.applicationContext, UmkmDatabase::class.java, DATABASE_NAME)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(SingletonRowsCallback)
                .build()

        private object SingletonRowsCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO app_settings " +
                        "(id, store_name, cashier_name, tax_rate_percent, rounding_increment, " +
                        "is_pin_enabled, printer_address, qris_image_path) " +
                        "VALUES (1, '', NULL, 0, 0, 0, NULL, NULL)",
                )
                db.execSQL(
                    "INSERT INTO security_store " +
                        "(id, pin_hash, recovery_answer_hash, is_pin_enabled) " +
                        "VALUES (1, NULL, NULL, 0)",
                )
            }
        }
    }
}
