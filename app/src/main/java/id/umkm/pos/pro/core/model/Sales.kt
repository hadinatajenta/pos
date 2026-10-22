package id.umkm.pos.pro.core.model

data class Customer(
    val id: Long,
    val name: String,
    val whatsappNumber: String?,
    val totalDebt: Long,
    val notes: String?,
)

data class Transaction(
    val id: Long,
    val invoiceNo: String,
    val createdAt: Long,
    val cashierId: Long?,
    val customerId: Long?,
    val subtotal: Long,
    val discountTotal: Long,
    val invoiceDiscount: Long,
    val taxTotal: Long,
    val rounding: Long,
    val grandTotal: Long,
    val totalHpp: Long,
    val netProfit: Long,
    val paymentMethod: PaymentMethod,
    val paidAmount: Long,
    val changeAmount: Long,
    val remainingDebt: Long,
    val dueDate: Long?,
    val status: TransactionStatus,
    val notes: String?,
    val cashierName: String? = null,
    val shiftId: Long? = null,
    val staffId: Long? = null,
)

data class TransactionItem(
    val id: Long,
    val transactionId: Long,
    val productId: Long,
    val variantName: String?,
    val qty: Double,
    val unitHpp: Long,
    val appliedUnitPrice: Long,
    val isNegoPrice: Boolean,
    val isWholesaleApplied: Boolean,
    val itemDiscount: Long,
    val subtotal: Long,
    val conversionFactor: Double? = null,
)

data class DebtPayment(
    val id: Long,
    val transactionId: Long,
    val customerId: Long,
    val paidAt: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val note: String?,
)

data class CashierShift(
    val id: Long,
    val cashierName: String,
    val openedAt: Long,
    val closedAt: Long?,
    val startingCash: Long,
    val expectedEndingCash: Long?,
    val actualEndingCash: Long?,
    val openedByStaffId: Long? = null,
    val closedByStaffId: Long? = null,
    val closedByName: String? = null,
)

data class ShiftCashSummary(
    val startingCash: Long,
    val cashSales: Long,
    val cashDebtPayments: Long,
    val cashRefunds: Long,
    val transactionCount: Int,
) {
    val expectedEndingCash: Long
        get() = startingCash + cashSales + cashDebtPayments - cashRefunds
}

data class ShiftSwitchResult(
    val closedShift: CashierShift,
    val openedShift: CashierShift,
)
