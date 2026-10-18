package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import id.umkm.pos.pro.core.contract.ReportRepository
import id.umkm.pos.pro.data.file.CsvSink
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.ui.common.PosStrings
import kotlinx.coroutines.CancellationException
import java.util.TimeZone

class SalesCsvExporter(
    private val reportRepository: ReportRepository,
    private val csvSink: CsvSink,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {

    suspend fun exportTransactions(
        range: ReportRange,
        targetPath: String,
    ): AppResult<BackupArtifact> {
        if (targetPath.isBlank()) {
            return AppResult.Failure(AppError.Validation("export path must not be blank"))
        }
        if (range.startMillis > range.endMillis) {
            return AppResult.Failure(AppError.Validation("start date must not be after end date"))
        }

        val transactions = try {
            reportRepository.transactionsInRange(range)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return AppResult.Failure(AppError.Storage(error.message ?: "failed to read transactions"))
        }

        val csv = ReportCsv.buildTransactionsCsv(transactions, timeZone)

        val byteCount = try {
            csvSink.write(targetPath, csv)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return AppResult.Failure(AppError.Storage(error.message ?: "failed to write export file"))
        }

        return AppResult.Success(BackupArtifact(path = targetPath, byteCount = byteCount))
    }

    suspend fun exportReport(
        content: ReportContent,
        targetPath: String,
        strings: PosStrings,
    ): AppResult<BackupArtifact> {
        if (targetPath.isBlank()) {
            return AppResult.Failure(AppError.Validation("export path must not be blank"))
        }

        val csv = ReportCsv.buildReportCsv(content, strings, timeZone)

        val byteCount = try {
            csvSink.write(targetPath, csv)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return AppResult.Failure(AppError.Storage(error.message ?: "failed to write export file"))
        }

        return AppResult.Success(BackupArtifact(path = targetPath, byteCount = byteCount))
    }
}
