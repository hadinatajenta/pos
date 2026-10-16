package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import org.junit.Test

class ReturnCorrectionTest {

    @Test
    fun returnRestocksBaseQuantityUsingConversionFactor() {
        val items = listOf(sampleTransactionItem(id = 1L, qty = 4.0, subtotal = 20_000L))
        val products = mapOf(1L to sampleProduct(id = 1L, baseHpp = 1_000L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 2.0, conversionFactor = 10.0))

        val result = correctReturn(items, requests, products)

        val correction = (result as AppResult.Success<ReturnCorrection>).value.lines.single()
        assertThat(correction.baseQtyRestocked).isWithin(1e-9).of(20.0)
        assertThat(correction.hppReversed).isEqualTo(20_000L)
        assertThat(correction.refundAmount).isEqualTo(10_000L)
    }

    @Test
    fun returnReversesHppFlooredOnce() {
        val items = listOf(sampleTransactionItem(id = 1L, qty = 4.0, subtotal = 20_000L))
        val products = mapOf(1L to sampleProduct(id = 1L, baseHpp = 3L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 0.5, conversionFactor = 1.0))

        val result = correctReturn(items, requests, products)

        val correction = (result as AppResult.Success<ReturnCorrection>).value.lines.single()
        assertThat(correction.hppReversed).isEqualTo(1L)
    }

    @Test
    fun returnRefundsProportionalSubtotalWithDiscount() {
        val items = listOf(sampleTransactionItem(id = 1L, qty = 4.0, itemDiscount = 2_000L, subtotal = 18_000L))
        val products = mapOf(1L to sampleProduct(id = 1L, baseHpp = 1_000L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 1.0, conversionFactor = 1.0))

        val result = correctReturn(items, requests, products)

        val correction = (result as AppResult.Success<ReturnCorrection>).value.lines.single()
        assertThat(correction.refundAmount).isEqualTo(4_500L)
    }

    @Test
    fun returnCarriesProductAndVariantIdentity() {
        val items = listOf(sampleTransactionItem(id = 1L, productId = 3L, variantName = "bundle", qty = 2.0, subtotal = 10_000L))
        val products = mapOf(3L to sampleProduct(id = 3L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 1.0, conversionFactor = 1.0))

        val result = correctReturn(items, requests, products)

        val correction = (result as AppResult.Success<ReturnCorrection>).value.lines.single()
        assertThat(correction.productId).isEqualTo(3L)
        assertThat(correction.variantName).isEqualTo("bundle")
    }

    @Test
    fun returnQuantityExceedingPurchaseIsRejected() {
        val items = listOf(sampleTransactionItem(id = 1L, qty = 2.0))
        val products = mapOf(1L to sampleProduct(id = 1L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 3.0, conversionFactor = 1.0))

        val result = correctReturn(items, requests, products)

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun nonPositiveReturnQuantityIsRejected() {
        val items = listOf(sampleTransactionItem(id = 1L, qty = 2.0))
        val products = mapOf(1L to sampleProduct(id = 1L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 0.0, conversionFactor = 1.0))

        val result = correctReturn(items, requests, products)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun nonPositiveConversionFactorIsRejected() {
        val items = listOf(sampleTransactionItem(id = 1L, qty = 2.0))
        val products = mapOf(1L to sampleProduct(id = 1L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 1.0, conversionFactor = 0.0))

        val result = correctReturn(items, requests, products)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun unknownTransactionItemIsRejected() {
        val products = mapOf(1L to sampleProduct(id = 1L))
        val requests = listOf(ReturnLineRequest(transactionItemId = 99L, returnedQty = 1.0, conversionFactor = 1.0))

        val result = correctReturn(emptyList(), requests, products)

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.NotFound::class.java)
    }

    @Test
    fun unknownProductIsRejected() {
        val items = listOf(sampleTransactionItem(id = 1L, productId = 5L, qty = 2.0))
        val requests = listOf(ReturnLineRequest(transactionItemId = 1L, returnedQty = 1.0, conversionFactor = 1.0))

        val result = correctReturn(items, requests, emptyMap())

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.NotFound::class.java)
    }

    @Test
    fun aggregatesTotalsAcrossLines() {
        val items = listOf(
            sampleTransactionItem(id = 1L, productId = 1L, qty = 4.0, subtotal = 20_000L),
            sampleTransactionItem(id = 2L, productId = 2L, qty = 2.0, subtotal = 10_000L),
        )
        val products = mapOf(
            1L to sampleProduct(id = 1L, baseHpp = 1_000L),
            2L to sampleProduct(id = 2L, baseHpp = 500L),
        )
        val requests = listOf(
            ReturnLineRequest(transactionItemId = 1L, returnedQty = 2.0, conversionFactor = 10.0),
            ReturnLineRequest(transactionItemId = 2L, returnedQty = 1.0, conversionFactor = 1.0),
        )

        val result = correctReturn(items, requests, products)

        val correction = (result as AppResult.Success<ReturnCorrection>).value
        assertThat(correction.lines).hasSize(2)
        assertThat(correction.totalRefund).isEqualTo(15_000L)
        assertThat(correction.totalHppReversed).isEqualTo(20_500L)
        assertThat(correction.totalBaseQtyRestocked).isWithin(1e-9).of(21.0)
    }

    @Test
    fun exchangeSettlementChargesDifferenceWhenNewSaleIsHigher() {
        val returnCorrection = ReturnCorrection(
            lines = emptyList(),
            totalRefund = 15_000L,
            totalHppReversed = 0L,
            totalBaseQtyRestocked = 0.0,
        )

        val settlement = settleExchange(returnCorrection, 40_000L)

        assertThat(settlement.amountDueFromCustomer).isEqualTo(25_000L)
        assertThat(settlement.refundToCustomer).isEqualTo(0L)
    }

    @Test
    fun exchangeSettlementRefundsWhenReturnExceedsNewSale() {
        val returnCorrection = ReturnCorrection(
            lines = emptyList(),
            totalRefund = 15_000L,
            totalHppReversed = 0L,
            totalBaseQtyRestocked = 0.0,
        )

        val settlement = settleExchange(returnCorrection, 10_000L)

        assertThat(settlement.refundToCustomer).isEqualTo(5_000L)
        assertThat(settlement.amountDueFromCustomer).isEqualTo(0L)
    }
}
