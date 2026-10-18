package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.EnStrings
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.ZhStrings
import org.junit.Test
import java.util.TimeZone

class ReportDatesTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun weekdayShortLabelMapsEpochDayToIndonesianWeekday() {
        assertThat(ReportDates.weekdayShortLabel(19_737L, IdStrings)).isEqualTo("Sen")
        assertThat(ReportDates.weekdayShortLabel(19_738L, IdStrings)).isEqualTo("Sel")
        assertThat(ReportDates.weekdayShortLabel(19_742L, IdStrings)).isEqualTo("Sab")
        assertThat(ReportDates.weekdayShortLabel(19_743L, IdStrings)).isEqualTo("Min")
    }

    @Test
    fun weekdayShortLabelTreatsUnixEpochAsThursday() {
        assertThat(ReportDates.weekdayShortLabel(0L, IdStrings)).isEqualTo("Kam")
    }

    @Test
    fun weekdayShortLabelFollowsTheSelectedCatalogue() {
        assertThat(ReportDates.weekdayShortLabel(19_737L, EnStrings)).isEqualTo("Mon")
        assertThat(ReportDates.weekdayShortLabel(19_742L, EnStrings)).isEqualTo("Sat")
        assertThat(ReportDates.weekdayShortLabel(0L, EnStrings)).isEqualTo("Thu")
        assertThat(ReportDates.weekdayShortLabel(19_737L, ZhStrings)).isEqualTo("周一")
        assertThat(ReportDates.weekdayShortLabel(19_742L, ZhStrings)).isEqualTo("周六")
        assertThat(ReportDates.weekdayShortLabel(0L, ZhStrings)).isEqualTo("周四")
    }

    @Test
    fun formatEpochDayShortUsesIndonesianMonthLabels() {
        assertThat(ReportDates.formatEpochDayShort(19_737L, IdStrings)).isEqualTo("15 Jan 2024")
        assertThat(ReportDates.formatEpochDayShort(19_768L, IdStrings)).isEqualTo("15 Feb 2024")
    }

    @Test
    fun formatEpochDayShortFollowsTheSelectedCatalogue() {
        assertThat(ReportDates.formatEpochDayShort(19_950L, IdStrings)).isEqualTo("15 Agu 2024")
        assertThat(ReportDates.formatEpochDayShort(19_950L, EnStrings)).isEqualTo("15 Aug 2024")
        assertThat(ReportDates.formatEpochDayShort(19_950L, ZhStrings)).isEqualTo("15 8月 2024")
    }

    @Test
    fun formatLocalDateShortHonoursTheRequestedTimeZone() {
        val jakarta = TimeZone.getTimeZone("Asia/Jakarta")
        val eveningUtc = 19_737L * ReportDates.MILLIS_PER_DAY + 18L * 3_600_000L

        assertThat(ReportDates.formatLocalDateShort(eveningUtc, utc, IdStrings)).isEqualTo("15 Jan 2024")
        assertThat(ReportDates.formatLocalDateShort(eveningUtc, jakarta, IdStrings)).isEqualTo("16 Jan 2024")
    }

    @Test
    fun formatLocalDateTimeShortRendersTimeZoneAndCatalogueMonth() {
        val jakarta = TimeZone.getTimeZone("Asia/Jakarta")
        val noonUtcOnFifteenthOfAugust2024 = 1_723_723_200_000L

        assertThat(ReportDates.formatLocalDateTimeShort(noonUtcOnFifteenthOfAugust2024, utc, IdStrings))
            .isEqualTo("15 Agu 2024 12:00")
        assertThat(ReportDates.formatLocalDateTimeShort(noonUtcOnFifteenthOfAugust2024, jakarta, IdStrings))
            .isEqualTo("15 Agu 2024 19:00")
        assertThat(ReportDates.formatLocalDateTimeShort(noonUtcOnFifteenthOfAugust2024, utc, EnStrings))
            .isEqualTo("15 Aug 2024 12:00")
        assertThat(ReportDates.formatLocalDateTimeShort(noonUtcOnFifteenthOfAugust2024, utc, ZhStrings))
            .isEqualTo("15 8月 2024 12:00")
    }
}
