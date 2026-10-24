package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.entity.InvoiceCounterEntity
import id.umkm.pos.pro.data.local.mapper.TransactionMapper
import id.umkm.pos.pro.data.local.util.InvoiceNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.TimeZone

class TransactionRepositoryImpl(
    private val database: UmkmDatabase,
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override suspend fun saveTransaction(
        transaction: Transaction,
        items: List<TransactionItem>,
    ): AppResult<Long> {
        if (items.isEmpty()) {
            return AppResult.Failure(AppError.Validation("transaction requires at least one item"))
        }
        val invalidItem = items.firstOrNull { item ->
            item.conversionFactor?.let { factor -> !factor.isFinite() || factor <= 0.0 } == true
        }
        if (invalidItem != null) {
            return AppResult.Failure(
                AppError.Validation("invalid unit conversion factor for product ${invalidItem.productId}"),
            )
        }
        return runCatching {
            database.withTransaction {
                val transactionId = transactionDao.insertTransactionWithItems(
                    transaction = TransactionMapper.toEntity(transaction),
                    items = items.map(TransactionMapper::toEntity),
                )
                decrementStock(items)
                transactionId
            }
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Failure(AppError.Storage(it.message ?: SAVE_FAILURE_MESSAGE)) },
        )
    }

    override suspend fun nextInvoiceNumber(createdAtMillis: Long): String {
        val timeZone = TimeZone.getDefault()
        val dayKey = InvoiceNumber.dayKey(createdAtMillis, timeZone)
        val sequence = database.withTransaction {
            val counterDao = database.invoiceCounterDao()
            val nextSequence = (counterDao.getCounter(dayKey)?.lastSequence ?: 0) + 1
            counterDao.upsertCounter(InvoiceCounterEntity(dayKey = dayKey, lastSequence = nextSequence))
            nextSequence
        }
        return InvoiceNumber.format(createdAtMillis, sequence, timeZone)
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? =
        transactionDao.getTransaction(transactionId)?.let(TransactionMapper::toModel)

    override fun observeTransactions(range: ReportRange): Flow<List<Transaction>> =
        transactionDao.observeTransactions(range.startMillis, range.endMillis)
            .map { entities -> entities.map(TransactionMapper::toModel) }

    override suspend fun getItems(transactionId: Long): List<TransactionItem> =
        transactionDao.getItems(transactionId).map(TransactionMapper::toModel)

    override suspend fun markReturned(
        transactionId: Long,
        returnedItems: List<TransactionItem>,
    ): AppResult<Unit> =
        runCatching {
            transactionDao.markTransactionReturned(
                transactionId = transactionId,
                returnedItems = returnedItems.map(TransactionMapper::toEntity),
            )
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Failure(AppError.Storage(it.message ?: RETURN_FAILURE_MESSAGE)) },
        )

    private suspend fun decrementStock(items: List<TransactionItem>) {
        val productDao = database.productDao()
        for (item in items) {
            val conversionFactor = item.conversionFactor ?: continue
            productDao.adjustStock(item.productId, -(item.qty * conversionFactor))
        }
    }

    private companion object {
        const val SAVE_FAILURE_MESSAGE = "failed to save transaction"
        const val RETURN_FAILURE_MESSAGE = "failed to mark transaction returned"
    }
}
