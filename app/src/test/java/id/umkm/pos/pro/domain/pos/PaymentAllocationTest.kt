package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.TransactionStatus
import org.junit.Test

class PaymentAllocationTest {

    @Test
    fun cashExactPaymentCompletesWithNoChange() {
        val allocation = allocatePayment(PaymentMethod.CASH, 50_000L, 50_000L)

        assertThat(allocation.paidAmount).isEqualTo(50_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
        assertThat(allocation.remainingDebt).isEqualTo(0L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun cashOverpaymentYieldsChangeAndCompletes() {
        val allocation = allocatePayment(PaymentMethod.CASH, 100_000L, 92_500L)

        assertThat(allocation.paidAmount).isEqualTo(100_000L)
        assertThat(allocation.changeAmount).isEqualTo(7_500L)
        assertThat(allocation.remainingDebt).isEqualTo(0L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun cashUnderpaymentBecomesPartialDebt() {
        val allocation = allocatePayment(PaymentMethod.CASH, 40_000L, 50_000L)

        assertThat(allocation.paidAmount).isEqualTo(40_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
        assertThat(allocation.remainingDebt).isEqualTo(10_000L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.PARTIAL_DEBT)
    }

    @Test
    fun qrisExactPaymentCompletes() {
        val allocation = allocatePayment(PaymentMethod.QRIS, 75_000L, 75_000L)

        assertThat(allocation.paidAmount).isEqualTo(75_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun qrisOverpaymentClampsPaidAmountToTotal() {
        val allocation = allocatePayment(PaymentMethod.QRIS, 80_000L, 75_000L)

        assertThat(allocation.paidAmount).isEqualTo(75_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
        assertThat(allocation.remainingDebt).isEqualTo(0L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun splitPaymentCompletesSale() {
        val allocation = allocatePayment(PaymentMethod.SPLIT, 75_000L, 75_000L)

        assertThat(allocation.paidAmount).isEqualTo(75_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun kasbonWithNoPaymentCarriesFullRemainingDebt() {
        val allocation = allocatePayment(PaymentMethod.KASBON, 0L, 120_000L)

        assertThat(allocation.paidAmount).isEqualTo(0L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
        assertThat(allocation.remainingDebt).isEqualTo(120_000L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.PARTIAL_DEBT)
    }

    @Test
    fun kasbonWithDownPaymentCarriesRemainingDebt() {
        val allocation = allocatePayment(PaymentMethod.KASBON, 50_000L, 120_000L)

        assertThat(allocation.paidAmount).isEqualTo(50_000L)
        assertThat(allocation.remainingDebt).isEqualTo(70_000L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.PARTIAL_DEBT)
    }

    @Test
    fun negativePaidAmountIsTreatedAsZero() {
        val allocation = allocatePayment(PaymentMethod.CASH, -1_000L, 50_000L)

        assertThat(allocation.paidAmount).isEqualTo(0L)
        assertThat(allocation.remainingDebt).isEqualTo(50_000L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.PARTIAL_DEBT)
    }

    @Test
    fun zeroTotalSaleCompletes() {
        val allocation = allocatePayment(PaymentMethod.CASH, 0L, 0L)

        assertThat(allocation.changeAmount).isEqualTo(0L)
        assertThat(allocation.remainingDebt).isEqualTo(0L)
        assertThat(allocation.status).isEqualTo(TransactionStatus.COMPLETED)
    }
}
