package id.umkm.pos.pro.ui.customers

import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.ui.common.PosStrings

enum class DebtStatus {
    PAID,
    UNPAID,
    PARTIALLY_PAID,
    OVERDUE,
}

fun resolveDebtStatus(
    customer: Customer,
    transactions: List<Transaction>,
    nowMillis: Long,
): DebtStatus {
    if (customer.totalDebt <= 0L) return DebtStatus.PAID
    val unpaidTransactions = transactions.filter { it.remainingDebt > 0L }
    if (unpaidTransactions.any { isOverdue(it, nowMillis) }) return DebtStatus.OVERDUE
    if (transactions.any { it.paidAmount > 0L }) return DebtStatus.PARTIALLY_PAID
    return DebtStatus.UNPAID
}

fun debtStatusLabel(status: DebtStatus, strings: PosStrings): String = when (status) {
    DebtStatus.PAID -> strings.debtStatusPaid
    DebtStatus.UNPAID -> strings.debtStatusUnpaid
    DebtStatus.PARTIALLY_PAID -> strings.debtStatusPartiallyPaid
    DebtStatus.OVERDUE -> strings.debtStatusOverdue
}

fun nextDueDateMillis(transactions: List<Transaction>): Long? = transactions
    .filter { it.remainingDebt > 0L }
    .mapNotNull { it.dueDate }
    .minOrNull()

fun isOverdue(transaction: Transaction, nowMillis: Long): Boolean =
    transaction.remainingDebt > 0L && transaction.dueDate?.let { it < nowMillis } == true
