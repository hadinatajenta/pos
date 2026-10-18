package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import org.junit.Test
import java.util.TimeZone

class ReportCsvTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun transactionsCsvStartsWithHeaderRow() {
        val csv = ReportCsv.buildTransactionsCsv(emptyList(), utc)
        assertThat(csv).isEqualTo(ReportCsv.TRANSACTION_COLUMNS.joinToString(",") + "\r\n")
    }

    @Test
    fun transactionsCsvWritesOneRowPerTransactionWithAllValues() {
        val csv = ReportCsv.buildTransactionsCsv(listOf(sampleTransaction()), utc)
        val lines = csv.split("\r\n")
        assertThat(lines).hasSize(3)
        assertThat(lines[0]).isEqualTo(ReportCsv.TRANSACTION_COLUMNS.joinToString(","))
        assertThat(lines[1]).isEqualTo(
            "INV-1,2024-01-15 00:00:00,CASH,COMPLETED," +
                "15000,0,0,0,0,20000,10000,5000,20000,0,0",
        )
        assertThat(lines[2]).isEmpty()
    }

    @Test
    fun transactionsCsvQuotesFieldsThatContainSeparators() {
        val csv = ReportCsv.buildTransactionsCsv(listOf(sampleTransaction(invoiceNo = "INV,1")), utc)
        val dataRow = csv.split("\r\n")[1]
        assertThat(dataRow).startsWith("\"INV,1\",2024-01-15 00:00:00")
    }

    @Test
    fun dailySalesCsvWritesHeaderAndRows() {
        val csv = ReportCsv.buildDailySalesCsv(
            listOf(DailySales(epochDay = 19_737L, totalSales = 150_000L, netProfit = 50_000L, transactionCount = 3)),
        )
        assertThat(csv).isEqualTo(
            "date,total_sales,net_profit,transaction_count\r\n" +
                "2024-01-15,150000,50000,3\r\n",
        )
    }

    @Test
    fun escapeFieldLeavesPlainValuesUntouched() {
        assertThat(ReportCsv.escapeField("plain")).isEqualTo("plain")
        assertThat(ReportCsv.escapeField("12345")).isEqualTo("12345")
    }

    @Test
    fun escapeFieldQuotesCommasQuotesAndLineBreaks() {
        assertThat(ReportCsv.escapeField("a,b")).isEqualTo("\"a,b\"")
        assertThat(ReportCsv.escapeField("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"")
        assertThat(ReportCsv.escapeField("line\nbreak")).isEqualTo("\"line\nbreak\"")
        assertThat(ReportCsv.escapeField("carriage\rreturn")).isEqualTo("\"carriage\rreturn\"")
    }

    @Test
    fun escapeFieldQuotesValuesWithSurroundingWhitespace() {
        assertThat(ReportCsv.escapeField(" padded ")).isEqualTo("\" padded \"")
    }

    @Test
    fun formatsTimestampInRequestedTimeZone() {
        assertThat(ReportDates.formatTimestamp(1_705_320_000_000L, utc)).isEqualTo("2024-01-15 12:00:00")
    }

    @Test
    fun formatsEpochDayAsCalendarDate() {
        assertThat(ReportDates.formatEpochDay(19_737L)).isEqualTo("2024-01-15")
    }

    @Test
    fun reportCsvWritesTransactionTableThenProductSummaryTable() {
        val content = ReportContent(
            transactions = listOf(sampleTransaction(invoiceNo = "INV-1")),
            productSummaries = listOf(
                ReportProductSummary(productId = 10L, name = "Kopi Susu", quantitySold = 3.0, salesValue = 45_000L),
            ),
        )

        val csv = ReportCsv.buildReportCsv(content, IdStrings, utc)
        val lines = csv.split("\r\n")

        assertThat(lines[0]).isEqualTo(ReportCsv.reportTransactionColumns(IdStrings).joinToString(","))
        assertThat(lines[1]).isEqualTo("2024-01-15 00:00:00,INV-1,20000,Tunai,10000,5000")
        assertThat(lines[2]).isEmpty()
        assertThat(lines[3]).isEqualTo(ReportCsv.productSummaryColumns(IdStrings).joinToString(","))
        assertThat(lines[4]).isEqualTo("Kopi Susu,3,45000")
    }

    @Test
    fun reportCsvQuotesProductNamesThatContainSeparators() {
        val content = ReportContent(
            transactions = emptyList(),
            productSummaries = listOf(
                ReportProductSummary(productId = 10L, name = "Kopi, Susu", quantitySold = 2.5, salesValue = 30_000L),
            ),
        )

        val csv = ReportCsv.buildReportCsv(content, IdStrings, utc)

        assertThat(csv).contains("\"Kopi, Susu\",2.5,30000")
    }

    @Test
    fun reportCsvWithNoRowsStillWritesBothHeaders() {
        val csv = ReportCsv.buildReportCsv(ReportContent(emptyList(), emptyList()), IdStrings, utc)

        assertThat(csv).isEqualTo(
            ReportCsv.reportTransactionColumns(IdStrings).joinToString(",") + "\r\n" +
                "\r\n" +
                ReportCsv.productSummaryColumns(IdStrings).joinToString(",") + "\r\n",
        )
    }

    @Test
    fun reportColumnsFollowTheSelectedCatalogue() {
        assertThat(ReportCsv.reportTransactionColumns(IdStrings)).containsExactly(
            "Tanggal", "No. Transaksi", "Total Penjualan", "Metode Pembayaran", "HPP", "Laba",
        ).inOrder()
        assertThat(ReportCsv.reportTransactionColumns(EnStrings)).containsExactly(
            "Date", "Transaction No.", "Total Sales", "Payment Method", "COGS", "Profit",
        ).inOrder()
        assertThat(ReportCsv.reportTransactionColumns(ZhStrings)).containsExactly(
            "日期", "交易单号", "销售总额", "收款方式", "商品成本", "利润",
        ).inOrder()
        assertThat(ReportCsv.productSummaryColumns(IdStrings))
            .containsExactly("Nama Produk", "Qty Terjual", "Nilai Penjualan").inOrder()
        assertThat(ReportCsv.productSummaryColumns(EnStrings))
            .containsExactly("Product Name", "Qty Sold", "Sales Value").inOrder()
        assertThat(ReportCsv.productSummaryColumns(ZhStrings))
            .containsExactly("商品名称", "销售数量", "销售金额").inOrder()
    }

    @Test
    fun reportCsvUsesCatalogueLabelsForHeadersAndPaymentMethod() {
        val content = ReportContent(
            transactions = listOf(sampleTransaction(invoiceNo = "INV-1")),
            productSummaries = listOf(
                ReportProductSummary(productId = 10L, name = "Kopi Susu", quantitySold = 3.0, salesValue = 45_000L),
            ),
        )

        val englishLines = ReportCsv.buildReportCsv(content, EnStrings, utc).split("\r\n")
        assertThat(englishLines[0]).isEqualTo("Date,Transaction No.,Total Sales,Payment Method,COGS,Profit")
        assertThat(englishLines[1]).isEqualTo("2024-01-15 00:00:00,INV-1,20000,Cash,10000,5000")
        assertThat(englishLines[3]).isEqualTo("Product Name,Qty Sold,Sales Value")

        val mandarinLines = ReportCsv.buildReportCsv(content, ZhStrings, utc).split("\r\n")
        assertThat(mandarinLines[0]).isEqualTo("日期,交易单号,销售总额,收款方式,商品成本,利润")
        assertThat(mandarinLines[1]).isEqualTo("2024-01-15 00:00:00,INV-1,20000,现金,10000,5000")
        assertThat(mandarinLines[3]).isEqualTo("商品名称,销售数量,销售金额")
    }
}
