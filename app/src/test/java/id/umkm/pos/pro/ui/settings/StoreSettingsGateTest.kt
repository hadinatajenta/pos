package id.umkm.pos.pro.ui.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StoreSettingsGateTest {

    @Test
    fun anUnchangedTaxAndRoundingNeedNoPin() {
        assertThat(
            requiresTaxOrRoundingGate(
                storedTaxRatePercent = 11,
                storedRoundingIncrement = 500L,
                taxRatePercent = 11,
                roundingIncrement = 500L,
            ),
        ).isFalse()
    }

    @Test
    fun raisingTheTaxRateNeedsAPin() {
        assertThat(
            requiresTaxOrRoundingGate(
                storedTaxRatePercent = 0,
                storedRoundingIncrement = 0L,
                taxRatePercent = 11,
                roundingIncrement = 0L,
            ),
        ).isTrue()
    }

    @Test
    fun changingOnlyTheRoundingNeedsAPin() {
        assertThat(
            requiresTaxOrRoundingGate(
                storedTaxRatePercent = 10,
                storedRoundingIncrement = 0L,
                taxRatePercent = 10,
                roundingIncrement = 100L,
            ),
        ).isTrue()
    }

    @Test
    fun droppingTheRoundingBackToZeroNeedsAPin() {
        assertThat(
            requiresTaxOrRoundingGate(
                storedTaxRatePercent = 0,
                storedRoundingIncrement = 1000L,
                taxRatePercent = 0,
                roundingIncrement = 0L,
            ),
        ).isTrue()
    }
}
