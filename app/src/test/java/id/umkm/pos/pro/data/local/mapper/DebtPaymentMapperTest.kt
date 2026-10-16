package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.DebtPayment
import id.umkm.pos.pro.core.model.PaymentMethod
import org.junit.Test

class DebtPaymentMapperTest {

    @Test
    fun debtPaymentRoundTripsThroughEntity() {
        val payment = DebtPayment(
            id = 21L,
            transactionId = 100L,
            customerId = 11L,
            paidAt = 1_700_000_000_000L,
            amount = 20_000L,
            paymentMethod = PaymentMethod.TRANSFER,
            note = "cicilan pertama",
        )

        val restored = DebtPaymentMapper.toModel(DebtPaymentMapper.toEntity(payment))

        assertThat(restored).isEqualTo(payment)
    }

    @Test
    fun debtPaymentEntityPreservesPaymentMethodAndNullableNote() {
        val payment = DebtPayment(
            id = 0L,
            transactionId = 101L,
            customerId = 12L,
            paidAt = 1L,
            amount = 5_000L,
            paymentMethod = PaymentMethod.KASBON,
            note = null,
        )

        val entity = DebtPaymentMapper.toEntity(payment)

        assertThat(entity.paymentMethod).isEqualTo(PaymentMethod.KASBON)
        assertThat(entity.note).isNull()
        assertThat(entity.amount).isEqualTo(5_000L)
    }
}
