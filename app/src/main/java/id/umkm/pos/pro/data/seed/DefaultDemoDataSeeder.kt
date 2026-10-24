package id.umkm.pos.pro.data.seed

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.PricingContext
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.domain.pos.ReturnLineRequest
import id.umkm.pos.pro.domain.usecase.CheckoutLine
import id.umkm.pos.pro.domain.usecase.CheckoutRequest
import id.umkm.pos.pro.domain.usecase.CheckoutUseCase
import id.umkm.pos.pro.domain.usecase.ProcessReturnUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

class DefaultDemoDataSeeder(
    private val database: UmkmDatabase,
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val returnRepository: ReturnRepository,
    private val priceEngine: PriceEngine,
    private val anchorClock: Clock,
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DemoDataSeeder {

    override suspend fun load(): AppResult<DemoSeedSummary> = withContext(dispatcher) {
        try {
            AppResult.Success(seed())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppResult.Failure(AppError.Storage(error.message ?: LOAD_FAILURE_MESSAGE))
        }
    }

    override suspend fun clear(): AppResult<DemoClearSummary> = withContext(dispatcher) {
        try {
            AppResult.Success(clearMarkedRecords())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppResult.Failure(AppError.Storage(error.message ?: CLEAR_FAILURE_MESSAGE))
        }
    }

    private suspend fun seed(): DemoSeedSummary {
        clearMarkedRecords()

        val anchorDayStart = DemoDayStart.localDayStartMillis(anchorClock.nowMillis(), timeZone)
        val demoClock = DemoClock(anchorDayStart)
        val sales = DemoSalesPlan.build()
        val netBaseQty = DemoStockPlan.netBaseQtyByProduct(sales)
        val catalog = insertCatalog(netBaseQty)
        val checkout = CheckoutUseCase(productRepository, transactionRepository, priceEngine, demoClock)

        val transactionIds = ArrayList<Long>(sales.size)
        sales.forEachIndexed { index, sale ->
            demoClock.set(DemoSchedule.saleMillis(anchorDayStart, sale))
            val request = buildCheckoutRequest(sale, catalog)
            when (val result = checkout(request)) {
                is AppResult.Success -> transactionIds += result.value.transactionId
                is AppResult.Failure -> throw DemoSeedFailure("transaction $index: ${result.error.message}")
            }
        }

        val returns = recordReturn(sales, transactionIds, anchorDayStart, demoClock)

        return DemoSeedSummary(
            products = DemoCatalog.products.size,
            transactions = transactionIds.size,
            returns = returns,
        )
    }

    private suspend fun clearMarkedRecords(): DemoClearSummary {
        val transactions = database.transactionDao().deleteTransactionsByNote(DemoMarkers.NOTE) +
            database.transactionDao().deleteTransactionsByNote(DemoMarkers.LEGACY_NOTE)
        val products = database.productDao().deleteProductsBySkuPattern(DemoMarkers.skuLikePattern()) +
            database.productDao().deleteProductsBySkuPattern(DemoMarkers.legacySkuLikePattern())
        return DemoClearSummary(
            products = products,
            transactions = transactions,
        )
    }

    private suspend fun insertCatalog(netBaseQty: List<Double>): SeededCatalog {
        val productIds = ArrayList<Long>(DemoCatalog.products.size)
        val unitIdsByProduct = ArrayList<List<Long>>(DemoCatalog.products.size)
        val productsById = LinkedHashMap<Long, Product>()
        val unitsByProduct = LinkedHashMap<Long, List<ProductUnit>>()

        DemoCatalog.products.forEachIndexed { index, spec ->
            val product = Product(
                id = 0L,
                sku = spec.sku,
                barcode = spec.barcode,
                name = spec.name,
                category = DemoMarkers.categoryFor(spec.category),
                baseUnit = spec.baseUnit,
                baseHpp = spec.baseHpp,
                basePrice = spec.basePrice,
                stockBaseQty = DemoStockPlan.initialStock(spec, netBaseQty[index]),
                minStockAlert = spec.minStockAlert,
                isActive = true,
            )
            val productId = productRepository.upsertProduct(product)
            productIds += productId
            productsById[productId] = product.copy(id = productId)

            val units = spec.units.map { unitSpec ->
                val unit = ProductUnit(
                    id = 0L,
                    productId = productId,
                    variantName = unitSpec.variantName,
                    conversionFactor = unitSpec.conversionFactor,
                    customBarcode = unitSpec.customBarcode,
                    unitPrice = unitSpec.unitPrice,
                    wholesaleMinQty = unitSpec.wholesaleMinQty,
                    wholesalePrice = unitSpec.wholesalePrice,
                )
                val unitId = productRepository.upsertUnit(unit)
                unit.copy(id = unitId)
            }
            unitIdsByProduct += units.map { it.id }
            unitsByProduct[productId] = units
        }

        return SeededCatalog(productIds, unitIdsByProduct, productsById, unitsByProduct)
    }

    private fun buildCheckoutRequest(
        sale: DemoSaleSpec,
        catalog: SeededCatalog,
    ): CheckoutRequest {
        val pricedGrandTotal = priceEngine.priceCart(
            lines = toCartLines(sale, catalog),
            products = catalog.productsById,
            unitsByProduct = catalog.unitsByProduct,
            context = PricingContext(invoiceDiscount = sale.invoiceDiscount),
        ).grandTotal

        val paidAmount = when (sale.settlement) {
            DemoSettlement.FULL -> pricedGrandTotal
            DemoSettlement.CASH_ROUNDED -> roundUpTo(pricedGrandTotal, CASH_ROUNDING_INCREMENT)
        }

        return CheckoutRequest(
            lines = sale.lines.map { line ->
                CheckoutLine(
                    productId = catalog.productIds[line.productIndex],
                    unitId = line.unitIndex?.let { unitIndex -> catalog.unitIds[line.productIndex][unitIndex] },
                    qty = line.qty,
                    negotiatedUnitPrice = line.negotiatedUnitPrice,
                    itemDiscount = line.itemDiscount,
                )
            },
            paymentMethod = sale.paymentMethod,
            paidAmount = paidAmount,
            invoiceDiscount = sale.invoiceDiscount,
            taxRatePercent = 0,
            roundingIncrement = 0L,
            notes = DemoMarkers.NOTE,
        )
    }

    private fun toCartLines(sale: DemoSaleSpec, catalog: SeededCatalog): List<CartLine> =
        sale.lines.mapIndexed { index, line ->
            CartLine(
                lineId = "L${index + 1}",
                productId = catalog.productIds[line.productIndex],
                unitId = line.unitIndex?.let { unitIndex -> catalog.unitIds[line.productIndex][unitIndex] },
                variantName = line.unitIndex?.let { unitIndex ->
                    DemoCatalog.products[line.productIndex].units[unitIndex].variantName
                },
                qty = line.qty,
                negotiatedUnitPrice = line.negotiatedUnitPrice,
                itemDiscount = line.itemDiscount,
            )
        }

    private suspend fun recordReturn(
        sales: List<DemoSaleSpec>,
        transactionIds: List<Long>,
        anchorDayStart: Long,
        demoClock: DemoClock,
    ): Int {
        val transactionId = transactionIds.getOrNull(DemoSalesPlan.RETURN_SALE_INDEX) ?: return 0
        val items = transactionRepository.getItems(transactionId)
        val item = items.getOrNull(DemoSalesPlan.RETURN_LINE_INDEX) ?: return 0
        val returnedQty = DemoSalesPlan.RETURN_QTY.coerceAtMost(item.qty)
        if (returnedQty <= 0.0) return 0

        demoClock.set(
            DemoSchedule.saleMillis(anchorDayStart, sales[DemoSalesPlan.RETURN_SALE_INDEX]) +
                DemoSchedule.MILLIS_PER_DAY,
        )
        val useCase = ProcessReturnUseCase(
            transactionRepository = transactionRepository,
            returnRepository = returnRepository,
            productRepository = productRepository,
            clock = demoClock,
        )
        return when (val result = useCase(transactionId, listOf(ReturnLineRequest(item.id, returnedQty)), DemoMarkers.NOTE)) {
            is AppResult.Success -> 1
            is AppResult.Failure -> throw DemoSeedFailure("return: ${result.error.message}")
        }
    }

    private fun roundUpTo(amount: Long, increment: Long): Long =
        if (amount <= 0L) 0L else ((amount + increment - 1) / increment) * increment

    private class DemoClock(private var currentMillis: Long) : Clock {
        fun set(millis: Long) {
            currentMillis = millis
        }

        override fun nowMillis(): Long = currentMillis
    }

    private class SeededCatalog(
        val productIds: List<Long>,
        val unitIds: List<List<Long>>,
        val productsById: Map<Long, Product>,
        val unitsByProduct: Map<Long, List<ProductUnit>>,
    )

    private class DemoSeedFailure(override val message: String) : Exception(message)

    private companion object {
        const val CASH_ROUNDING_INCREMENT = 5_000L
        const val LOAD_FAILURE_MESSAGE = "failed to load demo data"
        const val CLEAR_FAILURE_MESSAGE = "failed to clear demo data"
    }
}
