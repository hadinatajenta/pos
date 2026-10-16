package id.umkm.pos.pro.data.local.repository

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.UmkmDatabase
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
import id.umkm.pos.pro.data.local.entity.ProductEntity
import id.umkm.pos.pro.data.local.entity.ProductUnitEntity
import id.umkm.pos.pro.data.local.entity.ReturnEntity
import id.umkm.pos.pro.data.local.entity.ReturnLineEntity
import id.umkm.pos.pro.data.local.entity.TransactionEntity
import id.umkm.pos.pro.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReturnRepositoryImplTest {

    private val repository = ReturnRepositoryImpl(
        database = FakeUmkmDatabase(),
        returnDao = FakeReturnDao(),
        transactionDao = FakeTransactionDao(),
        productDao = FakeProductDao(),
    )

    @Test
    fun recordReturnRejectsEmptyLineList() = runTest {
        val record = ReturnRecord(
            id = 0L,
            transactionId = 42L,
            createdAt = 1_700_000_000_000L,
            refundAmount = 0L,
            totalHppReversed = 0L,
            note = null,
        )

        val result = repository.recordReturn(record, emptyList())

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
    }
}

private class FakeUmkmDatabase : UmkmDatabase() {

    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper =
        error("fake database must not open a helper")

    override fun createInvalidationTracker(): InvalidationTracker = InvalidationTracker(this)

    override fun clearAllTables() {
        error("fake database must not clear tables")
    }

    override fun productDao(): ProductDao = error("fake database must not expose daos")

    override fun customerDao(): CustomerDao = error("fake database must not expose daos")

    override fun transactionDao(): TransactionDao = error("fake database must not expose daos")

    override fun debtPaymentDao(): DebtPaymentDao = error("fake database must not expose daos")

    override fun shiftDao(): ShiftDao = error("fake database must not expose daos")

    override fun securityDao(): SecurityDao = error("fake database must not expose daos")

    override fun settingsDao(): SettingsDao = error("fake database must not expose daos")

    override fun reportDao(): ReportDao = error("fake database must not expose daos")

    override fun returnDao(): ReturnDao = error("fake database must not expose daos")

    override fun invoiceCounterDao(): InvoiceCounterDao = error("fake database must not expose daos")
}

private class FakeReturnDao : ReturnDao() {

    override suspend fun insertReturn(record: ReturnEntity): Long = unused()

    override suspend fun insertLines(lines: List<ReturnLineEntity>): List<Long> = unused()

    override suspend fun getReturnsForTransaction(transactionId: Long): List<ReturnEntity> = unused()

    override suspend fun getReturnsInRange(startMillis: Long, endMillis: Long): List<ReturnEntity> = unused()

    override suspend fun getLines(returnId: Long): List<ReturnLineEntity> = unused()
}

private class FakeTransactionDao : TransactionDao() {

    override suspend fun insertTransaction(transaction: TransactionEntity): Long = unused()

    override suspend fun insertItems(items: List<TransactionItemEntity>): List<Long> = unused()

    override suspend fun getTransaction(transactionId: Long): TransactionEntity? = unused()

    override suspend fun getItems(transactionId: Long): List<TransactionItemEntity> = unused()

    override fun observeTransactions(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> = unused()

    override suspend fun transactionsInRange(startMillis: Long, endMillis: Long): List<TransactionEntity> = unused()

    override suspend fun sumCashSalesSince(openedAtMillis: Long): Long = unused()

    override suspend fun updateStatus(transactionId: Long, status: TransactionStatus) {
        unused()
    }

    override suspend fun updateDebtState(transactionId: Long, remainingDebt: Long, status: TransactionStatus) {
        unused()
    }

    override suspend fun deleteTransactionsByNote(note: String): Int = unused()
}

private class FakeProductDao : ProductDao {

    override fun observeProducts(pattern: String): Flow<List<ProductEntity>> = unused()

    override suspend fun getProductById(productId: Long): ProductEntity? = unused()

    override suspend fun findProductByBarcode(barcode: String): ProductEntity? = unused()

    override suspend fun insertProduct(product: ProductEntity): Long = unused()

    override suspend fun updateProduct(product: ProductEntity) {
        unused()
    }

    override suspend fun deleteProduct(productId: Long) {
        unused()
    }

    override suspend fun getUnits(productId: Long): List<ProductUnitEntity> = unused()

    override suspend fun findUnitByBarcode(barcode: String): ProductUnitEntity? = unused()

    override suspend fun insertUnit(unit: ProductUnitEntity): Long = unused()

    override suspend fun updateUnit(unit: ProductUnitEntity) {
        unused()
    }

    override suspend fun deleteUnit(unitId: Long) {
        unused()
    }

    override suspend fun adjustStock(productId: Long, delta: Double) {
        unused()
    }

    override suspend fun deleteProductsBySkuPattern(skuPattern: String): Int = unused()
}

private fun unused(): Nothing = error("fake repository dependency must not be used")
