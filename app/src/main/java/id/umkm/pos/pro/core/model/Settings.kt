package id.umkm.pos.pro.core.model

data class AppSettings(
    val storeName: String,
    val cashierName: String?,
    val taxRatePercent: Int,
    val roundingIncrement: Long,
    val isPinEnabled: Boolean,
    val printerAddress: String?,
    val qrisImagePath: String?,
)
