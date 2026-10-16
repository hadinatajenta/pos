package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import id.umkm.pos.pro.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query(
        "SELECT * FROM customers " +
            "WHERE name LIKE :pattern ESCAPE '\\' " +
            "OR (whatsapp_number IS NOT NULL AND whatsapp_number LIKE :pattern ESCAPE '\\') " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeCustomers(pattern: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomer(customerId: Long): CustomerEntity?

    @Insert
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomer(customerId: Long)

    @Query("UPDATE customers SET total_debt = MAX(0, total_debt + :delta) WHERE id = :customerId")
    suspend fun adjustTotalDebt(customerId: Long, delta: Long)
}
