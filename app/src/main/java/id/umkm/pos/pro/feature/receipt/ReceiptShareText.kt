package id.umkm.pos.pro.feature.receipt

import id.umkm.pos.pro.feature.reports.Rupiah

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
    private const val APP_NAME = "POS UMKM Pro"

    fun build(data: ReceiptShareData): String {
        val lines = mutableListOf<String>()
        lines += "*${data.storeName.ifBlank { APP_NAME }}*"
        lines += APP_NAME
        lines += SEPARATOR
        lines += "No. Invoice : ${data.invoiceNo}"
        lines += "Tanggal : ${data.createdAtLabel}"
        data.cashierName?.takeIf { it.isNotBlank() }?.let { cashier ->
            lines += "Kasir : $cashier"
        }
        data.customerName?.takeIf { it.isNotBlank() }?.let { customer ->
            lines += "Pelanggan : $customer"
        }
        lines += "Metode : ${data.paymentMethodLabel}"
        lines += SEPARATOR
        lines += ""
        lines += "*Rincian pesanan*"
        data.items.forEachIndexed { index, item ->
            lines += "${index + 1}. ${item.name}"
            lines += "   ${item.quantityLabel} × ${Rupiah.format(item.unitPrice)} = ${Rupiah.format(item.lineTotal)}"
            if (item.itemDiscount > 0L) {
                lines += "   Diskon item : -${Rupiah.format(item.itemDiscount)}"
            }
        }
        lines += ""
        lines += SEPARATOR
        lines += "Subtotal : ${Rupiah.format(data.subtotal)}"
        if (data.invoiceDiscount > 0L) {
            lines += "Diskon : -${Rupiah.format(data.invoiceDiscount)}"
        }
        if (data.taxTotal > 0L) {
            lines += "Pajak : ${Rupiah.format(data.taxTotal)}"
        }
        if (data.rounding != 0L) {
            lines += "Pembulatan : ${Rupiah.format(data.rounding)}"
        }
        lines += "*TOTAL : ${Rupiah.format(data.grandTotal)}*"
        lines += SEPARATOR
        lines += "Dibayar : ${Rupiah.format(data.paidAmount)}"
        if (data.changeAmount > 0L) {
            lines += "Kembalian : ${Rupiah.format(data.changeAmount)}"
        }
        if (data.remainingDebt > 0L) {
            lines += "Sisa utang : ${Rupiah.format(data.remainingDebt)}"
            data.dueDateLabel?.let { dueDate ->
                lines += "Jatuh tempo : $dueDate"
            }
        }
        lines += SEPARATOR
        lines += ""
        lines += "Terima kasih sudah berbelanja."
        lines += "Simpan pesan ini sebagai bukti pembayaran."
        return lines.joinToString(separator = "\n")
    }
}
