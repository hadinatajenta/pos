package id.umkm.pos.pro.feature.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReceiptShareTextTest {

    private fun receiptData(
        storeName: String = "Toko Demo",
        cashierName: String? = "Pemilik",
        customerName: String? = null,
        invoiceDiscount: Long = 0L,
        taxTotal: Long = 0L,
        rounding: Long = 0L,
        changeAmount: Long = 0L,
        remainingDebt: Long = 0L,
        dueDateLabel: String? = null,
        itemDiscount: Long = 0L,
    ): ReceiptShareData = ReceiptShareData(
        storeName = storeName,
        invoiceNo = "INV-20261017-0001",
        createdAtLabel = "17 Okt 2026, 14.05",
        cashierName = cashierName,
        customerName = customerName,
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
        remainingDebt = remainingDebt,
        dueDateLabel = dueDateLabel,
    )

    @Test
    fun includesHeaderItemsAndTotals() {
        val text = ReceiptShareText.build(receiptData())

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
        val text = ReceiptShareText.build(receiptData(changeAmount = 5_000L))

        assertThat(text).contains("Dibayar : Rp 25.000")
        assertThat(text).contains("Kembalian : Rp 5.000")
        assertThat(text).doesNotContain("Sisa utang")
        assertThat(text).doesNotContain("Jatuh tempo")
    }

    @Test
    fun kasbonSaleShowsRemainingDebtAndDueDate() {
        val text = ReceiptShareText.build(
            receiptData(
                customerName = "Eren",
                remainingDebt = 7_500L,
                dueDateLabel = "24 Okt 2026",
            ),
        )

        assertThat(text).contains("Pelanggan : Eren")
        assertThat(text).contains("Sisa utang : Rp 7.500")
        assertThat(text).contains("Jatuh tempo : 24 Okt 2026")
        assertThat(text).doesNotContain("Kembalian")
    }

    @Test
    fun discountTaxAndRoundingAppearOnlyWhenApplied() {
        val withoutExtras = ReceiptShareText.build(receiptData())
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
        )
        assertThat(withExtras).contains("Diskon item : -Rp 250")
        assertThat(withExtras).contains("Diskon : -Rp 1.000")
        assertThat(withExtras).contains("Pajak : Rp 2.000")
        assertThat(withExtras).contains("Pembulatan : Rp 500")
    }

    @Test
    fun fallsBackToTheAppNameAndSkipsBlankOptionalFields() {
        val text = ReceiptShareText.build(
            receiptData(storeName = "", cashierName = null, customerName = null),
        )

        assertThat(text).contains("*POS UMKM Pro*")
        assertThat(text).doesNotContain("Kasir :")
        assertThat(text).doesNotContain("Pelanggan :")
    }
}
