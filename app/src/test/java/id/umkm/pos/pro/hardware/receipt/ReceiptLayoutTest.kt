package id.umkm.pos.pro.hardware.receipt

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ReceiptAlignment
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptLine
import org.junit.Assert.assertThrows
import org.junit.Test

class ReceiptLayoutTest {

    @Test
    fun wrapTextBreaksOnWordBoundaries() {
        assertThat(ReceiptLayout.wrapText("hello world today", 11))
            .containsExactly("hello world", "today")
            .inOrder()
    }

    @Test
    fun wrapTextSplitsWordsLongerThanTheWidth() {
        assertThat(ReceiptLayout.wrapText("abcdefghij", 4))
            .containsExactly("abcd", "efgh", "ij")
            .inOrder()
    }

    @Test
    fun wrapTextKeepsEmptyInputAsSingleBlankLine() {
        assertThat(ReceiptLayout.wrapText("", 32)).containsExactly("")
    }

    @Test
    fun wrapTextRejectsNonPositiveWidth() {
        assertThrows(IllegalArgumentException::class.java) { ReceiptLayout.wrapText("data", 0) }
    }

    @Test
    fun padToWidthAlignsStartCenterAndEnd() {
        assertThat(ReceiptLayout.padToWidth("AB", 6, ReceiptAlignment.START)).isEqualTo("AB    ")
        assertThat(ReceiptLayout.padToWidth("AB", 6, ReceiptAlignment.END)).isEqualTo("    AB")
        assertThat(ReceiptLayout.padToWidth("AB", 6, ReceiptAlignment.CENTER)).isEqualTo("  AB  ")
        assertThat(ReceiptLayout.padToWidth("AB", 5, ReceiptAlignment.CENTER)).isEqualTo(" AB  ")
    }

    @Test
    fun padToWidthLeavesTextThatFillsTheWidth() {
        assertThat(ReceiptLayout.padToWidth("ABCDE", 5, ReceiptAlignment.END)).isEqualTo("ABCDE")
    }

    @Test
    fun layoutLinesRendersStoreNameBoldAndCentered() {
        val receipt = ReceiptDocument(storeName = "Toko Sumber", lines = emptyList())

        val firstLine = ReceiptLayout.layoutLines(receipt, 32).first()

        assertThat(firstLine.isBold).isTrue()
        assertThat(firstLine.alignment).isEqualTo(ReceiptAlignment.CENTER)
        assertThat(firstLine.text).hasLength(32)
        assertThat(firstLine.text.trim()).isEqualTo("Toko Sumber")
    }

    @Test
    fun layoutLinesAppliesEachLineAlignmentAndPadding() {
        val receipt = ReceiptDocument(
            storeName = "",
            lines = listOf(
                ReceiptLine("Terima kasih", ReceiptAlignment.CENTER),
                ReceiptLine("Total", ReceiptAlignment.END, isBold = true),
            ),
        )

        val laidOutLines = ReceiptLayout.layoutLines(receipt, 32)

        assertThat(laidOutLines[0].text).isEqualTo(" ".repeat(10) + "Terima kasih" + " ".repeat(10))
        assertThat(laidOutLines[1].text).isEqualTo(" ".repeat(27) + "Total")
        assertThat(laidOutLines[1].isBold).isTrue()
    }

    @Test
    fun layoutLinesExpandsEmbeddedLineBreaks() {
        val receipt = ReceiptDocument(
            storeName = "",
            lines = listOf(ReceiptLine("Baris satu\nBaris dua", ReceiptAlignment.START)),
        )

        val laidOutLines = ReceiptLayout.layoutLines(receipt, 32)

        assertThat(laidOutLines).hasSize(2)
        assertThat(laidOutLines[0].text.trim()).isEqualTo("Baris satu")
        assertThat(laidOutLines[1].text.trim()).isEqualTo("Baris dua")
    }

    @Test
    fun renderTextPadsEveryLineToTheRequestedWidth() {
        val receipt = longReceipt()
        val widthAt58 = ReceiptPaperWidth.MILLIMETER_58.characters
        val widthAt80 = ReceiptPaperWidth.MILLIMETER_80.characters

        val renderedAt32 = ReceiptLayout.renderText(receipt, widthAt58)
        val renderedAt48 = ReceiptLayout.renderText(receipt, widthAt80)

        assertThat(widthAt58).isEqualTo(32)
        assertThat(widthAt80).isEqualTo(48)
        assertThat(renderedAt32.split('\n')).isNotEmpty()
        assertThat(renderedAt32.split('\n').all { it.length == widthAt58 }).isTrue()
        assertThat(renderedAt48.split('\n').all { it.length == widthAt80 }).isTrue()
        assertThat(renderedAt32.split('\n').size).isGreaterThan(renderedAt48.split('\n').size)
    }

    private fun longReceipt(): ReceiptDocument = ReceiptDocument(
        storeName = "Toko Sumber Rejeki Makmur",
        lines = listOf(
            ReceiptLine("Alamat Jalan Merdeka Nomor 123 Jakarta Pusat", ReceiptAlignment.START),
            ReceiptLine("Total", ReceiptAlignment.END, isBold = true),
            ReceiptLine("Terima kasih sudah berbelanja", ReceiptAlignment.CENTER),
        ),
    )
}
