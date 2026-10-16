package id.umkm.pos.pro.data.local.repository

import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.data.local.dao.ShiftDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.mapper.ShiftMapper

class ShiftRepositoryImpl(
    private val shiftDao: ShiftDao,
    private val transactionDao: TransactionDao,
    private val clock: Clock,
) : ShiftRepository {

    override suspend fun openShift(shift: CashierShift): Long =
        shiftDao.insertShift(ShiftMapper.toEntity(shift))

    override suspend fun closeShift(shiftId: Long, actualEndingCash: Long) {
        val shift = shiftDao.getShift(shiftId)
            ?: error("cannot close shift $shiftId because it does not exist")
        val cashSalesSinceOpen = transactionDao.sumCashSalesSince(shift.openedAt)
        shiftDao.closeShift(
            shiftId = shiftId,
            closedAt = clock.nowMillis(),
            expectedEndingCash = shift.startingCash + cashSalesSinceOpen,
            actualEndingCash = actualEndingCash,
        )
    }

    override suspend fun getActiveShift(): CashierShift? =
        shiftDao.getActiveShift()?.let(ShiftMapper::toModel)
}
