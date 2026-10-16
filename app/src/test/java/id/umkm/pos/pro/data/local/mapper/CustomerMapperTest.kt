package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.Customer
import org.junit.Test

class CustomerMapperTest {

    @Test
    fun customerRoundTripsThroughEntity() {
        val customer = Customer(
            id = 11L,
            name = "Bu Siti",
            whatsappNumber = "628123456789",
            totalDebt = 45_000L,
            notes = "langganan",
        )

        val restored = CustomerMapper.toModel(CustomerMapper.toEntity(customer))

        assertThat(restored).isEqualTo(customer)
    }

    @Test
    fun customerEntityKeepsNullableFieldsNull() {
        val customer = Customer(
            id = 0L,
            name = "Pak Budi",
            whatsappNumber = null,
            totalDebt = 0L,
            notes = null,
        )

        val entity = CustomerMapper.toEntity(customer)

        assertThat(entity.id).isEqualTo(0L)
        assertThat(entity.whatsappNumber).isNull()
        assertThat(entity.notes).isNull()
        assertThat(entity.totalDebt).isEqualTo(0L)
    }
}
