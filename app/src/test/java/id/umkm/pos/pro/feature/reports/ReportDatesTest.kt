package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.TimeZone

class ReportDatesTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun weekdayShortLabelMapsEpochDayToIndonesianWeekday() {
        assertThat(ReportDates.weekdayShortLabel(19_737L)).isEqualTo("Sen")
        assertThat(ReportDates.weekdayShortLabel(19_738L)).isEqualTo("Sel")
        assertThat(ReportDates.weekdayShortLabel(19_742L)).isEqualTo("Sab")
        assertThat(ReportDates.weekdayShortLabel(19_743L)).isEqualTo("Min")
    }

    @Test
    fun weekdayShortLabelTreatsUnixEpochAsThursday() {
        assertThat(ReportDates.weekdayShortLabel(0L)).isEqualTo("Kam")
    }

    @Test
    fun formatEpochDayShortUsesIndonesianMonthLabels() {
        assertThat(ReportDates.formatEpochDayShort(19_737L)).isEqualTo("15 Jan 2024")
        assertThat(ReportDates.formatEpochDayShort(19_768L)).isEqualTo("15 Feb 2024")
    }

    @Test
    fun formatLocalDateShortHonoursTheRequestedTimeZone() {
        val jakarta = TimeZone.getTimeZone("Asia/Jakarta")
        val eveningUtc = 19_737L * ReportDates.MILLIS_PER_DAY + 18L * 3_600_000L

        assertThat(ReportDates.formatLocalDateShort(eveningUtc, utc)).isEqualTo("15 Jan 2024")
        assertThat(ReportDates.formatLocalDateShort(eveningUtc, jakarta)).isEqualTo("16 Jan 2024")
    }
}
