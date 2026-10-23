package id.umkm.pos.pro.feature.receipt

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.feature.reports.FakeCustomerRepository
import id.umkm.pos.pro.feature.reports.FakeProductRepository
import id.umkm.pos.pro.feature.reports.FakeSettingsRepository
import id.umkm.pos.pro.feature.reports.FakeTransactionRepository
import id.umkm.pos.pro.feature.reports.sampleProduct
import id.umkm.pos.pro.feature.reports.sampleTransaction
import id.umkm.pos.pro.feature.reports.sampleTransactionItem
import id.umkm.pos.pro.ui.common.IdStrings
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StoredReceiptBuilderTest {

    @Test
    fun aDeletedProductsReceiptLineUsesTheSnapshotNameAndUnitLabel() = runTest {
        val item = sampleTransactionItem(transactionId = 1L, productId = 10L)
            .copy(productNameSnapshot = "Kopi Susu Lama", unitLabelSnapshot = "dus")

        val receipt = requireNotNull(
            StoredReceiptLoader.load(
                transactionId = 1L,
                transactionRepository = FakeTransactionRepository(
                    itemsByTransaction = mapOf(1L to listOf(item)),
                    transactionsById = mapOf(1L to sampleTransaction()),
                ),
                productRepository = FakeProductRepository(),
                customerRepository = FakeCustomerRepository(),
                settingsRepository = FakeSettingsRepository(),
                strings = IdStrings,
            ),
        )

        val line = receipt.shareData.items.single()
        assertThat(line.name).isEqualTo("Kopi Susu Lama")
        assertThat(line.quantityLabel).isEqualTo("3 dus")
        assertThat(StoredReceiptBuilder.toPrintText(receipt)).contains("3 dus x Kopi Susu Lama")
    }

    @Test
    fun aLiveProductWithoutASnapshotFallsBackToTheCurrentProduct() = runTest {
        val item = sampleTransactionItem(transactionId = 1L, productId = 10L)

        val receipt = requireNotNull(
            StoredReceiptLoader.load(
                transactionId = 1L,
                transactionRepository = FakeTransactionRepository(
                    itemsByTransaction = mapOf(1L to listOf(item)),
                    transactionsById = mapOf(1L to sampleTransaction()),
                ),
                productRepository = FakeProductRepository(
                    mapOf(10L to sampleProduct(id = 10L, name = "Kopi Susu")),
                ),
                customerRepository = FakeCustomerRepository(),
                settingsRepository = FakeSettingsRepository(),
                strings = IdStrings,
            ),
        )

        val line = receipt.shareData.items.single()
        assertThat(line.name).isEqualTo("Kopi Susu")
        assertThat(line.quantityLabel).isEqualTo("3 pcs")
    }
}
