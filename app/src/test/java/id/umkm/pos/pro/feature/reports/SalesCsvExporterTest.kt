package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.core.model.ReportRange
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.TimeZone

class SalesCsvExporterTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val range = ReportRange(1_705_276_800_000L, 1_705_363_199_999L)

    @Test
    fun writesCsvToSinkAndReturnsArtifact() = runTest {
        val sink = RecordingCsvSink()
        val exporter = SalesCsvExporter(
            reportRepository = FakeReportRepository(transactions = listOf(sampleTransaction())),
            csvSink = sink,
            timeZone = utc,
        )

        val result = exporter.exportTransactions(range, "/exports/report.csv")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val artifact = (result as AppResult.Success<BackupArtifact>).value
        assertThat(artifact.path).isEqualTo("/exports/report.csv")
        assertThat(sink.writtenPath).isEqualTo("/exports/report.csv")
        assertThat(sink.writtenContent).isEqualTo(
            ReportCsv.buildTransactionsCsv(listOf(sampleTransaction()), utc),
        )
        assertThat(artifact.byteCount).isEqualTo(sink.writtenContent!!.toByteArray(Charsets.UTF_8).size.toLong())
    }

    @Test
    fun writesHeaderOnlyWhenRangeHasNoTransactions() = runTest {
        val sink = RecordingCsvSink()
        val exporter = SalesCsvExporter(FakeReportRepository(), sink, utc)

        val result = exporter.exportTransactions(range, "/exports/empty.csv")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat(sink.writtenContent).isEqualTo(
            ReportCsv.TRANSACTION_COLUMNS.joinToString(",") + "\r\n",
        )
    }

    @Test
    fun rejectsBlankTargetPath() = runTest {
        val sink = RecordingCsvSink()
        val exporter = SalesCsvExporter(FakeReportRepository(), sink, utc)

        val result = exporter.exportTransactions(range, "   ")

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(sink.writtenContent).isNull()
    }

    @Test
    fun rejectsInvertedRange() = runTest {
        val exporter = SalesCsvExporter(FakeReportRepository(), RecordingCsvSink(), utc)

        val result = exporter.exportTransactions(ReportRange(200L, 100L), "/exports/report.csv")

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun mapsRepositoryReadFailureToStorageError() = runTest {
        val exporter = SalesCsvExporter(FakeReportRepository(failRead = true), RecordingCsvSink(), utc)

        val result = exporter.exportTransactions(range, "/exports/report.csv")

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Storage::class.java)
    }

    @Test
    fun mapsSinkWriteFailureToStorageError() = runTest {
        val exporter = SalesCsvExporter(FakeReportRepository(), RecordingCsvSink(failWrite = true), utc)

        val result = exporter.exportTransactions(range, "/exports/report.csv")

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Storage::class.java)
    }

    @Test
    fun exportReportWritesTransactionsAndProductSummary() = runTest {
        val sink = RecordingCsvSink()
        val exporter = SalesCsvExporter(FakeReportRepository(), sink, utc)
        val content = ReportContent(
            transactions = listOf(sampleTransaction()),
            productSummaries = listOf(
                ReportProductSummary(productId = 10L, name = "Kopi Susu", quantitySold = 3.0, salesValue = 45_000L),
            ),
        )

        val result = exporter.exportReport(content, "/exports/laporan.csv")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat(sink.writtenContent).isEqualTo(ReportCsv.buildReportCsv(content, utc))
        assertThat((result as AppResult.Success<BackupArtifact>).value.path).isEqualTo("/exports/laporan.csv")
    }

    @Test
    fun exportReportRejectsBlankTargetPath() = runTest {
        val sink = RecordingCsvSink()
        val exporter = SalesCsvExporter(FakeReportRepository(), sink, utc)

        val result = exporter.exportReport(ReportContent(emptyList(), emptyList()), " ")

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(sink.writtenContent).isNull()
    }
}
