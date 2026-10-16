package id.umkm.pos.pro.core.contract

import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.core.model.SalesSummary
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
    suspend fun setPin(cashierName: String, pinHash: String, recoveryAnswerHash: String)
    suspend fun verifyPin(pinHash: String): Boolean
    suspend fun verifyRecoveryAnswer(recoveryAnswerHash: String): Boolean
}

interface ReportRepository {
    suspend fun salesSummary(range: ReportRange): SalesSummary
    suspend fun salesByDay(range: ReportRange): List<DailySales>
    suspend fun transactionsInRange(range: ReportRange): List<Transaction>
}
