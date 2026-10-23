package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransactionDetailLoaderTest {

    @Test
    fun aDeletedProductsLineUsesTheSnapshotNameAndUnitLabel() = runTest {
        val item = sampleTransactionItem(transactionId = 1L, productId = 10L)
            .copy(productNameSnapshot = "Kopi Susu Lama", unitLabelSnapshot = "dus")

        val detail = TransactionDetailLoader.load(
            transactionId = 1L,
            transactionRepository = FakeTransactionRepository(
                itemsByTransaction = mapOf(1L to listOf(item)),
                transactionsById = mapOf(1L to sampleTransaction()),
            ),
            productRepository = FakeProductRepository(),
            customerRepository = FakeCustomerRepository(),
            settingsRepository = FakeSettingsRepository(),
        )

        val line = requireNotNull(detail).lines.single()
        assertThat(line.name).isEqualTo("Kopi Susu Lama")
        assertThat(line.quantityLabel).isEqualTo("3 dus")
    }

    @Test
    fun aLiveProductWithoutASnapshotFallsBackToTheCurrentProduct() = runTest {
        val item = sampleTransactionItem(transactionId = 1L, productId = 10L)

        val detail = TransactionDetailLoader.load(
            transactionId = 1L,
            transactionRepository = FakeTransactionRepository(
                itemsByTransaction = mapOf(1L to listOf(item)),
                transactionsById = mapOf(1L to sampleTransaction()),
            ),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct(id = 10L, name = "Kopi Susu"))),
            customerRepository = FakeCustomerRepository(),
            settingsRepository = FakeSettingsRepository(),
        )

        val line = requireNotNull(detail).lines.single()
        assertThat(line.name).isEqualTo("Kopi Susu")
        assertThat(line.quantityLabel).isEqualTo("3 pcs")
    }
}
