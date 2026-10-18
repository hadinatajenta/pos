package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
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
            strings = IdStrings,
        )

        assertThat(label).isEqualTo("Hari ini")
    }

    @Test
    fun sevenDayPresetLabelsSevenHariTerakhir() {
        val label = periodLabel(
            preset = ReportsRangePreset.LAST_7_DAYS,
            range = rangeFor(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 17)),
            timeZone = jakarta,
            strings = IdStrings,
        )

        assertThat(label).isEqualTo("7 hari terakhir")
    }

    @Test
    fun twentyEightDayPresetLabelsTwentyEightHariTerakhir() {
        val label = periodLabel(
            preset = ReportsRangePreset.LAST_28_DAYS,
            range = rangeFor(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 17)),
            timeZone = jakarta,
            strings = IdStrings,
        )

        assertThat(label).isEqualTo("28 hari terakhir")
    }

    @Test
    fun customPresetShowsStartAndEndShortDates() {
        val label = periodLabel(
            preset = ReportsRangePreset.CUSTOM,
            range = rangeFor(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 20)),
            timeZone = jakarta,
            strings = IdStrings,
        )

        assertThat(label).isEqualTo("17 Okt 2026 - 20 Okt 2026")
    }

    @Test
    fun presetsFollowTheSelectedCatalogue() {
        assertThat(
            periodLabel(
                preset = ReportsRangePreset.TODAY,
                range = rangeFor(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17)),
                timeZone = jakarta,
                strings = EnStrings,
            ),
        ).isEqualTo("Today")
        assertThat(
            periodLabel(
                preset = ReportsRangePreset.LAST_7_DAYS,
                range = rangeFor(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 17)),
                timeZone = jakarta,
                strings = EnStrings,
            ),
        ).isEqualTo("Last 7 days")
        assertThat(
            periodLabel(
                preset = ReportsRangePreset.LAST_28_DAYS,
                range = rangeFor(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 17)),
                timeZone = jakarta,
                strings = EnStrings,
            ),
        ).isEqualTo("Last 28 days")
        assertThat(
            periodLabel(
                preset = ReportsRangePreset.TODAY,
                range = rangeFor(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17)),
                timeZone = jakarta,
                strings = ZhStrings,
            ),
        ).isEqualTo("今日")
        assertThat(
            periodLabel(
                preset = ReportsRangePreset.LAST_7_DAYS,
                range = rangeFor(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 17)),
                timeZone = jakarta,
                strings = ZhStrings,
            ),
        ).isEqualTo("近 7 天")
        assertThat(
            periodLabel(
                preset = ReportsRangePreset.LAST_28_DAYS,
                range = rangeFor(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 17)),
                timeZone = jakarta,
                strings = ZhStrings,
            ),
        ).isEqualTo("近 28 天")
    }

    @Test
    fun customPresetLabelsFollowTheSelectedCatalogue() {
        val range = rangeFor(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 20))

        assertThat(
            periodLabel(
                preset = ReportsRangePreset.CUSTOM,
                range = range,
                timeZone = jakarta,
                strings = EnStrings,
            ),
        ).isEqualTo("17 Oct 2026 - 20 Oct 2026")
        assertThat(
            periodLabel(
                preset = ReportsRangePreset.CUSTOM,
                range = range,
                timeZone = jakarta,
                strings = ZhStrings,
            ),
        ).isEqualTo("17 10月 2026 - 20 10月 2026")
    }

    private fun rangeFor(start: LocalDate, end: LocalDate): ReportRange = ReportRange(
        startMillis = start.atStartOfDay(jakartaZone).toInstant().toEpochMilli(),
        endMillis = end.plusDays(1).atStartOfDay(jakartaZone).toInstant().toEpochMilli() - 1,
    )
}
