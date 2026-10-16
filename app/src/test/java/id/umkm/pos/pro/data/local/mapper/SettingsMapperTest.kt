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
            qrisImagePath = "/data/qris.png",
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
            qrisImagePath = null,
        )

        val entity = SettingsMapper.toEntity(settings)

        assertThat(entity.cashierName).isNull()
        assertThat(entity.printerAddress).isNull()
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
        assertThat(defaults.qrisImagePath).isNull()
    }
}
