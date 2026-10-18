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
        printerName = settings.printerName,
        paperWidthMm = settings.paperWidthMm,
        languageCode = settings.languageCode,
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
        printerName = entity.printerName,
        paperWidthMm = entity.paperWidthMm,
        languageCode = entity.languageCode,
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
        printerName = null,
        paperWidthMm = 58,
        languageCode = "id",
        qrisImagePath = null,
        qrisPayload = null,
    )
}

