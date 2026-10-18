package id.umkm.pos.pro.feature.receipt

import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.PosStrings

data class ReceiptShareItem(
    val name: String,
    val quantityLabel: String,
    val unitPrice: Long,
    val lineTotal: Long,
    val itemDiscount: Long = 0L,
)

data class ReceiptShareData(
    val storeName: String,
    val invoiceNo: String,
    val createdAtLabel: String,
    val cashierName: String?,
    val customerName: String?,
    val paymentMethodLabel: String,
    val items: List<ReceiptShareItem>,
    val subtotal: Long,
    val invoiceDiscount: Long,
    val taxTotal: Long,
    val rounding: Long,
    val grandTotal: Long,
    val paidAmount: Long,
    val changeAmount: Long,
    val remainingDebt: Long,
    val dueDateLabel: String?,
)

object ReceiptShareText {

    private const val SEPARATOR = "────────────────"

    fun build(data: ReceiptShareData, strings: PosStrings): String {
        val lines = mutableListOf<String>()
        lines += "*${data.storeName.ifBlank { strings.receiptDefaultStoreName }}*"
        lines += strings.receiptDefaultStoreName
        lines += SEPARATOR
        lines += strings.receiptInvoiceLabel(data.invoiceNo)
        lines += strings.receiptDateLabel(data.createdAtLabel)
        data.cashierName?.takeIf { it.isNotBlank() }?.let { cashier ->
            lines += strings.receiptCashierLabel(cashier)
        }
        data.customerName?.takeIf { it.isNotBlank() }?.let { customer ->
            lines += strings.receiptCustomerLabel(customer)
        }
        lines += strings.receiptMethodLabel(data.paymentMethodLabel)
        lines += SEPARATOR
        lines += ""
        lines += strings.receiptItemsTitle
        data.items.forEachIndexed { index, item ->
            lines += "${index + 1}. ${item.name}"
            lines += "   ${item.quantityLabel} × ${Rupiah.format(item.unitPrice)} = ${Rupiah.format(item.lineTotal)}"
            if (item.itemDiscount > 0L) {
                lines += "   ${strings.receiptItemDiscount(Rupiah.format(item.itemDiscount))}"
            }
        }
        lines += ""
        lines += SEPARATOR
        lines += strings.receiptSubtotal(Rupiah.format(data.subtotal))
        if (data.invoiceDiscount > 0L) {
            lines += strings.receiptDiscount(Rupiah.format(data.invoiceDiscount))
        }
        if (data.taxTotal > 0L) {
            lines += strings.receiptTax(Rupiah.format(data.taxTotal))
        }
        if (data.rounding != 0L) {
            lines += strings.receiptRounding(Rupiah.format(data.rounding))
        }
        lines += strings.receiptTotal(Rupiah.format(data.grandTotal))
        lines += SEPARATOR
        lines += strings.receiptPaid(Rupiah.format(data.paidAmount))
        if (data.changeAmount > 0L) {
            lines += strings.receiptChange(Rupiah.format(data.changeAmount))
        }
        if (data.remainingDebt > 0L) {
            lines += strings.receiptRemainingDebt(Rupiah.format(data.remainingDebt))
            data.dueDateLabel?.let { dueDate ->
                lines += strings.receiptDueDate(dueDate)
            }
        }
        lines += SEPARATOR
        lines += ""
        lines += strings.receiptThankYou
        lines += strings.receiptKeepAsProof
        return lines.joinToString(separator = "\n")
    }
}
