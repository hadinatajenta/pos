package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.umkm.pos.pro.data.local.entity.InvoiceCounterEntity

@Dao
interface InvoiceCounterDao {

    @Query("SELECT * FROM invoice_counters WHERE day_key = :dayKey")
    suspend fun getCounter(dayKey: String): InvoiceCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: InvoiceCounterEntity)
}
