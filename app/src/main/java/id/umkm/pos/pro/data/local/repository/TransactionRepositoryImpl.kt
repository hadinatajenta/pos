package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.CustomerDao
import id.umkm.pos.pro.data.local.dao.DebtPaymentDao
import id.umkm.pos.pro.data.local.dao.TransactionDao
import id.umkm.pos.pro.data.local.mapper.DebtPaymentMapper
import id.umkm.pos.pro.data.local.mapper.TransactionMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val database: UmkmDatabase,
    private val transactionDao: TransactionDao,
    private val debtPaymentDao: DebtPaymentDao,
    private val customerDao: CustomerDao,
) : TransactionRepository {

    override suspend fun saveTransaction(
        transaction: Transaction,
        items: List<TransactionItem>,
    ): AppResult<Long> {
        if (items.isEmpty()) {
            return AppResult.Failure(AppError.Validation("transaction requires at least one item"))
        }
        return runCatching {
            transactionDao.insertTransactionWithItems(
                transaction = TransactionMapper.toEntity(transaction),
                items = items.map(TransactionMapper::toEntity),
            )
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Failure(AppError.Storage(it.message ?: SAVE_FAILURE_MESSAGE)) },
        )
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? =
        transactionDao.getTransaction(transactionId)?.let(TransactionMapper::toModel)

    override fun observeTransactions(range: ReportRange): Flow<List<Transaction>> =
        transactionDao.observeTransactions(range.startMillis, range.endMillis)
            .map { entities -> entities.map(TransactionMapper::toModel) }

    override suspend fun getItems(transactionId: Long): List<TransactionItem> =
        transactionDao.getItems(transactionId).map(TransactionMapper::toModel)

    override suspend fun recordDebtPayment(payment: DebtPayment): AppResult<Long> =
        runCatching {
            database.withTransaction {
                val paymentId = debtPaymentDao.insertDebtPayment(DebtPaymentMapper.toEntity(payment))
                customerDao.adjustTotalDebt(payment.customerId, -payment.amount)
                applyPaymentToTransaction(payment)
                paymentId
            }
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Failure(AppError.Storage(it.message ?: DEBT_PAYMENT_FAILURE_MESSAGE)) },
        )

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

    private suspend fun applyPaymentToTransaction(payment: DebtPayment) {
        val transaction = transactionDao.getTransaction(payment.transactionId) ?: return
        val remainingDebt = (transaction.remainingDebt - payment.amount).coerceAtLeast(0L)
        val status = if (remainingDebt == 0L) TransactionStatus.COMPLETED else transaction.status
        transactionDao.updateDebtState(transaction.id, remainingDebt, status)
    }

    private companion object {
        const val SAVE_FAILURE_MESSAGE = "failed to save transaction"
        const val DEBT_PAYMENT_FAILURE_MESSAGE = "failed to record debt payment"
        const val RETURN_FAILURE_MESSAGE = "failed to mark transaction returned"
    }
}
