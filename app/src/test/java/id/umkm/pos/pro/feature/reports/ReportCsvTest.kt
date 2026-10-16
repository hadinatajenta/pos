package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.DailySales
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
}
