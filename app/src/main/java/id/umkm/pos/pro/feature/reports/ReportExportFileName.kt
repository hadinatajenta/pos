package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.ui.common.PosStrings
import java.io.File
import java.util.Locale
import java.util.TimeZone

object ReportExportFileName {

    private const val CSV_MIME_TYPE = "text/csv"
    private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val DEFAULT_MIME_TYPE = "application/octet-stream"

    fun forRange(
        range: ReportRange,
        format: ReportExportFormat,
        timeZone: TimeZone,
        strings: PosStrings,
    ): String {
        val startDate = ReportDates.formatEpochDay(
            ReportDates.epochDayForLocalMillis(range.startMillis, timeZone),
        )
        val endDate = ReportDates.formatEpochDay(
            ReportDates.epochDayForLocalMillis(range.endMillis, timeZone),
        )
        return "${strings.exportFilePrefix}-${startDate}_$endDate.${format.fileExtension}"
    }

    fun normalize(requestedName: String): String {
        val fileName = File(requestedName.trim()).name
        require(fileName.isNotBlank()) { "export file name must not be blank" }
        return fileName
    }

    fun mimeTypeFor(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
            "csv" -> CSV_MIME_TYPE
            "xlsx" -> XLSX_MIME_TYPE
            else -> DEFAULT_MIME_TYPE
        }
}
