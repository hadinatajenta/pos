package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.TimeZone

class ReportRangeFactoryTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val noonOnFifteenthOfJanuary2024 = 1_705_320_000_000L

    @Test
    fun todayCoversTheCurrentDay() {
        val range = ReportRangeFactory(FixedClock(noonOnFifteenthOfJanuary2024), utc).today()

        assertThat(range.startMillis).isEqualTo(1_705_276_800_000L)
        assertThat(range.endMillis).isEqualTo(1_705_363_199_999L)
    }

    @Test
    fun lastSevenDaysSpansInclusiveOfToday() {
        val range = ReportRangeFactory(FixedClock(noonOnFifteenthOfJanuary2024), utc).lastDays(7)

        assertThat(range.startMillis).isEqualTo(1_704_758_400_000L)
        assertThat(range.endMillis).isEqualTo(1_705_363_199_999L)
    }

    @Test
    fun lastDaysWithZeroStillCoversToday() {
        val range = ReportRangeFactory(FixedClock(noonOnFifteenthOfJanuary2024), utc).lastDays(0)

        assertThat(range.startMillis).isEqualTo(1_705_276_800_000L)
        assertThat(range.endMillis).isEqualTo(1_705_363_199_999L)
    }

    @Test
    fun forEpochDayZeroIsTheUnixEpoch() {
        val range = ReportRangeFactory(FixedClock(0L), utc).forEpochDay(0L)

        assertThat(range.startMillis).isEqualTo(0L)
        assertThat(range.endMillis).isEqualTo(86_399_999L)
    }

    @Test
    fun forEpochDayRangeOrdersReversedInput() {
        val factory = ReportRangeFactory(FixedClock(0L), utc)

        assertThat(factory.forEpochDayRange(12L, 10L)).isEqualTo(factory.forEpochDayRange(10L, 12L))
    }

    @Test
    fun thisMonthStartsOnTheFirstDay() {
        val range = ReportRangeFactory(FixedClock(noonOnFifteenthOfJanuary2024), utc).thisMonth()

        assertThat(range.startMillis).isEqualTo(1_704_067_200_000L)
    }

    @Test
    fun thisYearStartsOnJanuaryFirst() {
        val range = ReportRangeFactory(FixedClock(noonOnFifteenthOfJanuary2024), utc).thisYear()

        assertThat(range.startMillis).isEqualTo(1_704_067_200_000L)
    }

    @Test
    fun allTimeStartsAtEpochAndEndsNow() {
        val range = ReportRangeFactory(FixedClock(noonOnFifteenthOfJanuary2024), utc).allTime()

        assertThat(range.startMillis).isEqualTo(0L)
        assertThat(range.endMillis).isEqualTo(noonOnFifteenthOfJanuary2024)
    }
}
