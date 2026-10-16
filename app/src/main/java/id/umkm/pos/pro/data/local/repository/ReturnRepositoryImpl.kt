package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ReturnRepository
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.ReturnLine
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.ProductDao
import id.umkm.pos.pro.data.local.dao.ReturnDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.mapper.ReturnMapper

class ReturnRepositoryImpl(
    private val database: UmkmDatabase,
    private val returnDao: ReturnDao,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
) : ReturnRepository {

    override suspend fun recordReturn(
        record: ReturnRecord,
        lines: List<ReturnLine>,
    ): AppResult<Long> {
        if (lines.isEmpty()) {
            return AppResult.Failure(AppError.Validation(EMPTY_LINES_MESSAGE))
        }
        return runCatching {
            database.withTransaction {
                val returnId = returnDao.insertReturnWithLines(
                    record = ReturnMapper.toEntity(record),
                    lines = lines.map(ReturnMapper::toEntity),
                )
                transactionDao.updateStatus(record.transactionId, TransactionStatus.RETURNED)
                lines.forEach { line ->
                    productDao.adjustStock(line.productId, line.baseQtyRestocked)
                }
                returnId
            }
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Failure(AppError.Storage(it.message ?: RECORD_RETURN_FAILURE_MESSAGE)) },
        )
    }

    override suspend fun getReturns(transactionId: Long): List<ReturnRecord> =
        returnDao.getReturnsForTransaction(transactionId).map(ReturnMapper::toModel)

    override suspend fun returnsInRange(range: ReportRange): List<ReturnRecord> =
        returnDao.getReturnsInRange(range.startMillis, range.endMillis).map(ReturnMapper::toModel)

    private companion object {
        const val EMPTY_LINES_MESSAGE = "return requires at least one line"
        const val RECORD_RETURN_FAILURE_MESSAGE = "failed to record return"
    }
}
