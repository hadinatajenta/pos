package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    @Insert
    suspend fun insertShift(shift: CashierShiftEntity): Long

    @Query("SELECT * FROM cashier_shifts WHERE id = :shiftId")
    suspend fun getShift(shiftId: Long): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts WHERE closed_at IS NULL ORDER BY opened_at DESC LIMIT 1")
    suspend fun getActiveShift(): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts WHERE closed_at IS NULL ORDER BY opened_at DESC LIMIT 1")
    fun observeActiveShift(): Flow<CashierShiftEntity?>

    @Query("SELECT COUNT(*) FROM cashier_shifts WHERE closed_at IS NULL")
    suspend fun countActiveShifts(): Int

    @Query("SELECT * FROM cashier_shifts ORDER BY opened_at DESC LIMIT :limit")
    suspend fun getShiftHistory(limit: Int): List<CashierShiftEntity>

    @Query(
        "SELECT DISTINCT cashier_name FROM cashier_shifts " +
            "ORDER BY opened_at DESC LIMIT :limit",
    )
    suspend fun getRecentCashierNames(limit: Int): List<String>

    @Query(
        "UPDATE cashier_shifts SET closed_at = :closedAt, " +
            "expected_ending_cash = :expectedEndingCash, " +
            "actual_ending_cash = :actualEndingCash " +
            "WHERE id = :shiftId AND closed_at IS NULL",
    )
    suspend fun closeShift(
        shiftId: Long,
        closedAt: Long,
        expectedEndingCash: Long,
        actualEndingCash: Long,
    ): Int
}
