package id.umkm.pos.pro.feature.receipt

import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.formatDateTime

internal object StoredReceiptLoader {

    suspend fun load(
        transactionId: Long,
        transactionRepository: TransactionRepository,
        productRepository: ProductRepository,
        settingsRepository: SettingsRepository,
        strings: PosStrings,
    ): StoredReceipt? {
        val transaction = transactionRepository.getTransaction(transactionId) ?: return null
        val items = transactionRepository.getItems(transactionId)
        val productsById = LinkedHashMap<Long, Product>()
        items.map { item -> item.productId }.distinct().forEach { productId ->
            productRepository.getProductById(productId)?.let { product -> productsById[productId] = product }
        }
        val settings = settingsRepository.getSettings()
        return StoredReceiptBuilder.build(
            transaction = transaction,
            items = items,
            productsById = productsById,
            storeName = settings.storeName,
            cashierName = transaction.cashierName ?: settings.cashierName,
            createdAtLabel = formatDateTime(transaction.createdAt, strings),
            strings = strings,
        )
    }
}
