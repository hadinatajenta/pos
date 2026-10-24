package id.umkm.pos.pro.feature.receipt

import id.umkm.pos.pro.core.contract.ReceiptAlignment
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptLine
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.paymentMethodLabel

internal data class StoredReceipt(
    val shareData: ReceiptShareData,
    val document: ReceiptDocument,
)

internal object StoredReceiptBuilder {

    private const val RECEIPT_WIDTH_CHARS = 32
    private const val FALLBACK_STORE_NAME = "Pookie POS"

    fun build(
        transaction: Transaction,
        items: List<TransactionItem>,
        productsById: Map<Long, Product>,
        storeName: String,
        cashierName: String?,
        createdAtLabel: String,
        strings: PosStrings,
    ): StoredReceipt {
        val receiptItems = items.map { item ->
            val product = productsById[item.productId]
            val unitLabel = item.unitLabelSnapshot ?: item.variantName ?: product?.baseUnit.orEmpty()
            StoredReceiptItem(
                name = item.productNameSnapshot ?: product?.name ?: "Item ${item.productId}",
                quantityLabel = "${formatQuantity(item.qty)} $unitLabel".trim(),
                unitPrice = item.appliedUnitPrice,
                lineTotal = item.subtotal,
                itemDiscount = item.itemDiscount,
            )
        }
        val shareData = ReceiptShareData(
            storeName = storeName,
            invoiceNo = transaction.invoiceNo,
            createdAtLabel = createdAtLabel,
            cashierName = cashierName,
            paymentMethodLabel = paymentMethodLabel(transaction.paymentMethod, strings),
            items = receiptItems.map { item ->
                ReceiptShareItem(
                    name = item.name,
                    quantityLabel = item.quantityLabel,
                    unitPrice = item.unitPrice,
                    lineTotal = item.lineTotal,
                    itemDiscount = item.itemDiscount,
                )
            },
            subtotal = transaction.subtotal,
            invoiceDiscount = transaction.invoiceDiscount,
            taxTotal = transaction.taxTotal,
            rounding = transaction.rounding,
            grandTotal = transaction.grandTotal,
            paidAmount = transaction.paidAmount,
            changeAmount = transaction.changeAmount,
        )
        val document = ReceiptDocument(
            storeName = storeName,
            lines = documentLines(transaction, receiptItems, strings),
        )
        return StoredReceipt(shareData = shareData, document = document)
    }

    fun toPrintText(receipt: StoredReceipt): String = buildString {
        appendLine(receipt.shareData.storeName.ifBlank { FALLBACK_STORE_NAME })
        appendLine()
        receipt.document.lines.forEach { line -> appendLine(line.text) }
    }

    private fun documentLines(
        transaction: Transaction,
        receiptItems: List<StoredReceiptItem>,
        strings: PosStrings,
    ): List<ReceiptLine> {
        val lines = mutableListOf<ReceiptLine>()
        lines += ReceiptLine(
            strings.receiptInvoiceHeader(transaction.invoiceNo),
            alignment = ReceiptAlignment.CENTER,
        )
        lines += ReceiptLine("")
        receiptItems.forEach { item ->
            lines += ReceiptLine("${item.quantityLabel} x ${item.name}")
            lines += ReceiptLine(twoColumnLine("", Rupiah.format(item.lineTotal), RECEIPT_WIDTH_CHARS))
        }
        lines += ReceiptLine("")
        lines += ReceiptLine(
            twoColumnLine(strings.transactionsFieldSubtotal, Rupiah.format(transaction.subtotal), RECEIPT_WIDTH_CHARS),
        )
        if (transaction.invoiceDiscount > 0L) {
            lines += ReceiptLine(
                twoColumnLine(
                    strings.transactionsFieldDiscount,
                    "-${Rupiah.format(transaction.invoiceDiscount)}",
                    RECEIPT_WIDTH_CHARS,
                ),
            )
        }
        if (transaction.taxTotal > 0L) {
            lines += ReceiptLine(
                twoColumnLine(strings.transactionsFieldTax, Rupiah.format(transaction.taxTotal), RECEIPT_WIDTH_CHARS),
            )
        }
        if (transaction.rounding != 0L) {
            lines += ReceiptLine(
                twoColumnLine(
                    strings.transactionsFieldRounding,
                    Rupiah.format(transaction.rounding),
                    RECEIPT_WIDTH_CHARS,
                ),
            )
        }
        lines += ReceiptLine(
            twoColumnLine(strings.receiptTotalPlain, Rupiah.format(transaction.grandTotal), RECEIPT_WIDTH_CHARS),
            isBold = true,
        )
        lines += ReceiptLine(
            twoColumnLine(strings.transactionsFieldPaid, Rupiah.format(transaction.paidAmount), RECEIPT_WIDTH_CHARS),
        )
        if (transaction.changeAmount > 0L) {
            lines += ReceiptLine(
                twoColumnLine(
                    strings.transactionsFieldChange,
                    Rupiah.format(transaction.changeAmount),
                    RECEIPT_WIDTH_CHARS,
                ),
            )
        }
        return lines
    }
}

private data class StoredReceiptItem(
    val name: String,
    val quantityLabel: String,
    val unitPrice: Long,
    val lineTotal: Long,
    val itemDiscount: Long,
)
