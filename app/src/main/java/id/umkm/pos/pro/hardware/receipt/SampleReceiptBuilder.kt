package id.umkm.pos.pro.hardware.receipt

import id.umkm.pos.pro.core.contract.ReceiptAlignment
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptLine
import id.umkm.pos.pro.feature.receipt.twoColumnLine
import id.umkm.pos.pro.ui.common.PosStrings

object SampleReceiptBuilder {

    fun build(
        storeName: String,
        strings: PosStrings,
        paperWidthMm: Int,
        qrisImagePath: String? = null,
    ): ReceiptDocument {
        val widthChars = if (paperWidthMm >= 80) 48 else 32
        val divider = "-".repeat(widthChars)
        val name = storeName.ifBlank { strings.receiptDefaultStoreName }
        val lines = mutableListOf<ReceiptLine>()

        lines += ReceiptLine(strings.receiptTestTitle, ReceiptAlignment.CENTER, isBold = true)
        lines += ReceiptLine(divider, ReceiptAlignment.CENTER)
        lines += ReceiptLine("1 x ${strings.receiptTestItemOne}")
        lines += ReceiptLine(twoColumnLine("", "Rp 18.000", widthChars))
        lines += ReceiptLine("1 x ${strings.receiptTestItemTwo}")
        lines += ReceiptLine(twoColumnLine("", "Rp 22.000", widthChars))
        lines += ReceiptLine(divider, ReceiptAlignment.CENTER)
        lines += ReceiptLine(twoColumnLine(strings.transactionsFieldSubtotal, "Rp 40.000", widthChars))
        lines += ReceiptLine(twoColumnLine(strings.transactionsFieldDiscount, "-Rp 5.000", widthChars))
        lines += ReceiptLine(twoColumnLine(strings.transactionsFieldTax, "Rp 3.500", widthChars))
        lines += ReceiptLine(
            twoColumnLine(strings.receiptTotalPlain, "Rp 38.500", widthChars),
            isBold = true,
        )
        lines += ReceiptLine(twoColumnLine(strings.paymentCash, "Rp 50.000", widthChars))
        lines += ReceiptLine(twoColumnLine(strings.transactionsFieldChange, "Rp 11.500", widthChars))
        lines += ReceiptLine(divider, ReceiptAlignment.CENTER)
        lines += ReceiptLine(strings.receiptTestPrinterWorking, ReceiptAlignment.CENTER)
        lines += ReceiptLine(strings.receiptTestPaperWidth(paperWidthMm), ReceiptAlignment.CENTER)
        lines += ReceiptLine(strings.receiptTestThankYou, ReceiptAlignment.CENTER)

        return ReceiptDocument(
            storeName = name,
            lines = lines,
            qrisImagePath = qrisImagePath,
        )
    }
}
