package id.umkm.pos.pro.hardware.receipt

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ReceiptAlignment
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import org.junit.Test

class SampleReceiptBuilderTest {

    @Test
    fun build58mmUses32CharDividerAndIndonesianStrings() {
        val doc = SampleReceiptBuilder.build(
            storeName = "Kopi Kenangan UMKM",
            strings = IdStrings,
            paperWidthMm = 58,
        )

        assertThat(doc.storeName).isEqualTo("Kopi Kenangan UMKM")
        assertThat(doc.lines.first().text).isEqualTo("STRUK UJI COBA")
        assertThat(doc.lines.first().isBold).isTrue()
        assertThat(doc.lines.first().alignment).isEqualTo(ReceiptAlignment.CENTER)
        val dividerLine = doc.lines[1].text
        assertThat(dividerLine).isEqualTo("-".repeat(32))
        assertThat(doc.lines.map { it.text }).contains("1 x Kopi Susu Gula Aren")
        assertThat(doc.lines.any { it.text.contains("Lebar Kertas: 58mm") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("Printer Thermal Berfungsi Normal") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("Terima Kasih Atas Kunjungan Anda") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("Tunai") }).isTrue()
    }

    @Test
    fun build80mmUses48CharDividerAndEnglishStrings() {
        val doc = SampleReceiptBuilder.build(
            storeName = "Coffee Shop",
            strings = EnStrings,
            paperWidthMm = 80,
        )

        assertThat(doc.storeName).isEqualTo("Coffee Shop")
        assertThat(doc.lines.first().text).isEqualTo("TEST RECEIPT")
        val dividerLine = doc.lines[1].text
        assertThat(dividerLine).isEqualTo("-".repeat(48))
        assertThat(doc.lines.map { it.text }).contains("1 x Palm Sugar Milk Coffee")
        assertThat(doc.lines.any { it.text.contains("Paper Width: 80mm") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("Thermal Printer Working Properly") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("Thank You For Your Business") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("Cash") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("STRUK UJI COBA") }).isFalse()
    }

    @Test
    fun buildChineseStringsContainsCjkCharacters() {
        val doc = SampleReceiptBuilder.build(
            storeName = "老王茶餐厅",
            strings = ZhStrings,
            paperWidthMm = 58,
        )

        assertThat(doc.storeName).isEqualTo("老王茶餐厅")
        assertThat(doc.lines.first().text).isEqualTo("测试收据")
        assertThat(doc.lines.map { it.text }).contains("1 x 招牌奶茶")
        assertThat(doc.lines.any { it.text.contains("纸张规格: 58mm") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("热敏打印机工作正常") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("感谢您的光临") }).isTrue()
        assertThat(doc.lines.any { it.text.contains("现金") }).isTrue()
    }
}
