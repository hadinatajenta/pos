package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.model.ShiftCashSummary

object ShiftReconciliation {

    fun summarize(
        startingCash: Long,
        cashSales: Long,
        cashDebtPayments: Long,
        cashRefunds: Long,
        transactionCount: Int,
    ): ShiftCashSummary = ShiftCashSummary(
        startingCash = startingCash,
        cashSales = cashSales,
        cashDebtPayments = cashDebtPayments,
        cashRefunds = cashRefunds,
        transactionCount = transactionCount,
    )
}
