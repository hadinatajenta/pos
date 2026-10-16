package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity

@Dao
interface ShiftDao {

    @Insert
    suspend fun insertShift(shift: CashierShiftEntity): Long

    @Query("SELECT * FROM cashier_shifts WHERE id = :shiftId")
    suspend fun getShift(shiftId: Long): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts WHERE closed_at IS NULL ORDER BY opened_at DESC LIMIT 1")
    suspend fun getActiveShift(): CashierShiftEntity?

    @Query(
        "UPDATE cashier_shifts SET closed_at = :closedAt, " +
            "expected_ending_cash = :expectedEndingCash, " +
            "actual_ending_cash = :actualEndingCash " +
            "WHERE id = :shiftId",
    )
    suspend fun closeShift(
        shiftId: Long,
        closedAt: Long,
        expectedEndingCash: Long,
        actualEndingCash: Long,
    )
}
