package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.data.local.entity.CashierShiftEntity

object ShiftMapper {

    fun toEntity(shift: CashierShift): CashierShiftEntity = CashierShiftEntity(
        id = shift.id,
        cashierName = shift.cashierName,
        openedAt = shift.openedAt,
        closedAt = shift.closedAt,
        startingCash = shift.startingCash,
        expectedEndingCash = shift.expectedEndingCash,
        actualEndingCash = shift.actualEndingCash,
        openedByStaffId = shift.openedByStaffId,
        closedByStaffId = shift.closedByStaffId,
        closedByName = shift.closedByName,
    )

    fun toModel(entity: CashierShiftEntity): CashierShift = CashierShift(
        id = entity.id,
        cashierName = entity.cashierName,
        openedAt = entity.openedAt,
        closedAt = entity.closedAt,
        startingCash = entity.startingCash,
        expectedEndingCash = entity.expectedEndingCash,
        actualEndingCash = entity.actualEndingCash,
        openedByStaffId = entity.openedByStaffId,
        closedByStaffId = entity.closedByStaffId,
        closedByName = entity.closedByName,
    )
}
