package id.umkm.pos.pro.feature.receipt

import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.common.formatDateTime

internal object StoredReceiptLoader {

    suspend fun load(
        transactionId: Long,
        transactionRepository: TransactionRepository,
        productRepository: ProductRepository,
        customerRepository: CustomerRepository,
        settingsRepository: SettingsRepository,
    ): StoredReceipt? {
        val transaction = transactionRepository.getTransaction(transactionId) ?: return null
        val items = transactionRepository.getItems(transactionId)
        val productsById = LinkedHashMap<Long, Product>()
        items.map { item -> item.productId }.distinct().forEach { productId ->
            productRepository.getProductById(productId)?.let { product -> productsById[productId] = product }
        }
        val settings = settingsRepository.getSettings()
        val customerName = transaction.customerId?.let { customerId ->
            customerRepository.getCustomer(customerId)?.name
        }
        return StoredReceiptBuilder.build(
            transaction = transaction,
            items = items,
            productsById = productsById,
            storeName = settings.storeName,
            cashierName = settings.cashierName,
            customerName = customerName,
            createdAtLabel = formatDateTime(transaction.createdAt),
            dueDateLabel = transaction.dueDate?.let { dueDate -> formatDate(dueDate) },
        )
    }
}
