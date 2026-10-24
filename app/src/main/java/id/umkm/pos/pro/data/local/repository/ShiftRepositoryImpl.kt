package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.ShiftSwitchResult
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.ReturnDao
import id.umkm.pos.pro.data.local.dao.ShiftDao
import id.umkm.pos.pro.data.local.dao.StaffDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity
import id.umkm.pos.pro.data.local.mapper.ShiftMapper
import id.umkm.pos.pro.data.local.mapper.StaffMapper
import id.umkm.pos.pro.domain.pos.ShiftReconciliation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShiftRepositoryImpl(
    private val database: UmkmDatabase,
    private val shiftDao: ShiftDao,
    private val staffDao: StaffDao,
    private val transactionDao: TransactionDao,
    private val returnDao: ReturnDao,
    private val clock: Clock,
) : ShiftRepository {

    override fun observeActiveShift(): Flow<CashierShift?> =
        shiftDao.observeActiveShift().map { entity -> entity?.let(ShiftMapper::toModel) }

    override suspend fun getActiveShift(): CashierShift? =
        shiftDao.getActiveShift()?.let(ShiftMapper::toModel)

    override suspend fun getRecentCashierNames(limit: Int): List<String> =
        shiftDao.getRecentCashierNames(limit.coerceIn(1, MAX_RECENT_NAMES))

    override suspend fun getShiftHistory(limit: Int): List<CashierShift> =
        shiftDao.getShiftHistory(limit.coerceIn(1, MAX_HISTORY))
            .map(ShiftMapper::toModel)

    override suspend fun getShiftSummary(shiftId: Long): AppResult<ShiftCashSummary> {
        val shift = shiftDao.getShift(shiftId)
            ?: return AppResult.Failure(AppError.NotFound("shift $shiftId was not found"))
        return AppResult.Success(loadSummary(shift, shift.closedAt ?: clock.nowMillis()))
    }

    override suspend fun openShift(staffId: Long, startingCash: Long): AppResult<CashierShift> {
        if (startingCash < 0L) {
            return AppResult.Failure(AppError.Validation("starting cash must not be negative"))
        }
        return guarded {
            database.withTransaction {
                if (shiftDao.countActiveShifts() > 0) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("another shift is already open"),
                    )
                }
                val staff = staffDao.getStaff(staffId)
                    ?: return@withTransaction AppResult.Failure(
                        AppError.NotFound("staff $staffId was not found"),
                    )
                if (!staff.isActive) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("staff $staffId is inactive"),
                    )
                }
                val now = clock.nowMillis()
                val entity = CashierShiftEntity(
                    cashierName = staff.name,
                    openedAt = now,
                    closedAt = null,
                    startingCash = startingCash,
                    expectedEndingCash = null,
                    actualEndingCash = null,
                    openedByStaffId = staff.id,
                )
                val id = shiftDao.insertShift(entity)
                staffDao.setActiveStaffId(staff.id)
                AppResult.Success(ShiftMapper.toModel(entity.copy(id = id)))
            }
        }
    }

    override suspend fun closeShift(
        shiftId: Long,
        actualEndingCash: Long,
    ): AppResult<CashierShift> {
        if (actualEndingCash < 0L) {
            return AppResult.Failure(AppError.Validation("actual ending cash must not be negative"))
        }
        return guarded {
            database.withTransaction {
                val shift = shiftDao.getShift(shiftId)
                    ?: return@withTransaction AppResult.Failure(
                        AppError.NotFound("shift $shiftId was not found"),
                    )
                if (shift.closedAt != null) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("shift $shiftId is already closed"),
                    )
                }
                val now = clock.nowMillis()
                val summary = loadSummary(shift, now)
                val closer = currentStaff()
                val updated = shiftDao.closeShift(
                    shiftId = shiftId,
                    closedAt = now,
                    expectedEndingCash = summary.expectedEndingCash,
                    actualEndingCash = actualEndingCash,
                    closedByStaffId = closer?.id,
                    closedByName = closer?.name ?: shift.cashierName,
                )
                if (updated == 0) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("shift $shiftId is already closed"),
                    )
                }
                staffDao.setActiveStaffId(null)
                AppResult.Success(
                    ShiftMapper.toModel(
                        shift.copy(
                            closedAt = now,
                            expectedEndingCash = summary.expectedEndingCash,
                            actualEndingCash = actualEndingCash,
                            closedByStaffId = closer?.id,
                            closedByName = closer?.name ?: shift.cashierName,
                        ),
                    ),
                )
            }
        }
    }

    override suspend fun switchShift(
        closeShiftId: Long,
        actualEndingCash: Long,
        nextStaffId: Long,
        nextStartingCash: Long,
    ): AppResult<ShiftSwitchResult> {
        if (actualEndingCash < 0L) {
            return AppResult.Failure(AppError.Validation("actual ending cash must not be negative"))
        }
        if (nextStartingCash < 0L) {
            return AppResult.Failure(AppError.Validation("starting cash must not be negative"))
        }
        return guarded {
            database.withTransaction {
                val current = shiftDao.getShift(closeShiftId)
                    ?: return@withTransaction AppResult.Failure(
                        AppError.NotFound("shift $closeShiftId was not found"),
                    )
                if (current.closedAt != null) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("shift $closeShiftId is already closed"),
                    )
                }
                val nextStaff = staffDao.getStaff(nextStaffId)
                    ?: return@withTransaction AppResult.Failure(
                        AppError.NotFound("staff $nextStaffId was not found"),
                    )
                if (!nextStaff.isActive) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("staff $nextStaffId is inactive"),
                    )
                }
                val now = clock.nowMillis()
                val summary = loadSummary(current, now)
                val closer = currentStaff()
                val updated = shiftDao.closeShift(
                    shiftId = closeShiftId,
                    closedAt = now,
                    expectedEndingCash = summary.expectedEndingCash,
                    actualEndingCash = actualEndingCash,
                    closedByStaffId = closer?.id,
                    closedByName = closer?.name ?: current.cashierName,
                )
                if (updated == 0) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("shift $closeShiftId is already closed"),
                    )
                }
                if (shiftDao.countActiveShifts() > 0) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("another shift is still open"),
                    )
                }
                val nextEntity = CashierShiftEntity(
                    cashierName = nextStaff.name,
                    openedAt = now,
                    closedAt = null,
                    startingCash = nextStartingCash,
                    expectedEndingCash = null,
                    actualEndingCash = null,
                    openedByStaffId = nextStaff.id,
                )
                val nextId = shiftDao.insertShift(nextEntity)
                staffDao.setActiveStaffId(nextStaff.id)
                AppResult.Success(
                    ShiftSwitchResult(
                        closedShift = ShiftMapper.toModel(
                            current.copy(
                                closedAt = now,
                                expectedEndingCash = summary.expectedEndingCash,
                                actualEndingCash = actualEndingCash,
                                closedByStaffId = closer?.id,
                                closedByName = closer?.name ?: current.cashierName,
                            ),
                        ),
                        openedShift = ShiftMapper.toModel(nextEntity.copy(id = nextId)),
                    ),
                )
            }
        }
    }

    override suspend fun switchOperator(staffId: Long): AppResult<Staff> = guarded {
        database.withTransaction {
            if (shiftDao.getActiveShift() == null) {
                return@withTransaction AppResult.Failure(
                    AppError.Validation("no shift is open"),
                )
            }
            val staff = staffDao.getStaff(staffId)
                ?: return@withTransaction AppResult.Failure(
                    AppError.NotFound("staff $staffId was not found"),
                )
            if (!staff.isActive) {
                return@withTransaction AppResult.Failure(
                    AppError.Validation("staff $staffId is inactive"),
                )
            }
            staffDao.setActiveStaffId(staff.id)
            AppResult.Success(StaffMapper.toModel(staff))
        }
    }

    private suspend fun currentStaff(): Staff? =
        staffDao.getActiveStaffId()?.let { staffId ->
            staffDao.getStaff(staffId)?.let(StaffMapper::toModel)
        }

    private suspend fun loadSummary(shift: CashierShiftEntity, endMillis: Long): ShiftCashSummary {
        val safeEnd = maxOf(shift.openedAt, endMillis)
        return ShiftReconciliation.summarize(
            startingCash = shift.startingCash,
            cashSales = transactionDao.sumCashReceivedBetween(shift.openedAt, safeEnd),
            cashRefunds = returnDao.sumRefundsBetween(shift.openedAt, safeEnd),
            transactionCount = transactionDao.countTransactionsBetween(shift.openedAt, safeEnd),
        )
    }

    private suspend fun <T> guarded(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppResult.Failure(AppError.Storage(error.message ?: "shift operation failed"))
        }

    private companion object {
        const val MAX_RECENT_NAMES = 10
        const val MAX_HISTORY = 100
    }
}
