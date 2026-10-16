package id.umkm.pos.pro.domain.usecase

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckoutUseCaseTest {

    private val clock = FakeClock(5_000L)
    private val products = FakeProductRepository()
        .addProduct(sampleProduct(id = 1L))
        .addUnit(sampleDusUnit(id = 10L, productId = 1L))
    private val transactions = FakeTransactionRepository()
    private val useCase = CheckoutUseCase(products, transactions, DefaultPriceEngine(), clock)

    private fun request(
        lines: List<CheckoutLine>,
        customerId: Long? = null,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        paidAmount: Long = 0L,
        dueDate: Long? = null,
        invoiceDiscount: Long = 0L,
        taxRatePercent: Int = 0,
    ) = CheckoutRequest(
        lines = lines,
        customerId = customerId,
        paymentMethod = paymentMethod,
        paidAmount = paidAmount,
        dueDate = dueDate,
        invoiceDiscount = invoiceDiscount,
        taxRatePercent = taxRatePercent,
        roundingIncrement = 0L,
        notes = null,
    )

    @Test
    fun cashSaleComputesTotalsChangeAndProfit() = runTest {
        val result = useCase(
            request(
                lines = listOf(CheckoutLine(1L, null, 3.0, null, 0L)),
                paymentMethod = PaymentMethod.CASH,
                paidAmount = 10_000L,
            ),
        )

        val checkout = (result as AppResult.Success<CheckoutResult>).value
        assertThat(checkout.pricedCart.grandTotal).isEqualTo(6_000L)
        assertThat(checkout.pricedCart.totalHpp).isEqualTo(3_000L)
        assertThat(checkout.pricedCart.netProfit).isEqualTo(3_000L)
        assertThat(checkout.allocation.changeAmount).isEqualTo(4_000L)
        assertThat(checkout.allocation.status).isEqualTo(TransactionStatus.COMPLETED)

        val saved = transactions.savedTransactions.single()
        assertThat(saved.subtotal).isEqualTo(6_000L)
        assertThat(saved.grandTotal).isEqualTo(6_000L)
        assertThat(saved.paidAmount).isEqualTo(10_000L)
        assertThat(saved.changeAmount).isEqualTo(4_000L)
        assertThat(saved.remainingDebt).isEqualTo(0L)
        assertThat(saved.status).isEqualTo(TransactionStatus.COMPLETED)
        assertThat(saved.invoiceNo).startsWith("INV-TEST-")
        assertThat(transactions.itemsFor(checkout.transactionId).single().unitHpp).isEqualTo(1_000L)
    }

    @Test
    fun wholesaleTierAppliesForBulkUnitPurchase() = runTest {
        val result = useCase(
            request(
                lines = listOf(CheckoutLine(1L, 10L, 5.0, null, 0L)),
                paymentMethod = PaymentMethod.CASH,
                paidAmount = 375_000L,
            ),
        )

        val checkout = (result as AppResult.Success<CheckoutResult>).value
        assertThat(checkout.pricedCart.grandTotal).isEqualTo(375_000L)
        assertThat(checkout.pricedCart.totalHpp).isEqualTo(200_000L)
        assertThat(checkout.pricedCart.netProfit).isEqualTo(175_000L)
        assertThat(checkout.pricedCart.lines.single().isWholesale).isTrue()
    }

    @Test
    fun kasbonSaleCreatesPartialDebtWithDueDate() = runTest {
        val result = useCase(
            request(
                lines = listOf(CheckoutLine(1L, null, 3.0, null, 0L)),
                customerId = 7L,
                paymentMethod = PaymentMethod.KASBON,
                paidAmount = 1_000L,
                dueDate = 99_000L,
            ),
        )

        val checkout = (result as AppResult.Success<CheckoutResult>).value
        assertThat(checkout.allocation.remainingDebt).isEqualTo(5_000L)
        assertThat(checkout.allocation.status).isEqualTo(TransactionStatus.PARTIAL_DEBT)

        val saved = transactions.savedTransactions.single()
        assertThat(saved.customerId).isEqualTo(7L)
        assertThat(saved.remainingDebt).isEqualTo(5_000L)
        assertThat(saved.dueDate).isEqualTo(99_000L)
    }

    @Test
    fun discountAndTaxFeedTheStoredTotals() = runTest {
        val result = useCase(
            request(
                lines = listOf(CheckoutLine(1L, null, 3.0, null, 500L)),
                paymentMethod = PaymentMethod.CASH,
                paidAmount = 6_000L,
                invoiceDiscount = 500L,
                taxRatePercent = 10,
            ),
        )

        val checkout = (result as AppResult.Success<CheckoutResult>).value
        assertThat(checkout.pricedCart.subtotal).isEqualTo(5_500L)
        assertThat(checkout.pricedCart.taxTotal).isEqualTo(500L)
        assertThat(checkout.pricedCart.grandTotal).isEqualTo(5_500L)
        val saved = transactions.savedTransactions.single()
        assertThat(saved.discountTotal).isEqualTo(1_000L)
        assertThat(saved.invoiceDiscount).isEqualTo(500L)
        assertThat(saved.taxTotal).isEqualTo(500L)
    }

    @Test
    fun emptyCartFailsValidation() = runTest {
        val result = useCase(request(lines = emptyList()))

        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun unknownProductFailsNotFound() = runTest {
        val result = useCase(
            request(
                lines = listOf(CheckoutLine(999L, null, 1.0, null, 0L)),
                paymentMethod = PaymentMethod.CASH,
                paidAmount = 2_000L,
            ),
        )

        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.NotFound::class.java)
        assertThat(transactions.savedTransactions).isEmpty()
    }
}
