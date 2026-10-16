package id.umkm.pos.pro.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.mapper.ProductMapper
import id.umkm.pos.pro.data.local.mapper.TransactionMapper
import id.umkm.pos.pro.data.local.repository.ProductRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ReportRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ReturnRepositoryImpl
import id.umkm.pos.pro.data.local.repository.TransactionRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UmkmDatabaseInstrumentedTest {

    private lateinit var database: UmkmDatabase

    private val productRepository: ProductRepositoryImpl by lazy {
        ProductRepositoryImpl(database.productDao())
    }

    private val transactionRepository: TransactionRepositoryImpl by lazy {
        TransactionRepositoryImpl(
            database,
            database.transactionDao(),
            database.debtPaymentDao(),
            database.customerDao(),
        )
    }

    private val returnRepository: ReturnRepositoryImpl by lazy {
        ReturnRepositoryImpl(
            database,
            database.returnDao(),
            database.transactionDao(),
            database.productDao(),
        )
    }

    private val reportRepository: ReportRepositoryImpl by lazy {
        ReportRepositoryImpl(database.reportDao(), database.transactionDao())
    }

    @Before
    fun openInMemoryDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UmkmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun foreignKeyEnforcementIsActive() {
        assertTrue(isForeignKeyEnforcementEnabled())
    }

    @Test
    fun productPersistsAndRoundTripsThroughDaoAndRepository() = runBlocking {
        val product = newProduct(sku = "SKU-1", barcode = "8991001", name = "Kopi Bubuk")
        val productId = productRepository.upsertProduct(product)
        assertTrue(productId > 0L)

        assertEquals(product.copy(id = productId), productRepository.getProductById(productId))
        assertEquals(productId, productRepository.findProductByBarcode("8991001")!!.id)
        assertEquals(listOf(productId), productRepository.observeProducts("Kopi").first().map { it.id })

        val updated = product.copy(
            id = productId,
            name = "Kopi Bubuk Premium",
            basePrice = 2_500L,
            stockBaseQty = 42.0,
            isActive = false,
        )
        assertEquals(productId, productRepository.upsertProduct(updated))
        assertEquals(updated, productRepository.getProductById(productId))

        val rawEntity = ProductMapper.toEntity(
            newProduct(sku = "SKU-RAW", barcode = "8991002", name = "Gula Pasir"),
        )
        val rawEntityId = database.productDao().insertProduct(rawEntity)
        assertEquals(rawEntity.copy(id = rawEntityId), database.productDao().getProductById(rawEntityId))
    }

    @Test
    fun productUnitRoundTripsAndCascadesWhenProductDeleted() = runBlocking {
        val productId = productRepository.upsertProduct(
            newProduct(sku = "SKU-CASCADE", barcode = "8992001", name = "Susu Bubuk"),
        )
        val unitId = productRepository.upsertUnit(newUnit(productId, variantName = "Dus", conversionFactor = 40.0))
        assertTrue(unitId > 0L)

        val unit = productRepository.getUnits(productId).single()
        assertEquals(unitId, unit.id)
        assertEquals(productId, unit.productId)
        assertEquals("Dus", unit.variantName)
        assertEquals(40.0, unit.conversionFactor, EXACT_DOUBLE)
        assertEquals(78_000L, unit.unitPrice)
        assertEquals(5, unit.wholesaleMinQty)
        assertEquals(75_000L, unit.wholesalePrice)
        assertEquals(1, rowCount("product_variants_units"))

        productRepository.deleteProduct(productId)

        assertNull(productRepository.getProductById(productId))
        assertTrue(productRepository.getUnits(productId).isEmpty())
        assertEquals(0, rowCount("product_variants_units"))
    }

    @Test
    fun transactionItemsCascadeWhenTransactionDeleted() = runBlocking {
        val productId = productRepository.upsertProduct(
            newProduct(sku = "SKU-TX", barcode = "8993001", name = "Teh Kotak"),
        )
        val saved = transactionRepository.saveTransaction(
            newTransaction("INV-CASCADE"),
            listOf(newItem(productId, 2.0), newItem(productId, 1.0)),
        )
        val transactionId = (saved as AppResult.Success<Long>).value
        assertEquals(2, transactionRepository.getItems(transactionId).size)

        deleteTransactionRow(transactionId)

        assertNull(transactionRepository.getTransaction(transactionId))
        assertTrue(transactionRepository.getItems(transactionId).isEmpty())
        assertEquals(0, rowCount("transaction_items"))
    }

    @Test
    fun emptyDatabaseReadsReturnEmptyAndZero() = runBlocking {
        assertNull(productRepository.getProductById(1L))
        assertNull(productRepository.findProductByBarcode("missing"))
        assertTrue(productRepository.getUnits(1L).isEmpty())
        assertTrue(productRepository.observeProducts(null).first().isEmpty())

        assertNull(transactionRepository.getTransaction(1L))
        assertTrue(transactionRepository.getItems(1L).isEmpty())

        assertTrue(returnRepository.getReturns(1L).isEmpty())
        assertTrue(returnRepository.returnsInRange(FULL_RANGE).isEmpty())
        assertTrue(reportRepository.transactionsInRange(FULL_RANGE).isEmpty())
        assertTrue(reportRepository.salesByDay(FULL_RANGE).isEmpty())

        val summary = reportRepository.salesSummary(FULL_RANGE)
        assertEquals(0L, summary.totalSales)
        assertEquals(0L, summary.totalHpp)
        assertEquals(0L, summary.netProfit)
        assertEquals(0, summary.transactionCount)
    }

    @Test
    fun duplicateInvoiceNoThroughRepositoryPersistsNothingExtra() = runBlocking {
        val productId = productRepository.upsertProduct(
            newProduct(sku = "SKU-DUP", barcode = "8994001", name = "Minyak Goreng"),
        )

        val first = transactionRepository.saveTransaction(
            newTransaction("INV-DUP"),
            listOf(newItem(productId, 1.0)),
        )
        assertTrue(first is AppResult.Success)
        val firstId = (first as AppResult.Success<Long>).value
        assertEquals(1, rowCount("transactions"))
        assertEquals(1, rowCount("transaction_items"))

        val duplicate = transactionRepository.saveTransaction(
            newTransaction("INV-DUP"),
            listOf(newItem(productId, 2.0), newItem(productId, 3.0)),
        )

        assertTrue(duplicate is AppResult.Failure)
        assertTrue((duplicate as AppResult.Failure).error is AppError.Storage)
        assertEquals(1, rowCount("transactions"))
        assertEquals(1, rowCount("transaction_items"))
        assertEquals(1, transactionRepository.getItems(firstId).size)
    }

    @Test
    fun multiStepWriteRollsBackWhenUniqueInvoiceViolated() = runBlocking {
        val productId = productRepository.upsertProduct(
            newProduct(sku = "SKU-ROLLBACK", barcode = "8995001", name = "Beras"),
        )
        val existingId = database.transactionDao().insertTransactionWithItems(
            TransactionMapper.toEntity(newTransaction("INV-KEEP")),
            listOf(TransactionMapper.toEntity(newItem(productId, 1.0))),
        )
        val itemsBeforeFailure = rowCount("transaction_items")
        assertEquals(1, itemsBeforeFailure)

        val outcome = runCatching {
            database.withTransaction {
                database.transactionDao().insertItems(
                    listOf(
                        TransactionMapper.toEntity(newItem(productId, 5.0)).copy(transactionId = existingId),
                        TransactionMapper.toEntity(newItem(productId, 6.0)).copy(transactionId = existingId),
                    ),
                )
                database.transactionDao().insertTransaction(
                    TransactionMapper.toEntity(newTransaction("INV-KEEP")),
                )
            }
        }

        assertTrue(outcome.isFailure)
        assertEquals(1, rowCount("transactions"))
        assertEquals(itemsBeforeFailure, rowCount("transaction_items"))
    }

    private fun newProduct(
        sku: String,
        barcode: String?,
        name: String,
        stockBaseQty: Double = 100.0,
    ): Product = Product(
        id = 0L,
        sku = sku,
        barcode = barcode,
        name = name,
        category = null,
        baseUnit = "pcs",
        baseHpp = 1_000L,
        basePrice = 2_000L,
        stockBaseQty = stockBaseQty,
        minStockAlert = 5.0,
        isActive = true,
    )

    private fun newUnit(productId: Long, variantName: String, conversionFactor: Double): ProductUnit = ProductUnit(
        id = 0L,
        productId = productId,
        variantName = variantName,
        conversionFactor = conversionFactor,
        customBarcode = null,
        unitPrice = 78_000L,
        wholesaleMinQty = 5,
        wholesalePrice = 75_000L,
    )

    private fun newTransaction(invoiceNo: String): Transaction = Transaction(
        id = 0L,
        invoiceNo = invoiceNo,
        createdAt = 1_000L,
        cashierId = null,
        customerId = null,
        subtotal = 6_000L,
        discountTotal = 0L,
        invoiceDiscount = 0L,
        taxTotal = 0L,
        rounding = 0L,
        grandTotal = 6_000L,
        totalHpp = 3_000L,
        netProfit = 3_000L,
        paymentMethod = PaymentMethod.CASH,
        paidAmount = 6_000L,
        changeAmount = 0L,
        remainingDebt = 0L,
        dueDate = null,
        status = TransactionStatus.COMPLETED,
        notes = null,
    )

    private fun newItem(productId: Long, qty: Double): TransactionItem = TransactionItem(
        id = 0L,
        transactionId = 0L,
        productId = productId,
        variantName = null,
        qty = qty,
        unitHpp = 1_000L,
        appliedUnitPrice = 2_000L,
        isNegoPrice = false,
        isWholesaleApplied = false,
        itemDiscount = 0L,
        subtotal = (qty * 2_000.0).toLong(),
    )

    private fun rowCount(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun isForeignKeyEnforcementEnabled(): Boolean =
        database.openHelper.readableDatabase.query("PRAGMA foreign_keys").use { cursor ->
            cursor.moveToFirst() && cursor.getInt(0) == 1
        }

    private fun deleteTransactionRow(transactionId: Long) {
        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM transactions WHERE id = ?",
            arrayOf(transactionId),
        )
    }

    private companion object {
        const val EXACT_DOUBLE = 0.0
        val FULL_RANGE = ReportRange(0L, Long.MAX_VALUE)
    }
}
