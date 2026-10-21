package id.umkm.pos.pro.core.contract

import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.ReturnLine
import id.umkm.pos.pro.core.model.ReturnRecord
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityCredentials
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.SecurityState
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(query: String?): Flow<List<Product>>
    suspend fun getProductById(productId: Long): Product?
    suspend fun findProductByBarcode(barcode: String): Product?
    suspend fun upsertProduct(product: Product): Long
    suspend fun deleteProduct(productId: Long)
    suspend fun getUnits(productId: Long): List<ProductUnit>
    suspend fun findUnitByBarcode(barcode: String): ProductUnit?
    suspend fun upsertUnit(unit: ProductUnit): Long
    suspend fun deleteUnit(unitId: Long)
}

interface CustomerRepository {
    fun observeCustomers(query: String?): Flow<List<Customer>>
    suspend fun getCustomer(customerId: Long): Customer?
    suspend fun upsertCustomer(customer: Customer): Long
    suspend fun deleteCustomer(customerId: Long)
    fun observeDebtPayments(customerId: Long): Flow<List<DebtPayment>>
}

interface TransactionRepository {
    suspend fun saveTransaction(transaction: Transaction, items: List<TransactionItem>): AppResult<Long>
    suspend fun nextInvoiceNumber(createdAtMillis: Long): String
    suspend fun getTransaction(transactionId: Long): Transaction?
    fun observeTransactions(range: ReportRange): Flow<List<Transaction>>
    suspend fun getItems(transactionId: Long): List<TransactionItem>
    suspend fun recordDebtPayment(payment: DebtPayment): AppResult<Long>
    suspend fun markReturned(transactionId: Long, returnedItems: List<TransactionItem>): AppResult<Unit>
}

interface ShiftRepository {
    suspend fun openShift(shift: CashierShift): Long
    suspend fun closeShift(shiftId: Long, actualEndingCash: Long)
    suspend fun getActiveShift(): CashierShift?
}

interface SecurityRepository {
    fun observeState(): Flow<SecurityState>

    suspend fun getState(): SecurityState

    suspend fun getCredentials(): SecurityCredentials

    suspend fun saveCredentials(
        pinHash: String,
        recoveryCodeHash: String,
        pinUpdatedAtMillis: Long,
        audit: SecurityAuditEntry?,
    )

    suspend fun clearCredentials(audit: SecurityAuditEntry?)

    suspend fun recordAttempt(
        failedAttempts: Int,
        lockoutUntilMillis: Long?,
        audit: SecurityAuditEntry?,
    )

    suspend fun updateSession(role: SecurityRole?, unlockedUntilMillis: Long?)

    suspend fun appendAudit(entry: SecurityAuditEntry)

    suspend fun recentAudit(limit: Int): List<SecurityAuditEntry>

    suspend fun trimAudit(keep: Int)
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateSettings(settings: AppSettings)
}

interface ReportRepository {
    suspend fun salesSummary(range: ReportRange): SalesSummary
    suspend fun salesByDay(range: ReportRange): List<DailySales>
    suspend fun transactionsInRange(range: ReportRange): List<Transaction>
}

interface ReturnRepository {
    suspend fun recordReturn(record: ReturnRecord, lines: List<ReturnLine>): AppResult<Long>
    suspend fun getReturns(transactionId: Long): List<ReturnRecord>
    suspend fun returnsInRange(range: ReportRange): List<ReturnRecord>
}
