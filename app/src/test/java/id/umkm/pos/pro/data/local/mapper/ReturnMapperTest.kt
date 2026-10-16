package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.ReturnLine
import id.umkm.pos.pro.core.model.ReturnRecord
import org.junit.Test

class ReturnMapperTest {

    @Test
    fun returnRecordRoundTripsThroughEntity() {
        val record = ReturnRecord(
            id = 31L,
            transactionId = 100L,
            createdAt = 1_700_000_000_000L,
            refundAmount = 40_000L,
            totalHppReversed = 25_000L,
            note = "barang rusak",
        )

        val restored = ReturnMapper.toModel(ReturnMapper.toEntity(record))

        assertThat(restored).isEqualTo(record)
    }

    @Test
    fun returnRecordEntityKeepsNullableNoteNull() {
        val record = ReturnRecord(
            id = 0L,
            transactionId = 101L,
            createdAt = 5L,
            refundAmount = 0L,
            totalHppReversed = 0L,
            note = null,
        )

        val entity = ReturnMapper.toEntity(record)

        assertThat(entity.id).isEqualTo(0L)
        assertThat(entity.note).isNull()
        assertThat(entity.refundAmount).isEqualTo(0L)
        assertThat(entity.totalHppReversed).isEqualTo(0L)
    }

    @Test
    fun returnLineRoundTripsThroughEntity() {
        val line = ReturnLine(
            id = 41L,
            returnId = 31L,
            transactionItemId = 500L,
            productId = 7L,
            variantName = "Dus",
            qty = 2.0,
            conversionFactor = 40.0,
            refundAmount = 40_000L,
            hppReversed = 25_000L,
            baseQtyRestocked = 80.0,
        )

        val restored = ReturnMapper.toModel(ReturnMapper.toEntity(line))

        assertThat(restored).isEqualTo(line)
    }

    @Test
    fun returnLineEntityKeepsNullableFieldsAndQuantityTyping() {
        val line = ReturnLine(
            id = 0L,
            returnId = 0L,
            transactionItemId = null,
            productId = 8L,
            variantName = null,
            qty = 1.5,
            conversionFactor = 1.0,
            refundAmount = 3_000L,
            hppReversed = 2_000L,
            baseQtyRestocked = 1.5,
        )

        val entity = ReturnMapper.toEntity(line)

        assertThat(entity.transactionItemId).isNull()
        assertThat(entity.variantName).isNull()
        assertThat(entity.qty).isEqualTo(1.5)
        assertThat(entity.conversionFactor).isEqualTo(1.0)
        assertThat(entity.refundAmount).isEqualTo(3_000L)
        assertThat(entity.hppReversed).isEqualTo(2_000L)
        assertThat(entity.baseQtyRestocked).isEqualTo(1.5)
    }
}
