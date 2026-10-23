package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.ReportRange
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReportContentLoaderTest {

    private val range = ReportRange(startMillis = 0L, endMillis = 10_000L)

    @Test
    fun aDeletedProductStaysInTheSummaryUnderItsSnapshotName() = runTest {
        val item = sampleTransactionItem(productId = 10L, qty = 3.0, subtotal = 45_000L)
            .copy(productNameSnapshot = "Kopi Susu Lama")

        val content = ReportContentLoader.load(
            range = range,
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction())),
            transactionRepository = FakeTransactionRepository(itemsByTransaction = mapOf(1L to listOf(item))),
            productRepository = FakeProductRepository(),
        )

        val summary = content.productSummaries.single()
        assertThat(summary.productId).isEqualTo(10L)
        assertThat(summary.name).isEqualTo("Kopi Susu Lama")
        assertThat(summary.quantitySold).isEqualTo(3.0)
        assertThat(summary.salesValue).isEqualTo(45_000L)
    }

    @Test
    fun aLiveProductUsesItsCurrentNameInsteadOfTheSnapshot() = runTest {
        val item = sampleTransactionItem(productId = 10L, qty = 3.0, subtotal = 45_000L)
            .copy(productNameSnapshot = "Kopi Susu Lama")

        val content = ReportContentLoader.load(
            range = range,
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction())),
            transactionRepository = FakeTransactionRepository(itemsByTransaction = mapOf(1L to listOf(item))),
            productRepository = FakeProductRepository(mapOf(10L to sampleProduct(id = 10L, name = "Kopi Susu Baru"))),
        )

        assertThat(content.productSummaries.single().name).isEqualTo("Kopi Susu Baru")
    }
}
