package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.BackupArtifact
import kotlinx.coroutines.CancellationException
import java.util.TimeZone

class XlsxReportExporter(
    private val binarySink: BinarySink,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {

    suspend fun exportReport(
        content: ReportContent,
        targetPath: String,
    ): AppResult<BackupArtifact> {
        if (targetPath.isBlank()) {
            return AppResult.Failure(AppError.Validation("export path must not be blank"))
        }

        val workbook = ReportXlsx.buildWorkbook(content, timeZone)

        val byteCount = try {
            binarySink.write(targetPath, workbook)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return AppResult.Failure(AppError.Storage(error.message ?: "failed to write export file"))
        }

        return AppResult.Success(BackupArtifact(path = targetPath, byteCount = byteCount))
    }
}
