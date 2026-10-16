package id.umkm.pos.pro.domain.pos

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyRoundingTest {

    @Test
    fun roundHalfUpRoundsAtHalf() {
        assertThat(roundHalfUp(2.5)).isEqualTo(3L)
        assertThat(roundHalfUp(2.49)).isEqualTo(2L)
        assertThat(roundHalfUp(2.0)).isEqualTo(2L)
        assertThat(roundHalfUp(0.0)).isEqualTo(0L)
    }

    @Test
    fun roundHalfUpRoundsTiesUpward() {
        assertThat(roundHalfUp(0.5)).isEqualTo(1L)
        assertThat(roundHalfUp(1.5)).isEqualTo(2L)
    }

    @Test
    fun floorToWholeDropsFraction() {
        assertThat(floorToWhole(199.99)).isEqualTo(199L)
        assertThat(floorToWhole(200.0)).isEqualTo(200L)
        assertThat(floorToWhole(0.0)).isEqualTo(0L)
    }
}
