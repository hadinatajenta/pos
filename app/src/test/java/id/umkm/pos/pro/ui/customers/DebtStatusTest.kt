package id.umkm.pos.pro.ui.customers

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.ui.uiSampleCustomer
import id.umkm.pos.pro.ui.uiSampleDebtTransaction
import org.junit.Test

class DebtStatusTest {

    private val nowMillis = 10_000L

    @Test
    fun customerWithoutDebtIsPaid() {
        val customer = uiSampleCustomer(id = 1L, name = "Ibu Ani", totalDebt = 0L)

        val status = resolveDebtStatus(customer, emptyList(), nowMillis)

        assertThat(status).isEqualTo(DebtStatus.PAID)
    }

    @Test
    fun overdueDebtOutranksPartialPayments() {
        val customer = uiSampleCustomer(id = 1L, name = "Pak Budi", totalDebt = 100_000L)
        val transactions = listOf(
            uiSampleDebtTransaction(id = 1L, customerId = 1L, remainingDebt = 100_000L, paidAmount = 20_000L, dueDate = 5_000L),
        )

        assertThat(resolveDebtStatus(customer, transactions, nowMillis)).isEqualTo(DebtStatus.OVERDUE)
    }

    @Test
    fun debtWithAPriorPaymentIsPartiallyPaid() {
        val customer = uiSampleCustomer(id = 1L, name = "Pak Budi", totalDebt = 100_000L)
        val transactions = listOf(
            uiSampleDebtTransaction(id = 1L, customerId = 1L, remainingDebt = 100_000L, paidAmount = 20_000L),
        )

        assertThat(resolveDebtStatus(customer, transactions, nowMillis)).isEqualTo(DebtStatus.PARTIALLY_PAID)
    }

    @Test
    fun debtWithoutAnyPaymentIsUnpaid() {
        val customer = uiSampleCustomer(id = 1L, name = "Pak Budi", totalDebt = 100_000L)
        val transactions = listOf(
            uiSampleDebtTransaction(id = 1L, customerId = 1L, remainingDebt = 100_000L),
        )

        assertThat(resolveDebtStatus(customer, transactions, nowMillis)).isEqualTo(DebtStatus.UNPAID)
    }

    @Test
    fun settledTransactionIsNotOverdue() {
        val customer = uiSampleCustomer(id = 1L, name = "Pak Budi", totalDebt = 50_000L)
        val transactions = listOf(
            uiSampleDebtTransaction(id = 1L, customerId = 1L, remainingDebt = 0L, paidAmount = 50_000L, dueDate = 5_000L),
            uiSampleDebtTransaction(id = 2L, customerId = 1L, remainingDebt = 50_000L, dueDate = 99_000L),
        )

        assertThat(resolveDebtStatus(customer, transactions, nowMillis)).isEqualTo(DebtStatus.PARTIALLY_PAID)
    }

    @Test
    fun nextDueDateUsesTheEarliestUnpaidTransaction() {
        val transactions: List<Transaction> = listOf(
            uiSampleDebtTransaction(id = 1L, customerId = 1L, remainingDebt = 10_000L, dueDate = 80_000L),
            uiSampleDebtTransaction(id = 2L, customerId = 1L, remainingDebt = 10_000L, dueDate = 40_000L),
            uiSampleDebtTransaction(id = 3L, customerId = 1L, remainingDebt = 0L, paidAmount = 5_000L, dueDate = 20_000L),
            uiSampleDebtTransaction(id = 4L, customerId = 1L, remainingDebt = 10_000L, dueDate = null),
        )

        assertThat(nextDueDateMillis(transactions)).isEqualTo(40_000L)
    }

    @Test
    fun debtStatusLabelsAreDistinct() {
        val labels = DebtStatus.entries.map { debtStatusLabel(it) }

        assertThat(labels.toSet()).hasSize(DebtStatus.entries.size)
        assertThat(labels).containsExactly("Paid", "Unpaid", "Partially paid", "Overdue")
    }
}
