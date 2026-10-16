package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus

internal fun sampleProduct(
    id: Long = 1L,
    baseHpp: Long = 1_000L,
    basePrice: Long = 5_000L,
): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = null,
    name = "Product $id",
    category = null,
    baseUnit = "pcs",
    baseHpp = baseHpp,
    basePrice = basePrice,
    stockBaseQty = 100.0,
    minStockAlert = 5.0,
    isActive = true,
)

internal fun sampleUnit(
    id: Long,
    productId: Long = 1L,
    variantName: String = "unit-$id",
    conversionFactor: Double = 1.0,
    unitPrice: Long = 5_000L,
    wholesaleMinQty: Int? = null,
    wholesalePrice: Long? = null,
): ProductUnit = ProductUnit(
    id = id,
    productId = productId,
    variantName = variantName,
    conversionFactor = conversionFactor,
    customBarcode = null,
    unitPrice = unitPrice,
    wholesaleMinQty = wholesaleMinQty,
    wholesalePrice = wholesalePrice,
)

internal fun sampleLine(
    lineId: String = "line-1",
    productId: Long = 1L,
    unitId: Long? = null,
    variantName: String? = null,
    qty: Double = 1.0,
    negotiatedUnitPrice: Long? = null,
    itemDiscount: Long = 0L,
): CartLine = CartLine(
    lineId = lineId,
    productId = productId,
    unitId = unitId,
    variantName = variantName,
    qty = qty,
    negotiatedUnitPrice = negotiatedUnitPrice,
    itemDiscount = itemDiscount,
)

internal fun sampleTransactionItem(
    id: Long = 1L,
    transactionId: Long = 1L,
    productId: Long = 1L,
    variantName: String? = null,
    qty: Double = 1.0,
    unitHpp: Long = 1_000L,
    appliedUnitPrice: Long = 5_000L,
    isNegoPrice: Boolean = false,
    isWholesaleApplied: Boolean = false,
    itemDiscount: Long = 0L,
    subtotal: Long = 5_000L,
): TransactionItem = TransactionItem(
    id = id,
    transactionId = transactionId,
    productId = productId,
    variantName = variantName,
    qty = qty,
    unitHpp = unitHpp,
    appliedUnitPrice = appliedUnitPrice,
    isNegoPrice = isNegoPrice,
    isWholesaleApplied = isWholesaleApplied,
    itemDiscount = itemDiscount,
    subtotal = subtotal,
)

internal fun sampleTransaction(
    id: Long = 1L,
    invoiceNo: String = "INV-$id",
    customerId: Long? = 7L,
    subtotal: Long = 10_000L,
    invoiceDiscount: Long = 0L,
    grandTotal: Long = 10_000L,
    totalHpp: Long = 4_000L,
    remainingDebt: Long = 0L,
    status: TransactionStatus = TransactionStatus.COMPLETED,
    paymentMethod: PaymentMethod = PaymentMethod.KASBON,
): Transaction = Transaction(
    id = id,
    invoiceNo = invoiceNo,
    createdAt = 1_700_000_000_000L,
    cashierId = 1L,
    customerId = customerId,
    subtotal = subtotal,
    discountTotal = invoiceDiscount,
    invoiceDiscount = invoiceDiscount,
    taxTotal = 0L,
    rounding = 0L,
    grandTotal = grandTotal,
    totalHpp = totalHpp,
    netProfit = subtotal - invoiceDiscount - totalHpp,
    paymentMethod = paymentMethod,
    paidAmount = grandTotal - remainingDebt,
    changeAmount = 0L,
    remainingDebt = remainingDebt,
    dueDate = null,
    status = status,
    notes = null,
)
