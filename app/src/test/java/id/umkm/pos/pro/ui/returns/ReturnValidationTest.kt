package id.umkm.pos.pro.ui.returns

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.UiText
import org.junit.Test

class ReturnValidationTest {

    private fun line(
        transactionItemId: Long,
        purchasedQty: Double,
        returnQty: String,
    ): ReturnLineInput = ReturnLineInput(
        transactionItemId = transactionItemId,
        itemName = UiText.literal("Item $transactionItemId"),
        variantName = null,
        purchasedQty = purchasedQty,
        returnQty = returnQty,
    )

    @Test
    fun acceptsAFullReturn() {
        val validation = validateReturn(listOf(line(1L, purchasedQty = 3.0, returnQty = "3")))

        assertThat(validation.isValid).isTrue()
    }

    @Test
    fun acceptsAPartialReturn() {
        val validation = validateReturn(listOf(line(1L, purchasedQty = 3.0, returnQty = "1.5")))

        assertThat(validation.isValid).isTrue()
    }

    @Test
    fun rejectsAQuantityAboveThePurchasedAmount() {
        val validation = validateReturn(listOf(line(1L, purchasedQty = 3.0, returnQty = "4")))

        assertThat(validation.isValid).isFalse()
        assertThat(validation.lineErrors[1L]?.resolve(IdStrings))
            .isEqualTo("Jumlah retur tidak boleh melebihi jumlah yang dibeli.")
    }

    @Test
    fun rejectsAZeroQuantity() {
        val validation = validateReturn(listOf(line(1L, purchasedQty = 3.0, returnQty = "0")))

        assertThat(validation.lineErrors[1L]?.resolve(IdStrings))
            .isEqualTo("Masukkan jumlah lebih besar dari nol.")
    }

    @Test
    fun rejectsANegativeQuantity() {
        val validation = validateReturn(listOf(line(1L, purchasedQty = 3.0, returnQty = "-1")))

        assertThat(validation.lineErrors[1L]?.resolve(IdStrings))
            .isEqualTo("Masukkan jumlah lebih besar dari nol.")
    }

    @Test
    fun rejectsAnUnparsableQuantity() {
        val validation = validateReturn(listOf(line(1L, purchasedQty = 3.0, returnQty = "abc")))

        assertThat(validation.lineErrors[1L]?.resolve(IdStrings))
            .isEqualTo("Masukkan jumlah retur yang valid.")
    }

    @Test
    fun treatsABlankQuantityAsNotReturned() {
        val validation = validateReturn(
            listOf(
                line(1L, purchasedQty = 3.0, returnQty = ""),
                line(2L, purchasedQty = 2.0, returnQty = "2"),
            ),
        )

        assertThat(validation.isValid).isTrue()
        assertThat(validation.lineErrors).isEmpty()
    }

    @Test
    fun rejectsAReturnWithoutAnySelectedItem() {
        val validation = validateReturn(
            listOf(
                line(1L, purchasedQty = 3.0, returnQty = ""),
                line(2L, purchasedQty = 2.0, returnQty = "  "),
            ),
        )

        assertThat(validation.isValid).isFalse()
        assertThat(validation.formError?.resolve(IdStrings))
            .isEqualTo("Pilih setidaknya satu barang untuk diretur.")
    }

    @Test
    fun eachInvalidLineReportsItsOwnError() {
        val validation = validateReturn(
            listOf(
                line(1L, purchasedQty = 3.0, returnQty = "4"),
                line(2L, purchasedQty = 2.0, returnQty = "0"),
                line(3L, purchasedQty = 5.0, returnQty = "5"),
            ),
        )

        assertThat(validation.lineErrors.keys).containsExactly(1L, 2L)
    }

    @Test
    fun buildsRequestsForPositiveLinesOnly() {
        val requests = toReturnLineRequests(
            listOf(
                line(1L, purchasedQty = 3.0, returnQty = "2"),
                line(2L, purchasedQty = 2.0, returnQty = ""),
            ),
        )

        assertThat(requests).hasSize(1)
        assertThat(requests.single().transactionItemId).isEqualTo(1L)
        assertThat(requests.single().returnedQty).isEqualTo(2.0)
        assertThat(requests.single().conversionFactor).isNull()
    }
}
