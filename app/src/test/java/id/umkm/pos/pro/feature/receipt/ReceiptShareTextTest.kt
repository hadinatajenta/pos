package id.umkm.pos.pro.feature.receipt

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import org.junit.Test

class ReceiptShareTextTest {

    private fun receiptData(
        storeName: String = "Toko Demo",
        cashierName: String? = "Pemilik",
        invoiceDiscount: Long = 0L,
        taxTotal: Long = 0L,
        rounding: Long = 0L,
        changeAmount: Long = 0L,
        itemDiscount: Long = 0L,
    ): ReceiptShareData = ReceiptShareData(
        storeName = storeName,
        invoiceNo = "INV-20261017-0001",
        createdAtLabel = "17 Okt 2026, 14.05",
        cashierName = cashierName,
        paymentMethodLabel = "Tunai",
        items = listOf(
            ReceiptShareItem(
                name = "Kopi Susu",
                quantityLabel = "2 pcs",
                unitPrice = 10_000L,
                lineTotal = 20_000L,
                itemDiscount = itemDiscount,
            ),
        ),
        subtotal = 20_000L,
        invoiceDiscount = invoiceDiscount,
        taxTotal = taxTotal,
        rounding = rounding,
        grandTotal = 20_000L,
        paidAmount = 25_000L,
        changeAmount = changeAmount,
    )

    @Test
    fun includesHeaderItemsAndTotals() {
        val text = ReceiptShareText.build(receiptData(), IdStrings)

        assertThat(text).contains("*Toko Demo*")
        assertThat(text).contains("No. Invoice : INV-20261017-0001")
        assertThat(text).contains("Tanggal : 17 Okt 2026, 14.05")
        assertThat(text).contains("Kasir : Pemilik")
        assertThat(text).contains("Metode : Tunai")
        assertThat(text).contains("*Rincian pesanan*")
        assertThat(text).contains("1. Kopi Susu")
        assertThat(text).contains("2 pcs × Rp 10.000 = Rp 20.000")
        assertThat(text).contains("Subtotal : Rp 20.000")
        assertThat(text).contains("*TOTAL : Rp 20.000*")
        assertThat(text).contains("Terima kasih sudah berbelanja.")
        assertThat(text).contains("────────────────")
    }

    @Test
    fun cashSaleShowsChangeWithoutDebtLines() {
        val text = ReceiptShareText.build(receiptData(changeAmount = 5_000L), IdStrings)

        assertThat(text).contains("Dibayar : Rp 25.000")
        assertThat(text).contains("Kembalian : Rp 5.000")
        assertThat(text).doesNotContain("Sisa utang")
        assertThat(text).doesNotContain("Jatuh tempo")
    }

    @Test
    fun discountTaxAndRoundingAppearOnlyWhenApplied() {
        val withoutExtras = ReceiptShareText.build(receiptData(), IdStrings)
        assertThat(withoutExtras).doesNotContain("Diskon item")
        assertThat(withoutExtras).doesNotContain("Pajak")
        assertThat(withoutExtras).doesNotContain("Pembulatan")

        val withExtras = ReceiptShareText.build(
            receiptData(
                invoiceDiscount = 1_000L,
                taxTotal = 2_000L,
                rounding = 500L,
                itemDiscount = 250L,
            ),
            IdStrings,
        )
        assertThat(withExtras).contains("Diskon item : -Rp 250")
        assertThat(withExtras).contains("Diskon : -Rp 1.000")
        assertThat(withExtras).contains("Pajak : Rp 2.000")
        assertThat(withExtras).contains("Pembulatan : Rp 500")
    }

    @Test
    fun fallsBackToTheAppNameAndSkipsBlankOptionalFields() {
        val text = ReceiptShareText.build(
            receiptData(storeName = "", cashierName = null),
            IdStrings,
        )

        assertThat(text).contains("*Pookie POS*")
        assertThat(text).doesNotContain("Kasir :")
    }

    @Test
    fun englishCatalogueLocalizesLabelsAndFooter() {
        val text = ReceiptShareText.build(receiptData(changeAmount = 5_000L), EnStrings)

        assertThat(text).contains("Invoice no. : INV-20261017-0001")
        assertThat(text).contains("Cashier : Pemilik")
        assertThat(text).contains("*Order details*")
        assertThat(text).contains("Paid : Rp 25.000")
        assertThat(text).contains("Change : Rp 5.000")
        assertThat(text).contains("Thank you for your purchase.")
        assertThat(text).contains("Keep this message as proof of payment.")
        assertThat(text).doesNotContain("Terima kasih")
        assertThat(text).doesNotContain("*Rincian pesanan*")
    }

    @Test
    fun chineseCatalogueLocalizesLabelsAndFooter() {
        val text = ReceiptShareText.build(receiptData(), ZhStrings)

        assertThat(text).contains("单据编号 : INV-20261017-0001")
        assertThat(text).contains("*商品明细*")
        assertThat(text).contains("小计 : Rp 20.000")
        assertThat(text).contains("*总计 : Rp 20.000*")
        assertThat(text).contains("感谢您的光临。")
        assertThat(text).contains("请保存本条信息作为付款凭证。")
        assertThat(text).doesNotContain("Terima kasih")
    }
}
