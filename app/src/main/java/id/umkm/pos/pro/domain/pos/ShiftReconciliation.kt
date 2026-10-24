package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.model.ShiftCashSummary

object ShiftReconciliation {

    fun summarize(
        startingCash: Long,
        cashSales: Long,
        cashRefunds: Long,
        transactionCount: Int,
    ): ShiftCashSummary = ShiftCashSummary(
        startingCash = startingCash,
        cashSales = cashSales,
        cashRefunds = cashRefunds,
        transactionCount = transactionCount,
    )
}
