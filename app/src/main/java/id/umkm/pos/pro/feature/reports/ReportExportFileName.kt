package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.data.file.FileNames
import id.umkm.pos.pro.ui.common.PosStrings
import java.util.TimeZone

object ReportExportFileName {

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

    fun normalize(requestedName: String): String = FileNames.normalized(requestedName)

    fun mimeTypeFor(fileName: String): String = FileNames.mimeTypeFor(fileName)
}
