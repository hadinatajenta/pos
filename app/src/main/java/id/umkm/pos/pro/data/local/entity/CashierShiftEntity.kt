package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cashier_shifts",
    indices = [
        Index(value = ["closed_at"]),
        Index(value = ["opened_at"]),
    ],
)
data class CashierShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "cashier_name") val cashierName: String,
    @ColumnInfo(name = "opened_at") val openedAt: Long,
    @ColumnInfo(name = "closed_at") val closedAt: Long?,
    @ColumnInfo(name = "starting_cash") val startingCash: Long,
    @ColumnInfo(name = "expected_ending_cash") val expectedEndingCash: Long?,
    @ColumnInfo(name = "actual_ending_cash") val actualEndingCash: Long?,
)
