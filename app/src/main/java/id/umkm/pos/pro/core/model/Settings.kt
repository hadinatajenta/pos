package id.umkm.pos.pro.core.model

data class AppSettings(
    val storeName: String,
    val cashierName: String?,
    val taxRatePercent: Int,
    val roundingIncrement: Long,
    val isPinEnabled: Boolean,
    val printerAddress: String?,
    val printerName: String? = null,
    val paperWidthMm: Int = 58,
    val languageCode: String = "id",
    val qrisImagePath: String?,
    val qrisPayload: String? = null,
    val activeStaffId: Long? = null,
)

