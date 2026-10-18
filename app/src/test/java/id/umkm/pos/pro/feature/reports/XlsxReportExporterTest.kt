package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.TimeZone
import java.util.zip.ZipInputStream

class XlsxReportExporterTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun content(): ReportContent = ReportContent(
        transactions = listOf(sampleTransaction()),
        productSummaries = listOf(
            ReportProductSummary(productId = 10L, name = "Kopi Susu", quantitySold = 3.0, salesValue = 45_000L),
        ),
    )

    @Test
    fun writesWorkbookToSinkAndReturnsArtifact() = runTest {
        val sink = RecordingBinarySink()
        val exporter = XlsxReportExporter(sink, utc)

        val result = exporter.exportReport(content(), "/exports/laporan.xlsx", IdStrings)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val artifact = (result as AppResult.Success<BackupArtifact>).value
        assertThat(artifact.path).isEqualTo("/exports/laporan.xlsx")
        assertThat(sink.writtenPath).isEqualTo("/exports/laporan.xlsx")
        assertThat(sink.writtenBytes).isNotNull()
        assertThat(artifact.byteCount).isEqualTo(sink.writtenBytes!!.size.toLong())
        assertThat(sink.writtenBytes!!.size).isGreaterThan(0)
    }

    @Test
    fun writesWorkbookFromTheProvidedCatalogue() = runTest {
        val sink = RecordingBinarySink()
        val exporter = XlsxReportExporter(sink, utc)

        val result = exporter.exportReport(content(), "/exports/report.xlsx", EnStrings)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val workbook = readEntry(requireNotNull(sink.writtenBytes), "xl/workbook.xml")
        assertThat(workbook).contains("name=\"Transactions\"")
        assertThat(workbook).contains("name=\"Product Summary\"")
        assertThat(readEntry(sink.writtenBytes!!, "xl/worksheets/sheet1.xml"))
            .contains("<t xml:space=\"preserve\">Transaction No.</t>")
    }

    @Test
    fun rejectsBlankTargetPath() = runTest {
        val sink = RecordingBinarySink()
        val exporter = XlsxReportExporter(sink, utc)

        val result = exporter.exportReport(content(), "  ", IdStrings)

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(sink.writtenBytes).isNull()
    }

    @Test
    fun mapsSinkWriteFailureToStorageError() = runTest {
        val exporter = XlsxReportExporter(RecordingBinarySink(failWrite = true), utc)

        val result = exporter.exportReport(content(), "/exports/laporan.xlsx", IdStrings)

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Storage::class.java)
    }

    private fun readEntry(bytes: ByteArray, entryName: String): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        error("workbook entry $entryName is missing")
    }
}
