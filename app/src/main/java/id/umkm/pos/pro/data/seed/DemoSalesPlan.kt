package id.umkm.pos.pro.data.seed

import id.umkm.pos.pro.core.model.PaymentMethod
import java.util.Calendar
import java.util.TimeZone

data class DemoLineSpec(
    val productIndex: Int,
    val qty: Double,
    val unitIndex: Int? = null,
    val negotiatedUnitPrice: Long? = null,
    val itemDiscount: Long = 0L,
)

enum class DemoSettlement {
    FULL,
    CASH_ROUNDED,
}

data class DemoSaleSpec(
    val dayOffset: Int,
    val minuteOfDay: Int,
    val lines: List<DemoLineSpec>,
    val paymentMethod: PaymentMethod,
    val settlement: DemoSettlement,
    val invoiceDiscount: Long = 0L,
)

object DemoSchedule {

    const val MILLIS_PER_DAY: Long = 86_400_000L
    const val MILLIS_PER_MINUTE: Long = 60_000L

    fun saleMillis(anchorDayStartMillis: Long, sale: DemoSaleSpec): Long =
        anchorDayStartMillis - sale.dayOffset * MILLIS_PER_DAY + sale.minuteOfDay * MILLIS_PER_MINUTE
}

object DemoDayStart {

    fun localDayStartMillis(millis: Long, timeZone: TimeZone): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

object DemoSalesPlan {

    private const val SEED: Long = 20_240_517L

    const val DAY_SPAN: Int = 30
    const val TRANSACTION_COUNT: Int = 45

    const val KASBON_SALE_INDEX: Int = 6
    const val NEGOTIATED_SALE_INDEX: Int = 7
    const val WHOLESALE_SALE_INDEX: Int = 19
    const val CLEARANCE_SALE_INDEX: Int = 22

    const val RETURN_SALE_INDEX: Int = 1
    const val RETURN_LINE_INDEX: Int = 0
    const val RETURN_QTY: Double = 1.0

    private const val FIRST_SALE_MINUTE: Int = 540
    private const val MINUTES_BETWEEN_SALES: Int = 75
    private const val NEGOTIATED_UNIT_PRICE: Long = 8_500L
    private const val WHOLESALE_LINE_QTY: Double = 3.0
    private const val CLEARANCE_MINYAK_QTY: Double = 2.0
    private const val CLEARANCE_BERAS_QTY: Double = 3.0

    val DAY_SALE_COUNTS: List<Int> = listOf(
        2, 1, 3, 1, 1, 2, 3, 1, 2, 0,
        1, 2, 0, 3, 2, 1, 1, 2, 1, 3,
        1, 2, 1, 2, 0, 1, 3, 2, 1, 0,
    )

    private val PAYMENT_CYCLE: List<PaymentMethod> = listOf(
        PaymentMethod.CASH,
        PaymentMethod.QRIS,
        PaymentMethod.TRANSFER,
        PaymentMethod.CASH,
        PaymentMethod.QRIS,
        PaymentMethod.TRANSFER,
        PaymentMethod.CASH,
    )

    fun build(): List<DemoSaleSpec> {
        val random = DemoRandom(SEED)
        val sales = ArrayList<DemoSaleSpec>(TRANSACTION_COUNT)
        var saleIndex = 0

        DAY_SALE_COUNTS.forEachIndexed { slotIndex, saleCount ->
            val dayOffset = DAY_SPAN - slotIndex
            repeat(saleCount) { withinDay ->
                val paymentMethod = PAYMENT_CYCLE[saleIndex % PAYMENT_CYCLE.size]
                sales += DemoSaleSpec(
                    dayOffset = dayOffset,
                    minuteOfDay = FIRST_SALE_MINUTE + withinDay * MINUTES_BETWEEN_SALES,
                    lines = buildLines(random),
                    paymentMethod = paymentMethod,
                    settlement = if (paymentMethod == PaymentMethod.CASH) {
                        DemoSettlement.CASH_ROUNDED
                    } else {
                        DemoSettlement.FULL
                    },
                )
                saleIndex++
            }
        }

        sales[KASBON_SALE_INDEX] = sales[KASBON_SALE_INDEX].copy(
            paymentMethod = PaymentMethod.CASH,
            settlement = DemoSettlement.CASH_ROUNDED,
        )
        sales[NEGOTIATED_SALE_INDEX] = sales[NEGOTIATED_SALE_INDEX].copy(
            lines = sales[NEGOTIATED_SALE_INDEX].lines + DemoLineSpec(
                productIndex = DemoCatalog.kopiSusuIndex,
                qty = 2.0,
                negotiatedUnitPrice = NEGOTIATED_UNIT_PRICE,
            ),
        )
        sales[WHOLESALE_SALE_INDEX] = sales[WHOLESALE_SALE_INDEX].copy(
            lines = sales[WHOLESALE_SALE_INDEX].lines + DemoLineSpec(
                productIndex = DemoCatalog.indomieIndex,
                qty = WHOLESALE_LINE_QTY,
                unitIndex = 0,
            ),
        )
        sales[CLEARANCE_SALE_INDEX] = sales[CLEARANCE_SALE_INDEX].copy(
            lines = sales[CLEARANCE_SALE_INDEX].lines + listOf(
                DemoLineSpec(productIndex = DemoCatalog.minyakGorengIndex, qty = CLEARANCE_MINYAK_QTY),
                DemoLineSpec(productIndex = DemoCatalog.berasIndex, qty = CLEARANCE_BERAS_QTY),
            ),
        )

        return sales
    }

    private fun buildLines(random: DemoRandom): List<DemoLineSpec> {
        val lineCount = 1 + random.nextInt(MAX_LINES_PER_SALE)
        val productIndexes = LinkedHashSet<Int>()
        repeat(lineCount) {
            if (productIndexes.size < DemoCatalog.products.size) {
                productIndexes += random.nextInt(DemoCatalog.products.size)
            }
        }
        if (productIndexes.isEmpty()) {
            productIndexes += 0
        }
        return productIndexes.map { productIndex ->
            DemoLineSpec(
                productIndex = productIndex,
                qty = (1 + random.nextInt(MAX_LINE_QTY)).toDouble(),
                itemDiscount = if (random.nextInt(DISCOUNT_CHANCE) == 0) ITEM_DISCOUNT else 0L,
            )
        }
    }

    private const val MAX_LINES_PER_SALE = 3
    private const val MAX_LINE_QTY = 3
    private const val DISCOUNT_CHANCE = 5
    private const val ITEM_DISCOUNT = 500L
}

private class DemoRandom(seed: Long) {

    private var state: Long = seed

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        state = state * MULTIPLIER + INCREMENT
        return ((state ushr 33).toInt()) % bound
    }

    private companion object {
        const val MULTIPLIER = 6364136223846793005L
        const val INCREMENT = 1442695040888963407L
    }
}
