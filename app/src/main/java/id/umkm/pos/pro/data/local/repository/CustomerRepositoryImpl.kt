package id.umkm.pos.pro.data.local.repository

import id.umkm.pos.pro.core.contract.CustomerRepository
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.data.local.dao.CustomerDao
import id.umkm.pos.pro.data.local.dao.DebtPaymentDao
import id.umkm.pos.pro.data.local.mapper.CustomerMapper
import id.umkm.pos.pro.data.local.mapper.DebtPaymentMapper
import id.umkm.pos.pro.data.local.util.toContainsLikePattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao,
    private val debtPaymentDao: DebtPaymentDao,
) : CustomerRepository {

    override fun observeCustomers(query: String?): Flow<List<Customer>> =
        customerDao.observeCustomers(query.toContainsLikePattern())
            .map { entities -> entities.map(CustomerMapper::toModel) }

    override suspend fun getCustomer(customerId: Long): Customer? =
        customerDao.getCustomer(customerId)?.let(CustomerMapper::toModel)

    override suspend fun upsertCustomer(customer: Customer): Long {
        val entity = CustomerMapper.toEntity(customer)
        return if (customer.id == 0L) {
            customerDao.insertCustomer(entity)
        } else {
            customerDao.updateCustomer(entity)
            customer.id
        }
    }

    override suspend fun deleteCustomer(customerId: Long) {
        customerDao.deleteCustomer(customerId)
    }

    override fun observeDebtPayments(customerId: Long): Flow<List<DebtPayment>> =
        debtPaymentDao.observeDebtPayments(customerId)
            .map { entities -> entities.map(DebtPaymentMapper::toModel) }
}
