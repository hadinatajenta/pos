package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.ReportRange
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class ReportPeriodLabelTest {

    private val jakarta: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    private val jakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")

    @Test
    fun todayPresetLabelsHariIni() {
        val label = periodLabel(
            preset = ReportsRangePreset.TODAY,
            range = rangeFor(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17)),
            timeZone = jakarta,
        )

        assertThat(label).isEqualTo("Hari ini")
    }

    @Test
    fun sevenDayPresetLabelsSevenHariTerakhir() {
        val label = periodLabel(
            preset = ReportsRangePreset.LAST_7_DAYS,
            range = rangeFor(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 17)),
            timeZone = jakarta,
        )

        assertThat(label).isEqualTo("7 hari terakhir")
    }

    @Test
    fun twentyEightDayPresetLabelsTwentyEightHariTerakhir() {
        val label = periodLabel(
            preset = ReportsRangePreset.LAST_28_DAYS,
            range = rangeFor(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 17)),
            timeZone = jakarta,
        )

        assertThat(label).isEqualTo("28 hari terakhir")
    }

    @Test
    fun customPresetShowsStartAndEndShortDates() {
        val label = periodLabel(
            preset = ReportsRangePreset.CUSTOM,
            range = rangeFor(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 20)),
            timeZone = jakarta,
        )

        assertThat(label).isEqualTo("17 Okt 2026 - 20 Okt 2026")
    }

    private fun rangeFor(start: LocalDate, end: LocalDate): ReportRange = ReportRange(
        startMillis = start.atStartOfDay(jakartaZone).toInstant().toEpochMilli(),
        endMillis = end.plusDays(1).atStartOfDay(jakartaZone).toInstant().toEpochMilli() - 1,
    )
}
