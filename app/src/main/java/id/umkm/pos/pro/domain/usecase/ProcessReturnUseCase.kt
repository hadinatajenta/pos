package id.umkm.pos.pro.domain.usecase

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ReturnLine
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.domain.pos.ReturnLineRequest
import id.umkm.pos.pro.domain.pos.correctReturn

class ProcessReturnUseCase(
    private val transactionRepository: TransactionRepository,
    private val returnRepository: ReturnRepository,
    private val productRepository: ProductRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        transactionId: Long,
        requests: List<ReturnLineRequest>,
        note: String?,
    ): AppResult<ReturnRecord> {
        if (requests.isEmpty()) {
            return AppResult.Failure(AppError.Validation("return requires at least one line"))
        }

        val transaction = transactionRepository.getTransaction(transactionId)
            ?: return AppResult.Failure(AppError.NotFound("transaction $transactionId was not found"))
        val items = transactionRepository.getItems(transactionId)
        val productsById = loadProducts(items.map { it.productId }.distinct())

        return when (val correction = correctReturn(items, requests, productsById)) {
            is AppResult.Failure -> correction
            is AppResult.Success -> {
                val record = ReturnRecord(
                    id = 0L,
                    transactionId = transaction.id,
                    createdAt = clock.nowMillis(),
                    refundAmount = correction.value.totalRefund,
                    totalHppReversed = correction.value.totalHppReversed,
                    note = note,
                )
                val lines = correction.value.lines.mapIndexed { index, line ->
                    ReturnLine(
                        id = 0L,
                        returnId = 0L,
                        transactionItemId = line.transactionItemId,
                        productId = line.productId,
                        variantName = line.variantName,
                        qty = line.returnedQty,
                        conversionFactor = requests[index].conversionFactor,
                        refundAmount = line.refundAmount,
                        hppReversed = line.hppReversed,
                        baseQtyRestocked = line.baseQtyRestocked,
                    )
                }
                when (val saved = returnRepository.recordReturn(record, lines)) {
                    is AppResult.Failure -> saved
                    is AppResult.Success -> AppResult.Success(record.copy(id = saved.value))
                }
            }
        }
    }

    private suspend fun loadProducts(productIds: List<Long>): Map<Long, Product> {
        val products = LinkedHashMap<Long, Product>()
        for (productId in productIds) {
            productRepository.getProductById(productId)?.let { products[productId] = it }
        }
        return products
    }
}
