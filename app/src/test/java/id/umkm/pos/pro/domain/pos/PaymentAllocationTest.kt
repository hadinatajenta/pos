package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.PaymentMethod
import org.junit.Test

class PaymentAllocationTest {

    @Test
    fun cashExactPaymentCompletesWithNoChange() {
        val allocation = allocatePayment(PaymentMethod.CASH, 50_000L, 50_000L)

        assertThat(allocation.paidAmount).isEqualTo(50_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
    }

    @Test
    fun cashOverpaymentYieldsChange() {
        val allocation = allocatePayment(PaymentMethod.CASH, 100_000L, 92_500L)

        assertThat(allocation.paidAmount).isEqualTo(100_000L)
        assertThat(allocation.changeAmount).isEqualTo(7_500L)
    }

    @Test
    fun cashUnderpaymentKeepsTheTenderedAmountWithoutChange() {
        val allocation = allocatePayment(PaymentMethod.CASH, 40_000L, 50_000L)

        assertThat(allocation.paidAmount).isEqualTo(40_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
    }

    @Test
    fun qrisExactPaymentCompletes() {
        val allocation = allocatePayment(PaymentMethod.QRIS, 75_000L, 75_000L)

        assertThat(allocation.paidAmount).isEqualTo(75_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
    }

    @Test
    fun qrisOverpaymentClampsPaidAmountToTotal() {
        val allocation = allocatePayment(PaymentMethod.QRIS, 80_000L, 75_000L)

        assertThat(allocation.paidAmount).isEqualTo(75_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
    }

    @Test
    fun transferOverpaymentClampsPaidAmountToTotal() {
        val allocation = allocatePayment(PaymentMethod.TRANSFER, 90_000L, 80_000L)

        assertThat(allocation.paidAmount).isEqualTo(80_000L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
    }

    @Test
    fun negativePaidAmountIsTreatedAsZero() {
        val allocation = allocatePayment(PaymentMethod.CASH, -1_000L, 50_000L)

        assertThat(allocation.paidAmount).isEqualTo(0L)
        assertThat(allocation.changeAmount).isEqualTo(0L)
    }

    @Test
    fun zeroTotalSaleCompletes() {
        val allocation = allocatePayment(PaymentMethod.CASH, 0L, 0L)

        assertThat(allocation.changeAmount).isEqualTo(0L)
    }
}
