package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.ShiftSwitchResult
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.DebtPaymentDao
import id.umkm.pos.pro.data.local.dao.ReturnDao
import id.umkm.pos.pro.data.local.dao.ShiftDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity
import id.umkm.pos.pro.data.local.mapper.ShiftMapper
import id.umkm.pos.pro.domain.pos.ShiftReconciliation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShiftRepositoryImpl(
    private val database: UmkmDatabase,
    private val shiftDao: ShiftDao,
    private val transactionDao: TransactionDao,
    private val debtPaymentDao: DebtPaymentDao,
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

    override suspend fun openShift(cashierName: String, startingCash: Long): AppResult<CashierShift> {
        validateName(cashierName)?.let { return AppResult.Failure(it) }
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
                val now = clock.nowMillis()
                val entity = CashierShiftEntity(
                    cashierName = cashierName.trim(),
                    openedAt = now,
                    closedAt = null,
                    startingCash = startingCash,
                    expectedEndingCash = null,
                    actualEndingCash = null,
                )
                val id = shiftDao.insertShift(entity)
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
                val updated = shiftDao.closeShift(
                    shiftId = shiftId,
                    closedAt = now,
                    expectedEndingCash = summary.expectedEndingCash,
                    actualEndingCash = actualEndingCash,
                )
                if (updated == 0) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("shift $shiftId is already closed"),
                    )
                }
                AppResult.Success(
                    ShiftMapper.toModel(
                        shift.copy(
                            closedAt = now,
                            expectedEndingCash = summary.expectedEndingCash,
                            actualEndingCash = actualEndingCash,
                        ),
                    ),
                )
            }
        }
    }

    override suspend fun switchShift(
        closeShiftId: Long,
        actualEndingCash: Long,
        nextCashierName: String,
        nextStartingCash: Long,
    ): AppResult<ShiftSwitchResult> {
        if (actualEndingCash < 0L) {
            return AppResult.Failure(AppError.Validation("actual ending cash must not be negative"))
        }
        validateName(nextCashierName)?.let { return AppResult.Failure(it) }
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
                val now = clock.nowMillis()
                val summary = loadSummary(current, now)
                val updated = shiftDao.closeShift(
                    shiftId = closeShiftId,
                    closedAt = now,
                    expectedEndingCash = summary.expectedEndingCash,
                    actualEndingCash = actualEndingCash,
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
                    cashierName = nextCashierName.trim(),
                    openedAt = now,
                    closedAt = null,
                    startingCash = nextStartingCash,
                    expectedEndingCash = null,
                    actualEndingCash = null,
                )
                val nextId = shiftDao.insertShift(nextEntity)
                AppResult.Success(
                    ShiftSwitchResult(
                        closedShift = ShiftMapper.toModel(
                            current.copy(
                                closedAt = now,
                                expectedEndingCash = summary.expectedEndingCash,
                                actualEndingCash = actualEndingCash,
                            ),
                        ),
                        openedShift = ShiftMapper.toModel(nextEntity.copy(id = nextId)),
                    ),
                )
            }
        }
    }

    private suspend fun loadSummary(shift: CashierShiftEntity, endMillis: Long): ShiftCashSummary {
        val safeEnd = maxOf(shift.openedAt, endMillis)
        return ShiftReconciliation.summarize(
            startingCash = shift.startingCash,
            cashSales = transactionDao.sumCashReceivedBetween(shift.openedAt, safeEnd),
            cashDebtPayments = debtPaymentDao.sumCashPaymentsBetween(shift.openedAt, safeEnd),
            cashRefunds = returnDao.sumRefundsBetween(shift.openedAt, safeEnd),
            transactionCount = transactionDao.countTransactionsBetween(shift.openedAt, safeEnd),
        )
    }

    private fun validateName(cashierName: String): AppError? = when {
        cashierName.isBlank() -> AppError.Validation("cashier name is required")
        cashierName.trim().length > MAX_NAME_LENGTH ->
            AppError.Validation("cashier name is too long")

        else -> null
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
        const val MAX_NAME_LENGTH = 60
        const val MAX_RECENT_NAMES = 10
        const val MAX_HISTORY = 100
    }
}
