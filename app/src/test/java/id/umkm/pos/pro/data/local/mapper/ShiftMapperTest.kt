package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.CashierShift
import org.junit.Test

class ShiftMapperTest {

    @Test
    fun openShiftRoundTripsThroughEntityWithNullCloseFields() {
        val shift = CashierShift(
            id = 5L,
            cashierName = "Andi",
            openedAt = 1_700_000_000_000L,
            closedAt = null,
            startingCash = 200_000L,
            expectedEndingCash = null,
            actualEndingCash = null,
        )

        val restored = ShiftMapper.toModel(ShiftMapper.toEntity(shift))

        assertThat(restored).isEqualTo(shift)
    }

    @Test
    fun closedShiftRoundTripsThroughEntity() {
        val shift = CashierShift(
            id = 6L,
            cashierName = "Rina",
            openedAt = 1_700_000_000_000L,
            closedAt = 1_700_010_000_000L,
            startingCash = 150_000L,
            expectedEndingCash = 1_350_000L,
            actualEndingCash = 1_340_000L,
        )

        val restored = ShiftMapper.toModel(ShiftMapper.toEntity(shift))

        assertThat(restored).isEqualTo(shift)
    }
}
