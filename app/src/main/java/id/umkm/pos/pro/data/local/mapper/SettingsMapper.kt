package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.data.local.entity.SettingsEntity

object SettingsMapper {

    fun toEntity(settings: AppSettings): SettingsEntity = SettingsEntity(
        storeName = settings.storeName,
        cashierName = settings.cashierName,
        taxRatePercent = settings.taxRatePercent,
        roundingIncrement = settings.roundingIncrement,
        isPinEnabled = settings.isPinEnabled,
        printerAddress = settings.printerAddress,
        qrisImagePath = settings.qrisImagePath,
        qrisPayload = settings.qrisPayload,
    )

    fun toModel(entity: SettingsEntity): AppSettings = AppSettings(
        storeName = entity.storeName,
        cashierName = entity.cashierName,
        taxRatePercent = entity.taxRatePercent,
        roundingIncrement = entity.roundingIncrement,
        isPinEnabled = entity.isPinEnabled,
        printerAddress = entity.printerAddress,
        qrisImagePath = entity.qrisImagePath,
        qrisPayload = entity.qrisPayload,
    )

    fun defaultModel(): AppSettings = AppSettings(
        storeName = "",
        cashierName = null,
        taxRatePercent = 0,
        roundingIncrement = 0L,
        isPinEnabled = false,
        printerAddress = null,
        qrisImagePath = null,
        qrisPayload = null,
    )
}
