package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.ReportRange

internal object ReportContentLoader {

    suspend fun load(
        range: ReportRange,
        reportRepository: ReportRepository,
        transactionRepository: TransactionRepository,
        productRepository: ProductRepository,
    ): ReportContent {
        val transactions = reportRepository.transactionsInRange(range)
        val quantityByProduct = LinkedHashMap<Long, Double>()
        val salesValueByProduct = LinkedHashMap<Long, Long>()
        transactions.forEach { transaction ->
            transactionRepository.getItems(transaction.id).forEach { item ->
                quantityByProduct[item.productId] =
                    (quantityByProduct[item.productId] ?: 0.0) + item.qty
                salesValueByProduct[item.productId] =
                    (salesValueByProduct[item.productId] ?: 0L) + item.subtotal
            }
        }
        val summaries = quantityByProduct.entries
            .mapNotNull { entry ->
                productRepository.getProductById(entry.key)?.let { product ->
                    ReportProductSummary(
                        productId = product.id,
                        name = product.name,
                        quantitySold = entry.value,
                        salesValue = salesValueByProduct[entry.key] ?: 0L,
                    )
                }
            }
            .sortedByDescending { summary -> summary.salesValue }
        return ReportContent(transactions = transactions, productSummaries = summaries)
    }
}
