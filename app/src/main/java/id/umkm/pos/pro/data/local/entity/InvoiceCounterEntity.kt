package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_counters")
data class InvoiceCounterEntity(
    @PrimaryKey @ColumnInfo(name = "day_key") val dayKey: String,
    @ColumnInfo(name = "last_sequence") val lastSequence: Int,
)
