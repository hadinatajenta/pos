package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.core.model.TransactionStatus
import org.junit.Test

class TransactionMapperTest {

    @Test
    fun transactionRoundTripsThroughEntity() {
        val transaction = Transaction(
            id = 100L,
            invoiceNo = "INV-2026-0001",
            createdAt = 1_700_000_000_000L,
            cashierId = 1L,
            subtotal = 90_000L,
            discountTotal = 15_000L,
            invoiceDiscount = 10_000L,
            taxTotal = 4_000L,
            rounding = 0L,
            grandTotal = 84_000L,
            totalHpp = 60_000L,
            netProfit = 20_000L,
            paymentMethod = PaymentMethod.TRANSFER,
            paidAmount = 80_000L,
            changeAmount = 0L,
            status = TransactionStatus.COMPLETED,
            notes = "transfer",
        )

        val restored = TransactionMapper.toModel(TransactionMapper.toEntity(transaction))

        assertThat(restored).isEqualTo(transaction)
    }

    @Test
    fun transactionEntityCarriesInvoiceDiscountDecisionD1() {
        val transaction = Transaction(
            id = 0L,
            invoiceNo = "INV-2026-0002",
            createdAt = 10L,
            cashierId = null,
            subtotal = 50_000L,
            discountTotal = 5_000L,
            invoiceDiscount = 5_000L,
            taxTotal = 0L,
            rounding = 0L,
            grandTotal = 45_000L,
            totalHpp = 30_000L,
            netProfit = 15_000L,
            paymentMethod = PaymentMethod.CASH,
            paidAmount = 50_000L,
            changeAmount = 5_000L,
            status = TransactionStatus.COMPLETED,
            notes = null,
        )

        val entity = TransactionMapper.toEntity(transaction)

        assertThat(entity.invoiceDiscount).isEqualTo(5_000L)
        assertThat(entity.discountTotal).isEqualTo(5_000L)
        assertThat(entity.cashierId).isNull()
        assertThat(entity.notes).isNull()
        assertThat(entity.paymentMethod).isEqualTo(PaymentMethod.CASH)
        assertThat(entity.status).isEqualTo(TransactionStatus.COMPLETED)
    }

    @Test
    fun transactionItemRoundTripsThroughEntity() {
        val item = TransactionItem(
            id = 500L,
            transactionId = 100L,
            productId = 7L,
            variantName = "Dus",
            qty = 2.0,
            unitHpp = 60_000L,
            appliedUnitPrice = 78_000L,
            isNegoPrice = false,
            isWholesaleApplied = true,
            itemDiscount = 1_000L,
            subtotal = 155_000L,
        )

        val restored = TransactionMapper.toModel(TransactionMapper.toEntity(item))

        assertThat(restored).isEqualTo(item)
    }

    @Test
    fun transactionItemKeepsBooleanFlagsAndNullableVariant() {
        val item = TransactionItem(
            id = 0L,
            transactionId = 0L,
            productId = 8L,
            variantName = null,
            qty = 1.5,
            unitHpp = 2_000L,
            appliedUnitPrice = 3_000L,
            isNegoPrice = true,
            isWholesaleApplied = false,
            itemDiscount = 0L,
            subtotal = 4_500L,
        )

        val entity = TransactionMapper.toEntity(item)

        assertThat(entity.variantName).isNull()
        assertThat(entity.qty).isEqualTo(1.5)
        assertThat(entity.isNegoPrice).isTrue()
        assertThat(entity.isWholesaleApplied).isFalse()
        assertThat(entity.appliedUnitPrice).isEqualTo(3_000L)
    }

    @Test
    fun transactionItemCarriesConversionFactorThroughEntity() {
        val item = TransactionItem(
            id = 0L,
            transactionId = 0L,
            productId = 9L,
            variantName = "Dus",
            qty = 5.0,
            unitHpp = 40_000L,
            appliedUnitPrice = 75_000L,
            isNegoPrice = false,
            isWholesaleApplied = true,
            itemDiscount = 0L,
            subtotal = 375_000L,
            conversionFactor = 40.0,
        )

        val entity = TransactionMapper.toEntity(item)

        assertThat(entity.conversionFactor).isEqualTo(40.0)
        assertThat(TransactionMapper.toModel(entity)).isEqualTo(item)
    }

    @Test
    fun transactionItemWithoutKnownFactorMapsToNullColumn() {
        val item = TransactionItem(
            id = 0L,
            transactionId = 0L,
            productId = 10L,
            variantName = null,
            qty = 1.0,
            unitHpp = 1_000L,
            appliedUnitPrice = 2_000L,
            isNegoPrice = false,
            isWholesaleApplied = false,
            itemDiscount = 0L,
            subtotal = 2_000L,
            conversionFactor = null,
        )

        val entity = TransactionMapper.toEntity(item)

        assertThat(entity.conversionFactor).isNull()
        assertThat(TransactionMapper.toModel(entity)).isEqualTo(item)
    }
}
