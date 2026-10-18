package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.TimeZone
import java.util.zip.ZipInputStream

class ReportXlsxTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun content(): ReportContent = ReportContent(
        transactions = listOf(
            sampleTransaction(id = 1L, invoiceNo = "INV-1", grandTotal = 20_000L, totalHpp = 10_000L, netProfit = 5_000L),
        ),
        productSummaries = listOf(
            ReportProductSummary(productId = 10L, name = "Kopi Susu", quantitySold = 3.0, salesValue = 45_000L),
        ),
    )

    @Test
    fun workbookContainsExactlyTheExpectedEntries() {
        val entries = readEntries(ReportXlsx.buildWorkbook(content(), IdStrings, utc))

        assertThat(entries.keys).containsExactly(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml",
        )
    }

    @Test
    fun transactionSheetHasHeaderAndKnownRow() {
        val sheet = readEntries(ReportXlsx.buildWorkbook(content(), IdStrings, utc)).getValue("xl/worksheets/sheet1.xml")

        assertThat(sheet).contains("<t xml:space=\"preserve\">Tanggal</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">No. Transaksi</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Total Penjualan</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Metode Pembayaran</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">HPP</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Laba</t>")

        assertThat(sheet).contains("<c r=\"A2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">2024-01-15 00:00:00</t></is></c>")
        assertThat(sheet).contains("<c r=\"B2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">INV-1</t></is></c>")
        assertThat(sheet).contains("<c r=\"C2\"><v>20000</v></c>")
        assertThat(sheet).contains("<c r=\"D2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Tunai</t></is></c>")
        assertThat(sheet).contains("<c r=\"E2\"><v>10000</v></c>")
        assertThat(sheet).contains("<c r=\"F2\"><v>5000</v></c>")
    }

    @Test
    fun productSheetHasHeaderAndRow() {
        val sheet = readEntries(ReportXlsx.buildWorkbook(content(), IdStrings, utc)).getValue("xl/worksheets/sheet2.xml")

        assertThat(sheet).contains("<t xml:space=\"preserve\">Nama Produk</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Qty Terjual</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Nilai Penjualan</t>")
        assertThat(sheet).contains("<c r=\"A2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Kopi Susu</t></is></c>")
        assertThat(sheet).contains("<c r=\"B2\"><v>3</v></c>")
        assertThat(sheet).contains("<c r=\"C2\"><v>45000</v></c>")
    }

    @Test
    fun workbookAndContentTypesReferenceBothSheets() {
        val entries = readEntries(ReportXlsx.buildWorkbook(content(), IdStrings, utc))

        assertThat(entries.getValue("[Content_Types].xml")).contains("/xl/worksheets/sheet1.xml")
        assertThat(entries.getValue("[Content_Types].xml")).contains("/xl/worksheets/sheet2.xml")

        val workbook = entries.getValue("xl/workbook.xml")
        assertThat(workbook).contains("name=\"Transaksi\"")
        assertThat(workbook).contains("name=\"Ringkasan Produk\"")

        val relationships = entries.getValue("xl/_rels/workbook.xml.rels")
        assertThat(relationships).contains("worksheets/sheet1.xml")
        assertThat(relationships).contains("worksheets/sheet2.xml")
    }

    @Test
    fun escapesXmlSpecialCharactersInInlineStrings() {
        val content = ReportContent(
            transactions = listOf(sampleTransaction(invoiceNo = "INV<&>\"'")),
            productSummaries = emptyList(),
        )

        val sheet = readEntries(ReportXlsx.buildWorkbook(content, IdStrings, utc)).getValue("xl/worksheets/sheet1.xml")

        assertThat(sheet).contains("INV&lt;&amp;&gt;&quot;&apos;")
    }

    @Test
    fun emptyContentStillProducesHeaderOnlySheets() {
        val entries = readEntries(ReportXlsx.buildWorkbook(ReportContent(emptyList(), emptyList()), IdStrings, utc))

        assertThat(entries.getValue("xl/worksheets/sheet1.xml")).contains("Tanggal")
        assertThat(entries.getValue("xl/worksheets/sheet2.xml")).contains("Nama Produk")
    }

    @Test
    fun workbookSheetNamesAndHeadersFollowTheSelectedCatalogue() {
        val englishEntries = readEntries(ReportXlsx.buildWorkbook(content(), EnStrings, utc))

        assertThat(englishEntries.getValue("xl/workbook.xml")).contains("name=\"Transactions\"")
        assertThat(englishEntries.getValue("xl/workbook.xml")).contains("name=\"Product Summary\"")
        val englishTransactionSheet = englishEntries.getValue("xl/worksheets/sheet1.xml")
        assertThat(englishTransactionSheet).contains("<t xml:space=\"preserve\">Date</t>")
        assertThat(englishTransactionSheet).contains("<t xml:space=\"preserve\">Transaction No.</t>")
        assertThat(englishTransactionSheet).contains("<t xml:space=\"preserve\">COGS</t>")
        assertThat(englishTransactionSheet).contains("<c r=\"D2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Cash</t></is></c>")
        assertThat(englishEntries.getValue("xl/worksheets/sheet2.xml"))
            .contains("<t xml:space=\"preserve\">Qty Sold</t>")

        val mandarinEntries = readEntries(ReportXlsx.buildWorkbook(content(), ZhStrings, utc))

        assertThat(mandarinEntries.getValue("xl/workbook.xml")).contains("name=\"交易明细\"")
        assertThat(mandarinEntries.getValue("xl/workbook.xml")).contains("name=\"商品汇总\"")
        val mandarinTransactionSheet = mandarinEntries.getValue("xl/worksheets/sheet1.xml")
        assertThat(mandarinTransactionSheet).contains("<t xml:space=\"preserve\">日期</t>")
        assertThat(mandarinTransactionSheet).contains("<t xml:space=\"preserve\">商品成本</t>")
        assertThat(mandarinTransactionSheet).contains("<c r=\"D2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">现金</t></is></c>")
        assertThat(mandarinEntries.getValue("xl/worksheets/sheet2.xml"))
            .contains("<t xml:space=\"preserve\">销售数量</t>")
    }

    private fun readEntries(bytes: ByteArray): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }
}
