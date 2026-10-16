package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.ReturnLine
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.data.local.entity.ReturnEntity
import id.umkm.pos.pro.data.local.entity.ReturnLineEntity

object ReturnMapper {

    fun toEntity(record: ReturnRecord): ReturnEntity = ReturnEntity(
        id = record.id,
        transactionId = record.transactionId,
        createdAt = record.createdAt,
        refundAmount = record.refundAmount,
        totalHppReversed = record.totalHppReversed,
        note = record.note,
    )

    fun toModel(entity: ReturnEntity): ReturnRecord = ReturnRecord(
        id = entity.id,
        transactionId = entity.transactionId,
        createdAt = entity.createdAt,
        refundAmount = entity.refundAmount,
        totalHppReversed = entity.totalHppReversed,
        note = entity.note,
    )

    fun toEntity(line: ReturnLine): ReturnLineEntity = ReturnLineEntity(
        id = line.id,
        returnId = line.returnId,
        transactionItemId = line.transactionItemId,
        productId = line.productId,
        variantName = line.variantName,
        qty = line.qty,
        conversionFactor = line.conversionFactor,
        refundAmount = line.refundAmount,
        hppReversed = line.hppReversed,
        baseQtyRestocked = line.baseQtyRestocked,
    )

    fun toModel(entity: ReturnLineEntity): ReturnLine = ReturnLine(
        id = entity.id,
        returnId = entity.returnId,
        transactionItemId = entity.transactionItemId,
        productId = entity.productId,
        variantName = entity.variantName,
        qty = entity.qty,
        conversionFactor = entity.conversionFactor,
        refundAmount = entity.refundAmount,
        hppReversed = entity.hppReversed,
        baseQtyRestocked = entity.baseQtyRestocked,
    )
}
