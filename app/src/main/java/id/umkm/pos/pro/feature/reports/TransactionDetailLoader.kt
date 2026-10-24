package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.ui.common.formatQuantity

internal object TransactionDetailLoader {

    suspend fun load(
        transactionId: Long,
        transactionRepository: TransactionRepository,
        productRepository: ProductRepository,
        settingsRepository: SettingsRepository,
    ): TransactionDetail? {
        val transaction = transactionRepository.getTransaction(transactionId) ?: return null
        val items = transactionRepository.getItems(transactionId)
        val lines = items.map { item ->
            val product = productRepository.getProductById(item.productId)
            val unitLabel = item.unitLabelSnapshot ?: item.variantName ?: product?.baseUnit.orEmpty()
            TransactionDetailLine(
                name = item.productNameSnapshot ?: product?.name ?: "Item ${item.productId}",
                quantityLabel = "${formatQuantity(item.qty)} $unitLabel".trim(),
                unitPrice = item.appliedUnitPrice,
                itemDiscount = item.itemDiscount,
                lineTotal = item.subtotal,
            )
        }
        return TransactionDetail(
            transaction = transaction,
            cashierName = settingsRepository.getSettings().cashierName,
            lines = lines,
        )
    }
}
