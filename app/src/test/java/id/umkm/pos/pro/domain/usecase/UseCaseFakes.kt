package id.umkm.pos.pro.domain.usecase

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.ReturnLine
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeClock(var currentMillis: Long = 1_000L) : Clock {
    override fun nowMillis(): Long = currentMillis
}

class FakeProductRepository : ProductRepository {
    private val products = LinkedHashMap<Long, Product>()
    private val units = LinkedHashMap<Long, MutableList<ProductUnit>>()

    fun addProduct(product: Product): FakeProductRepository {
        products[product.id] = product
        return this
    }

    fun addUnit(unit: ProductUnit): FakeProductRepository {
        units.getOrPut(unit.productId) { mutableListOf() }.add(unit)
        return this
    }

    override fun observeProducts(query: String?): Flow<List<Product>> = flowOf(products.values.toList())

    override suspend fun getProductById(productId: Long): Product? = products[productId]

    override suspend fun findProductByBarcode(barcode: String): Product? =
        products.values.firstOrNull { it.barcode == barcode }

    override suspend fun upsertProduct(product: Product): Long {
        products[product.id] = product
        return product.id
    }

    override suspend fun deleteProduct(productId: Long) {
        products.remove(productId)
    }

    override suspend fun getUnits(productId: Long): List<ProductUnit> = units[productId].orEmpty().toList()

    override suspend fun findUnitByBarcode(barcode: String): ProductUnit? =
        units.values.flatten().firstOrNull { it.customBarcode == barcode }

    override suspend fun upsertUnit(unit: ProductUnit): Long {
        units.getOrPut(unit.productId) { mutableListOf() }.add(unit)
        return unit.id
    }

    override suspend fun deleteUnit(unitId: Long) {
        units.values.forEach { list -> list.removeAll { it.id == unitId } }
    }
}

class FakeTransactionRepository : TransactionRepository {
    private val transactions = LinkedHashMap<Long, Transaction>()
    private val itemsByTransaction = LinkedHashMap<Long, List<TransactionItem>>()
    private var nextTransactionId = 1L
    private var nextItemId = 1_000L
    private var nextPaymentId = 1L

    val savedTransactions: List<Transaction> get() = transactions.values.toList()

    fun itemsFor(transactionId: Long): List<TransactionItem> = itemsByTransaction[transactionId].orEmpty()

    override suspend fun saveTransaction(
        transaction: Transaction,
        items: List<TransactionItem>,
    ): AppResult<Long> {
        if (items.isEmpty()) {
            return AppResult.Failure(AppError.Validation("transaction requires at least one item"))
        }
        val transactionId = nextTransactionId++
        transactions[transactionId] = transaction.copy(id = transactionId)
        itemsByTransaction[transactionId] =
            items.map { item -> item.copy(id = nextItemId++, transactionId = transactionId) }
        return AppResult.Success(transactionId)
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? = transactions[transactionId]

    override fun observeTransactions(range: ReportRange): Flow<List<Transaction>> = flowOf(transactions.values.toList())

    override suspend fun getItems(transactionId: Long): List<TransactionItem> = itemsByTransaction[transactionId].orEmpty()

    override suspend fun recordDebtPayment(payment: DebtPayment): AppResult<Long> {
        val existing = transactions[payment.transactionId]
            ?: return AppResult.Failure(AppError.NotFound("transaction ${payment.transactionId} was not found"))
        val remainingDebt = (existing.remainingDebt - payment.amount).coerceAtLeast(0L)
        transactions[payment.transactionId] = existing.copy(
            remainingDebt = remainingDebt,
            status = if (remainingDebt == 0L) TransactionStatus.COMPLETED else existing.status,
        )
        return AppResult.Success(nextPaymentId++)
    }

    override suspend fun markReturned(
        transactionId: Long,
        returnedItems: List<TransactionItem>,
    ): AppResult<Unit> {
        val existing = transactions[transactionId]
            ?: return AppResult.Failure(AppError.NotFound("transaction $transactionId was not found"))
        transactions[transactionId] = existing.copy(status = TransactionStatus.RETURNED)
        return AppResult.Success(Unit)
    }
}

data class RecordedReturn(val record: ReturnRecord, val lines: List<ReturnLine>)

class FakeReturnRepository(
    private val transactions: FakeTransactionRepository,
) : ReturnRepository {
    val recorded = mutableListOf<RecordedReturn>()
    private var nextId = 1L

    override suspend fun recordReturn(record: ReturnRecord, lines: List<ReturnLine>): AppResult<Long> {
        if (lines.isEmpty()) {
            return AppResult.Failure(AppError.Validation("return requires at least one line"))
        }
        val returnId = nextId++
        recorded += RecordedReturn(
            record = record.copy(id = returnId),
            lines = lines.map { line -> line.copy(returnId = returnId) },
        )
        transactions.markReturned(record.transactionId, emptyList())
        return AppResult.Success(returnId)
    }

    override suspend fun getReturns(transactionId: Long): List<ReturnRecord> =
        recorded.filter { it.record.transactionId == transactionId }.map { it.record }

    override suspend fun returnsInRange(range: ReportRange): List<ReturnRecord> = recorded.map { it.record }
}

fun sampleProduct(id: Long): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = null,
    name = "Product $id",
    category = null,
    baseUnit = "pcs",
    baseHpp = 1_000L,
    basePrice = 2_000L,
    stockBaseQty = 100.0,
    minStockAlert = 5.0,
    isActive = true,
)

fun sampleDusUnit(id: Long, productId: Long): ProductUnit = ProductUnit(
    id = id,
    productId = productId,
    variantName = "Dus",
    conversionFactor = 40.0,
    customBarcode = null,
    unitPrice = 78_000L,
    wholesaleMinQty = 5,
    wholesalePrice = 75_000L,
)
