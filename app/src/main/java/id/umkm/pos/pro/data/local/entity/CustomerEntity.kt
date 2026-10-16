package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["whatsapp_number"]),
    ],
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "whatsapp_number") val whatsappNumber: String?,
    @ColumnInfo(name = "total_debt") val totalDebt: Long,
    @ColumnInfo(name = "notes") val notes: String?,
)
