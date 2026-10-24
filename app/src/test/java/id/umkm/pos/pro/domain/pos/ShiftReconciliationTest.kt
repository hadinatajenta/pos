package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShiftReconciliationTest {

    @Test
    fun expectedEndingCashAddsCashSalesThenSubtractsRefunds() {
        val summary = ShiftReconciliation.summarize(
            startingCash = 100_000L,
            cashSales = 250_000L,
            cashRefunds = 20_000L,
            transactionCount = 7,
        )

        assertThat(summary.startingCash).isEqualTo(100_000L)
        assertThat(summary.cashSales).isEqualTo(250_000L)
        assertThat(summary.cashRefunds).isEqualTo(20_000L)
        assertThat(summary.expectedEndingCash).isEqualTo(330_000L)
    }

    @Test
    fun anEmptyShiftKeepsOnlyTheStartingCash() {
        val summary = ShiftReconciliation.summarize(
            startingCash = 0L,
            cashSales = 0L,
            cashRefunds = 0L,
            transactionCount = 0,
        )

        assertThat(summary.expectedEndingCash).isEqualTo(0L)
        assertThat(summary.transactionCount).isEqualTo(0)
    }

    @Test
    fun refundsAboveCashSalesProduceANegativeVariance() {
        val summary = ShiftReconciliation.summarize(
            startingCash = 100_000L,
            cashSales = 30_000L,
            cashRefunds = 50_000L,
            transactionCount = 3,
        )
        val actualEndingCash = 75_000L

        assertThat(summary.expectedEndingCash).isEqualTo(80_000L)
        assertThat(actualEndingCash - summary.expectedEndingCash).isEqualTo(-5_000L)
    }

    @Test
    fun largeCashAmountsStayWithinLongArithmetic() {
        val summary = ShiftReconciliation.summarize(
            startingCash = 1_000_000_000_000_000_000L,
            cashSales = 2_000_000_000_000_000_000L,
            cashRefunds = 100_000_000_000_000_000L,
            transactionCount = 10_000,
        )

        assertThat(summary.expectedEndingCash).isEqualTo(2_900_000_000_000_000_000L)
    }

    @Test
    fun transactionCountPassesThroughUnchanged() {
        val summary = ShiftReconciliation.summarize(
            startingCash = 5_000L,
            cashSales = 10_000L,
            cashRefunds = 1_000L,
            transactionCount = 42,
        )

        assertThat(summary.transactionCount).isEqualTo(42)
    }
}
