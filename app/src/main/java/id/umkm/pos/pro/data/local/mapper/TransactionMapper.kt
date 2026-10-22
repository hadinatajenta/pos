package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.data.local.entity.TransactionEntity
import id.umkm.pos.pro.data.local.entity.TransactionItemEntity

object TransactionMapper {

    fun toEntity(transaction: Transaction): TransactionEntity = TransactionEntity(
        id = transaction.id,
        invoiceNo = transaction.invoiceNo,
        createdAt = transaction.createdAt,
        cashierId = transaction.cashierId,
        customerId = transaction.customerId,
        subtotal = transaction.subtotal,
        discountTotal = transaction.discountTotal,
        invoiceDiscount = transaction.invoiceDiscount,
        taxTotal = transaction.taxTotal,
        rounding = transaction.rounding,
        grandTotal = transaction.grandTotal,
        totalHpp = transaction.totalHpp,
        netProfit = transaction.netProfit,
        paymentMethod = transaction.paymentMethod,
        paidAmount = transaction.paidAmount,
        changeAmount = transaction.changeAmount,
        remainingDebt = transaction.remainingDebt,
        dueDate = transaction.dueDate,
        status = transaction.status,
        notes = transaction.notes,
        cashierName = transaction.cashierName,
        shiftId = transaction.shiftId,
        staffId = transaction.staffId,
    )

    fun toModel(entity: TransactionEntity): Transaction = Transaction(
        id = entity.id,
        invoiceNo = entity.invoiceNo,
        createdAt = entity.createdAt,
        cashierId = entity.cashierId,
        customerId = entity.customerId,
        subtotal = entity.subtotal,
        discountTotal = entity.discountTotal,
        invoiceDiscount = entity.invoiceDiscount,
        taxTotal = entity.taxTotal,
        rounding = entity.rounding,
        grandTotal = entity.grandTotal,
        totalHpp = entity.totalHpp,
        netProfit = entity.netProfit,
        paymentMethod = entity.paymentMethod,
        paidAmount = entity.paidAmount,
        changeAmount = entity.changeAmount,
        remainingDebt = entity.remainingDebt,
        dueDate = entity.dueDate,
        status = entity.status,
        notes = entity.notes,
        cashierName = entity.cashierName,
        shiftId = entity.shiftId,
        staffId = entity.staffId,
    )

    fun toEntity(item: TransactionItem): TransactionItemEntity = TransactionItemEntity(
        id = item.id,
        transactionId = item.transactionId,
        productId = item.productId,
        variantName = item.variantName,
        qty = item.qty,
        unitHpp = item.unitHpp,
        appliedUnitPrice = item.appliedUnitPrice,
        isNegoPrice = item.isNegoPrice,
        isWholesaleApplied = item.isWholesaleApplied,
        itemDiscount = item.itemDiscount,
        subtotal = item.subtotal,
        conversionFactor = item.conversionFactor,
    )

    fun toModel(entity: TransactionItemEntity): TransactionItem = TransactionItem(
        id = entity.id,
        transactionId = entity.transactionId,
        productId = entity.productId,
        variantName = entity.variantName,
        qty = entity.qty,
        unitHpp = entity.unitHpp,
        appliedUnitPrice = entity.appliedUnitPrice,
        isNegoPrice = entity.isNegoPrice,
        isWholesaleApplied = entity.isWholesaleApplied,
        itemDiscount = entity.itemDiscount,
        subtotal = entity.subtotal,
        conversionFactor = entity.conversionFactor,
    )
}
