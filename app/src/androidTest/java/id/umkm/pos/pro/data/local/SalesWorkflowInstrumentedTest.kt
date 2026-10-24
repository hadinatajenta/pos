package id.umkm.pos.pro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.repository.ProductRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ReportRepositoryImpl
import id.umkm.pos.pro.data.local.repository.ReturnRepositoryImpl
import id.umkm.pos.pro.data.local.repository.TransactionRepositoryImpl
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.domain.pos.ReturnLineRequest
import id.umkm.pos.pro.domain.usecase.CheckoutLine
import id.umkm.pos.pro.domain.usecase.CheckoutRequest
import id.umkm.pos.pro.domain.usecase.CheckoutResult
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.domain.usecase.ProcessReturnUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalesWorkflowInstrumentedTest {

    private lateinit var database: UmkmDatabase
    private lateinit var productRepository: ProductRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var returnRepository: ReturnRepositoryImpl
    private lateinit var reportRepository: ReportRepositoryImpl
    private lateinit var checkoutUseCase: CheckoutUseCase
    private lateinit var processReturnUseCase: ProcessReturnUseCase

    private val clock = FixedClock(FIXED_NOW_MILLIS)
    private val reportRange = ReportRange(0L, Long.MAX_VALUE)

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
        returnRepository = ReturnRepositoryImpl(
            database,
            database.returnDao(),
            database.transactionDao(),
            database.productDao(),
        )
        reportRepository = ReportRepositoryImpl(database.reportDao(), database.transactionDao())
        val priceEngine: PriceEngine = DefaultPriceEngine()
        checkoutUseCase = CheckoutUseCase(productRepository, transactionRepository, priceEngine, clock)
        processReturnUseCase =
            ProcessReturnUseCase(transactionRepository, returnRepository, productRepository, clock)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun twoSalesOnSameDayAggregateInSalesSummary() = runBlocking {
        val seeded = seedProduct(stockBaseQty = 500.0)

        val firstSale = checkoutUseCase(saleRequest(seeded.productId, unitId = null, qty = 3.0, paidAmount = 10_000L))
        assertTrue(firstSale is AppResult.Success)
        clock.currentMillis = FIXED_NOW_MILLIS + 1_000L
        val secondSale = checkoutUseCase(saleRequest(seeded.productId, unitId = seeded.dusUnitId, qty = 5.0, paidAmount = 375_000L))
        assertTrue(secondSale is AppResult.Success)

        val summary = reportRepository.salesSummary(reportRange)
        assertEquals(381_000L, summary.totalSales)
        assertEquals(203_000L, summary.totalHpp)
        assertEquals(178_000L, summary.netProfit)
        assertEquals(2, summary.transactionCount)
    }

    @Test
    fun cashSalePersistsTotalsChangeProfitAndStatus() = runBlocking {
        val seeded = seedProduct(stockBaseQty = 100.0)

        val result = checkoutUseCase(saleRequest(seeded.productId, unitId = null, qty = 3.0, paidAmount = 10_000L))
        val checkout = (result as AppResult.Success<CheckoutResult>).value

        assertEquals(6_000L, checkout.pricedCart.subtotal)
        assertEquals(6_000L, checkout.pricedCart.grandTotal)
        assertEquals(3_000L, checkout.pricedCart.totalHpp)
        assertEquals(3_000L, checkout.pricedCart.netProfit)
        assertEquals(4_000L, checkout.allocation.changeAmount)

        val saved = transactionRepository.getTransaction(checkout.transactionId)!!
        assertEquals(checkout.invoiceNo, saved.invoiceNo)
        assertTrue(saved.invoiceNo.matches(SINGLE_SALE_INVOICE_PATTERN))
        assertEquals(FIXED_NOW_MILLIS, saved.createdAt)
        assertEquals(6_000L, saved.subtotal)
        assertEquals(6_000L, saved.grandTotal)
        assertEquals(3_000L, saved.totalHpp)
        assertEquals(3_000L, saved.netProfit)
        assertEquals(10_000L, saved.paidAmount)
        assertEquals(4_000L, saved.changeAmount)
        assertEquals(TransactionStatus.COMPLETED, saved.status)

        val items = transactionRepository.getItems(checkout.transactionId)
        assertEquals(1, items.size)
        assertEquals(3.0, items.single().qty, DELTA)
        assertEquals(1_000L, items.single().unitHpp)
        assertEquals(2_000L, items.single().appliedUnitPrice)
        assertFalse(items.single().isWholesaleApplied)

        val summary = reportRepository.salesSummary(reportRange)
        assertEquals(6_000L, summary.totalSales)
        assertEquals(3_000L, summary.totalHpp)
        assertEquals(3_000L, summary.netProfit)
        assertEquals(1, summary.transactionCount)
    }

    @Test
    fun cashSaleDecrementsStockByBaseQuantitySold() = runBlocking {
        val seeded = seedProduct(stockBaseQty = 100.0)

        val result = checkoutUseCase(saleRequest(seeded.productId, unitId = null, qty = 3.0, paidAmount = 10_000L))
        assertTrue(result is AppResult.Success)

        val stockAfterSale = productRepository.getProductById(seeded.productId)!!.stockBaseQty
        assertEquals(97.0, stockAfterSale, DELTA)
    }

    @Test
    fun wholesaleBulkSaleAppliesTierPriceAndPersistsTotals() = runBlocking {
        val seeded = seedProduct(stockBaseQty = 200.0)

        val result = checkoutUseCase(saleRequest(seeded.productId, unitId = seeded.dusUnitId, qty = 5.0, paidAmount = 375_000L))
        val checkout = (result as AppResult.Success<CheckoutResult>).value

        assertEquals(375_000L, checkout.pricedCart.subtotal)
        assertEquals(375_000L, checkout.pricedCart.grandTotal)
        assertEquals(200_000L, checkout.pricedCart.totalHpp)
        assertEquals(175_000L, checkout.pricedCart.netProfit)
        assertTrue(checkout.pricedCart.lines.single().isWholesale)
        assertEquals(75_000L, checkout.pricedCart.lines.single().unitPrice)

        val saved = transactionRepository.getTransaction(checkout.transactionId)!!
        assertEquals(checkout.invoiceNo, saved.invoiceNo)
        assertTrue(saved.invoiceNo.matches(SINGLE_SALE_INVOICE_PATTERN))
        assertEquals(375_000L, saved.grandTotal)
        assertEquals(200_000L, saved.totalHpp)
        assertEquals(175_000L, saved.netProfit)
        assertEquals(TransactionStatus.COMPLETED, saved.status)

        val item = transactionRepository.getItems(checkout.transactionId).single()
        assertEquals(5.0, item.qty, DELTA)
        assertEquals(40_000L, item.unitHpp)
        assertEquals(75_000L, item.appliedUnitPrice)
        assertTrue(item.isWholesaleApplied)

        val summary = reportRepository.salesSummary(reportRange)
        assertEquals(375_000L, summary.totalSales)
        assertEquals(200_000L, summary.totalHpp)
        assertEquals(175_000L, summary.netProfit)
        assertEquals(1, summary.transactionCount)
    }

    @Test
    fun wholesaleBulkSaleDecrementsStockByBaseQuantitySold() = runBlocking {
        val seeded = seedProduct(stockBaseQty = 200.0)

        val result = checkoutUseCase(saleRequest(seeded.productId, unitId = seeded.dusUnitId, qty = 5.0, paidAmount = 375_000L))
        assertTrue(result is AppResult.Success)

        val stockAfterSale = productRepository.getProductById(seeded.productId)!!.stockBaseQty
        assertEquals(0.0, stockAfterSale, DELTA)
    }

    @Test
    fun returnRestocksRefundsReversesHppAndNetsSummary() = runBlocking {
        val seeded = seedProduct(stockBaseQty = 100.0)
        val sale = checkoutUseCase(saleRequest(seeded.productId, unitId = null, qty = 3.0, paidAmount = 10_000L))
        val transactionId = (sale as AppResult.Success<CheckoutResult>).value.transactionId
        val soldItem = transactionRepository.getItems(transactionId).single()
        val stockBeforeReturn = productRepository.getProductById(seeded.productId)!!.stockBaseQty

        val returnResult = processReturnUseCase(
            transactionId,
            listOf(ReturnLineRequest(soldItem.id, 1.0, 1.0)),
            "barang rusak",
        )
        val record = (returnResult as AppResult.Success<ReturnRecord>).value
        assertEquals(transactionId, record.transactionId)
        assertEquals(2_000L, record.refundAmount)
        assertEquals(1_000L, record.totalHppReversed)

        val storedRecord = returnRepository.getReturns(transactionId).single()
        val storedLine = database.returnDao().getLines(storedRecord.id).single()
        assertEquals(2_000L, storedRecord.refundAmount)
        assertEquals(1_000L, storedRecord.totalHppReversed)
        assertEquals(1.0, storedLine.qty, DELTA)
        assertEquals(1.0, storedLine.conversionFactor, DELTA)
        assertEquals(1.0, storedLine.baseQtyRestocked, DELTA)
        assertEquals(1_000L, storedLine.hppReversed)

        assertEquals(TransactionStatus.RETURNED, transactionRepository.getTransaction(transactionId)!!.status)

        val summaryAfterReturn = reportRepository.salesSummary(reportRange)
        assertEquals(4_000L, summaryAfterReturn.totalSales)
        assertEquals(2_000L, summaryAfterReturn.totalHpp)
        assertEquals(2_000L, summaryAfterReturn.netProfit)
        assertEquals(1, summaryAfterReturn.transactionCount)

        val stockAfterReturn = productRepository.getProductById(seeded.productId)!!.stockBaseQty
        assertEquals(1.0, stockAfterReturn - stockBeforeReturn, DELTA)
        assertEquals(98.0, stockAfterReturn, DELTA)
    }

    @Test
    fun returnRestocksByStoredConversionFactorWhenRequestOmitsIt() = runBlocking {
        val seeded = seedProduct(stockBaseQty = 200.0)
        val sale = checkoutUseCase(saleRequest(seeded.productId, unitId = seeded.dusUnitId, qty = 5.0, paidAmount = 375_000L))
        val transactionId = (sale as AppResult.Success<CheckoutResult>).value.transactionId
        val soldItem = transactionRepository.getItems(transactionId).single()
        assertEquals(40.0, soldItem.conversionFactor!!, DELTA)

        val returnResult = processReturnUseCase(
            transactionId,
            listOf(ReturnLineRequest(soldItem.id, 1.0, conversionFactor = null)),
            note = null,
        )
        val record = (returnResult as AppResult.Success<ReturnRecord>).value

        val storedLine = database.returnDao().getLines(record.id).single()
        assertEquals(1.0, storedLine.qty, DELTA)
        assertEquals(40.0, storedLine.conversionFactor, DELTA)
        assertEquals(40.0, storedLine.baseQtyRestocked, DELTA)
        assertEquals(40_000L, storedLine.hppReversed)

        val stockAfterReturn = productRepository.getProductById(seeded.productId)!!.stockBaseQty
        assertEquals(40.0, stockAfterReturn, DELTA)
    }

    private suspend fun seedProduct(stockBaseQty: Double): SeededProduct {
        val productId = productRepository.upsertProduct(
            Product(
                id = 0L,
                sku = "SKU-DUS",
                barcode = null,
                name = "Kopi Bubuk",
                category = null,
                baseUnit = "pcs",
                baseHpp = 1_000L,
                basePrice = 2_000L,
                stockBaseQty = stockBaseQty,
                minStockAlert = 5.0,
                isActive = true,
            ),
        )
        val dusUnitId = productRepository.upsertUnit(
            ProductUnit(
                id = 0L,
                productId = productId,
                variantName = "Dus",
                conversionFactor = 40.0,
                customBarcode = null,
                unitPrice = 78_000L,
                wholesaleMinQty = 5,
                wholesalePrice = 75_000L,
            ),
        )
        return SeededProduct(productId = productId, dusUnitId = dusUnitId)
    }

    private fun saleRequest(productId: Long, unitId: Long?, qty: Double, paidAmount: Long): CheckoutRequest =
        CheckoutRequest(
            lines = listOf(CheckoutLine(productId, unitId, qty, null, 0L)),
            paymentMethod = PaymentMethod.CASH,
            paidAmount = paidAmount,
            invoiceDiscount = 0L,
            taxRatePercent = 0,
            roundingIncrement = 0L,
            notes = null,
        )

    private data class SeededProduct(val productId: Long, val dusUnitId: Long)

    private companion object {
        const val FIXED_NOW_MILLIS = 5_000L
        const val DELTA = 1e-9
        val SINGLE_SALE_INVOICE_PATTERN = Regex("INV-\\d{8}-0001")
    }
}

private class FixedClock(var currentMillis: Long) : Clock {
    override fun nowMillis(): Long = currentMillis
}
