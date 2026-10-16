package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.TimeZone

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

        val result = exporter.exportReport(content(), "/exports/laporan.xlsx")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val artifact = (result as AppResult.Success<BackupArtifact>).value
        assertThat(artifact.path).isEqualTo("/exports/laporan.xlsx")
        assertThat(sink.writtenPath).isEqualTo("/exports/laporan.xlsx")
        assertThat(sink.writtenBytes).isNotNull()
        assertThat(artifact.byteCount).isEqualTo(sink.writtenBytes!!.size.toLong())
        assertThat(sink.writtenBytes!!.size).isGreaterThan(0)
    }

    @Test
    fun rejectsBlankTargetPath() = runTest {
        val sink = RecordingBinarySink()
        val exporter = XlsxReportExporter(sink, utc)

        val result = exporter.exportReport(content(), "  ")

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(sink.writtenBytes).isNull()
    }

    @Test
    fun mapsSinkWriteFailureToStorageError() = runTest {
        val exporter = XlsxReportExporter(RecordingBinarySink(failWrite = true), utc)

        val result = exporter.exportReport(content(), "/exports/laporan.xlsx")

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Storage::class.java)
    }
}
