package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "store_name") val storeName: String,
    @ColumnInfo(name = "cashier_name") val cashierName: String?,
    @ColumnInfo(name = "tax_rate_percent") val taxRatePercent: Int,
    @ColumnInfo(name = "rounding_increment") val roundingIncrement: Long,
    @ColumnInfo(name = "is_pin_enabled") val isPinEnabled: Boolean,
    @ColumnInfo(name = "printer_address") val printerAddress: String?,
    @ColumnInfo(name = "printer_name") val printerName: String? = null,
    @ColumnInfo(name = "paper_width_mm") val paperWidthMm: Int = 58,
    @ColumnInfo(name = "language_code") val languageCode: String = "id",
    @ColumnInfo(name = "qris_image_path") val qrisImagePath: String?,
    @ColumnInfo(name = "qris_payload") val qrisPayload: String?,
    @ColumnInfo(name = "active_staff_id") val activeStaffId: Long? = null,
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}

