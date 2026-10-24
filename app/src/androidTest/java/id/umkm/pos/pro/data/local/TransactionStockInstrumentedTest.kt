package id.umkm.pos.pro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.repository.ProductRepositoryImpl
import id.umkm.pos.pro.data.local.repository.TransactionRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class TransactionStockInstrumentedTest {

    private lateinit var database: UmkmDatabase
    private lateinit var productRepository: ProductRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl

    @Before
    fun openInMemoryDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UmkmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productRepository = ProductRepositoryImpl(database.productDao())
        transactionRepository = TransactionRepositoryImpl(
            database = database,
            transactionDao = database.transactionDao(),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun sameMillisecondInvoicesAreDistinctAndDailySequenced() = runBlocking {
        val createdAtMillis = SAMPLE_MILLIS
        val expectedDay = SimpleDateFormat(DAY_KEY_PATTERN, Locale.US).format(Date(createdAtMillis))

        val invoices = (1..6).map { transactionRepository.nextInvoiceNumber(createdAtMillis) }

        assertEquals("INV-$expectedDay-0001", invoices.first())
        assertEquals("INV-$expectedDay-0006", invoices.last())
        assertEquals(invoices.size, invoices.toSet().size)
    }

    @Test
    fun concurrentSameMillisecondInvoicesRemainUnique() = runBlocking {
        val invoices = (1..16)
            .map { async(Dispatchers.IO) { transactionRepository.nextInvoiceNumber(SAMPLE_MILLIS) } }
            .awaitAll()

        assertEquals(16, invoices.toSet().size)
        assertTrue(invoices.all { it.startsWith("INV-") })
    }

    @Test
    fun invalidConversionFactorIsRejectedBeforeAnyWrite() = runBlocking {
        val productId = productRepository.upsertProduct(newProduct(stockBaseQty = 50.0))
        val invalidFactors = listOf(0.0, -2.0, Double.NaN, Double.POSITIVE_INFINITY)

        for ((index, factor) in invalidFactors.withIndex()) {
            val outcome = transactionRepository.saveTransaction(
                newTransaction("INV-BAD-$index"),
                listOf(newItem(productId, qty = 2.0, conversionFactor = factor)),
            )
            assertTrue(outcome is AppResult.Failure)
            assertTrue((outcome as AppResult.Failure).error is AppError.Validation)
        }

        assertEquals(50.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)
        assertEquals(0, rowCount("transactions"))
        assertEquals(0, rowCount("transaction_items"))
    }

    @Test
    fun nullConversionFactorLeavesStockUnchanged() = runBlocking {
        val productId = productRepository.upsertProduct(newProduct(stockBaseQty = 30.0))

        val outcome = transactionRepository.saveTransaction(
            newTransaction("INV-UNKNOWN"),
            listOf(newItem(productId, qty = 4.0, conversionFactor = null)),
        )

        assertTrue(outcome is AppResult.Success)
        assertEquals(30.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)
    }

    @Test
    fun singleUnitSaleDecrementsStockByBaseQuantity() = runBlocking {
        val productId = productRepository.upsertProduct(newProduct(stockBaseQty = 100.0))

        transactionRepository.saveTransaction(
            newTransaction("INV-SINGLE"),
            listOf(newItem(productId, qty = 3.0, conversionFactor = 1.0)),
        )

        assertEquals(97.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)
    }

    @Test
    fun wholesaleUnitSaleDecrementsStockByConversionFactor() = runBlocking {
        val productId = productRepository.upsertProduct(newProduct(stockBaseQty = 200.0))

        transactionRepository.saveTransaction(
            newTransaction("INV-WHOLESALE"),
            listOf(newItem(productId, qty = 5.0, conversionFactor = 40.0)),
        )

        assertEquals(0.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)
    }

    @Test
    fun repeatedSalesAccumulateStockDecrements() = runBlocking {
        val productId = productRepository.upsertProduct(newProduct(stockBaseQty = 100.0))

        transactionRepository.saveTransaction(
            newTransaction("INV-REPEAT-1"),
            listOf(newItem(productId, qty = 2.0, conversionFactor = 1.0)),
        )
        transactionRepository.saveTransaction(
            newTransaction("INV-REPEAT-2"),
            listOf(newItem(productId, qty = 3.0, conversionFactor = 1.0)),
        )
        transactionRepository.saveTransaction(
            newTransaction("INV-REPEAT-3"),
            listOf(newItem(productId, qty = 1.0, conversionFactor = 40.0)),
        )

        assertEquals(55.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)
    }

    @Test
    fun saleAllowsNegativeStockWithoutClamping() = runBlocking {
        val productId = productRepository.upsertProduct(newProduct(stockBaseQty = 10.0))

        transactionRepository.saveTransaction(
            newTransaction("INV-NEGATIVE"),
            listOf(newItem(productId, qty = 15.0, conversionFactor = 1.0)),
        )

        assertEquals(-5.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)
    }

    @Test
    fun failedSaleLeavesStockAndRowsUnchanged() = runBlocking {
        val productId = productRepository.upsertProduct(newProduct(stockBaseQty = 100.0))
        transactionRepository.saveTransaction(
            newTransaction("INV-DUPLICATE"),
            listOf(newItem(productId, qty = 3.0, conversionFactor = 1.0)),
        )
        assertEquals(97.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)

        val duplicate = transactionRepository.saveTransaction(
            newTransaction("INV-DUPLICATE"),
            listOf(newItem(productId, qty = 10.0, conversionFactor = 1.0)),
        )

        assertTrue(duplicate is AppResult.Failure)
        assertTrue((duplicate as AppResult.Failure).error is AppError.Storage)
        assertEquals(97.0, productRepository.getProductById(productId)!!.stockBaseQty, DELTA)
        assertEquals(1, rowCount("transactions"))
        assertEquals(1, rowCount("transaction_items"))
    }

    private fun newProduct(stockBaseQty: Double): Product = Product(
        id = 0L,
        sku = "SKU-STOCK",
        barcode = null,
        name = "Kopi Bubuk",
        category = null,
        baseUnit = "pcs",
        baseHpp = 1_000L,
        basePrice = 2_000L,
        stockBaseQty = stockBaseQty,
        minStockAlert = 5.0,
        isActive = true,
    )

    private fun newTransaction(invoiceNo: String): Transaction = Transaction(
        id = 0L,
        invoiceNo = invoiceNo,
        createdAt = SAMPLE_MILLIS,
        cashierId = null,
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
        status = TransactionStatus.COMPLETED,
        notes = null,
    )

    private fun newItem(
        productId: Long,
        qty: Double,
        conversionFactor: Double?,
    ): TransactionItem = TransactionItem(
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
        conversionFactor = conversionFactor,
    )

    private fun rowCount(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val SAMPLE_MILLIS = 1_700_000_000_000L
        const val DAY_KEY_PATTERN = "yyyyMMdd"
        const val DELTA = 1e-9
    }
}
