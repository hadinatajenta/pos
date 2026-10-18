package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.AppSettings
import org.junit.Test

class SettingsMapperTest {

    @Test
    fun settingsRoundTripsThroughEntity() {
        val settings = AppSettings(
            storeName = "Toko Berkah",
            cashierName = "Andi",
            taxRatePercent = 11,
            roundingIncrement = 100L,
            isPinEnabled = true,
            printerAddress = "00:11:22:33:44:55",
            printerName = "RPP02N",
            paperWidthMm = 80,
            languageCode = "zh",
            qrisImagePath = "/data/qris.png",
            qrisPayload = "000201...",
        )

        val restored = SettingsMapper.toModel(SettingsMapper.toEntity(settings))

        assertThat(restored).isEqualTo(settings)
    }

    @Test
    fun settingsEntityKeepsNullableFieldsNull() {
        val settings = AppSettings(
            storeName = "Toko Baru",
            cashierName = null,
            taxRatePercent = 0,
            roundingIncrement = 0L,
            isPinEnabled = false,
            printerAddress = null,
            printerName = null,
            paperWidthMm = 58,
            languageCode = "id",
            qrisImagePath = null,
        )

        val entity = SettingsMapper.toEntity(settings)

        assertThat(entity.cashierName).isNull()
        assertThat(entity.printerAddress).isNull()
        assertThat(entity.printerName).isNull()
        assertThat(entity.paperWidthMm).isEqualTo(58)
        assertThat(entity.languageCode).isEqualTo("id")
        assertThat(entity.qrisImagePath).isNull()
        assertThat(entity.taxRatePercent).isEqualTo(0)
        assertThat(entity.roundingIncrement).isEqualTo(0L)
        assertThat(entity.isPinEnabled).isFalse()
        assertThat(entity.id).isEqualTo(1)
    }

    @Test
    fun defaultModelHasNoConfiguredValues() {
        val defaults = SettingsMapper.defaultModel()

        assertThat(defaults.storeName).isEmpty()
        assertThat(defaults.cashierName).isNull()
        assertThat(defaults.taxRatePercent).isEqualTo(0)
        assertThat(defaults.roundingIncrement).isEqualTo(0L)
        assertThat(defaults.isPinEnabled).isFalse()
        assertThat(defaults.printerAddress).isNull()
        assertThat(defaults.printerName).isNull()
        assertThat(defaults.paperWidthMm).isEqualTo(58)
        assertThat(defaults.languageCode).isEqualTo("id")
        assertThat(defaults.qrisImagePath).isNull()
    }
}

