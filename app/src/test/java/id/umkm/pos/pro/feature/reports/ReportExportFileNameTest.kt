package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.TimeZone

class ReportExportFileNameTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val jakarta = TimeZone.getTimeZone("Asia/Jakarta")

    @Test
    fun forRangeUsesBothRangeDatesAndTheFormatExtension() {
        val octoberRange = ReportRange(
            startMillis = 20_727L * ReportDates.MILLIS_PER_DAY,
            endMillis = 20_743L * ReportDates.MILLIS_PER_DAY - 1,
        )

        assertThat(ReportExportFileName.forRange(octoberRange, ReportExportFormat.CSV, utc, IdStrings))
            .isEqualTo("laporan-2026-10-01_2026-10-16.csv")
        assertThat(ReportExportFileName.forRange(octoberRange, ReportExportFormat.XLSX, utc, IdStrings))
            .isEqualTo("laporan-2026-10-01_2026-10-16.xlsx")
    }

    @Test
    fun forRangeResolvesDatesInTheProvidedTimeZone() {
        val eveningInUtc = ReportRange(
            startMillis = 19_737L * ReportDates.MILLIS_PER_DAY + 18L * 3_600_000L,
            endMillis = 19_737L * ReportDates.MILLIS_PER_DAY + 18L * 3_600_000L,
        )

        assertThat(ReportExportFileName.forRange(eveningInUtc, ReportExportFormat.CSV, utc, IdStrings))
            .isEqualTo("laporan-2024-01-15_2024-01-15.csv")
        assertThat(ReportExportFileName.forRange(eveningInUtc, ReportExportFormat.CSV, jakarta, IdStrings))
            .isEqualTo("laporan-2024-01-16_2024-01-16.csv")
    }

    @Test
    fun forRangePrefixFollowsTheSelectedCatalogue() {
        val octoberRange = ReportRange(
            startMillis = 20_727L * ReportDates.MILLIS_PER_DAY,
            endMillis = 20_743L * ReportDates.MILLIS_PER_DAY - 1,
        )

        assertThat(ReportExportFileName.forRange(octoberRange, ReportExportFormat.CSV, utc, EnStrings))
            .isEqualTo("report-2026-10-01_2026-10-16.csv")
        assertThat(ReportExportFileName.forRange(octoberRange, ReportExportFormat.XLSX, utc, EnStrings))
            .isEqualTo("report-2026-10-01_2026-10-16.xlsx")
        assertThat(ReportExportFileName.forRange(octoberRange, ReportExportFormat.CSV, utc, ZhStrings))
            .isEqualTo("报表-2026-10-01_2026-10-16.csv")
        assertThat(ReportExportFileName.forRange(octoberRange, ReportExportFormat.XLSX, utc, ZhStrings))
            .isEqualTo("报表-2026-10-01_2026-10-16.xlsx")
    }

    @Test
    fun normalizeTrimsAndDropsDirectorySegments() {
        assertThat(ReportExportFileName.normalize("  /tmp/exports/laporan.csv  ")).isEqualTo("laporan.csv")
        assertThat(ReportExportFileName.normalize("laporan.xlsx")).isEqualTo("laporan.xlsx")
    }

    @Test
    fun normalizeRejectsBlankNames() {
        assertThrows(IllegalArgumentException::class.java) {
            ReportExportFileName.normalize("   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReportExportFileName.normalize("")
        }
    }

    @Test
    fun mimeTypeFollowsTheFileExtension() {
        assertThat(ReportExportFileName.mimeTypeFor("laporan-2026-10-01_2026-10-16.csv"))
            .isEqualTo("text/csv")
        assertThat(ReportExportFileName.mimeTypeFor("laporan-2026-10-01_2026-10-16.XLSX"))
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        assertThat(ReportExportFileName.mimeTypeFor("laporan"))
            .isEqualTo("application/octet-stream")
    }
}
