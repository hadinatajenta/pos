package id.umkm.pos.pro.data.seed

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.PaymentMethod
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DemoSalesPlanTest {

    private val sales = DemoSalesPlan.build()

    @Test
    fun planProducesConfiguredTransactionCount() {
        assertThat(sales).hasSize(DemoSalesPlan.TRANSACTION_COUNT)
        assertThat(DemoSalesPlan.DAY_SALE_COUNTS.sum()).isEqualTo(DemoSalesPlan.TRANSACTION_COUNT)
    }

    @Test
    fun salesStayWithinThePreviousThirtyDays() {
        sales.forEach { sale ->
            assertThat(sale.dayOffset).isAtLeast(1)
            assertThat(sale.dayOffset).isAtMost(DemoSalesPlan.DAY_SPAN)
            assertThat(sale.minuteOfDay).isAtLeast(0)
            assertThat(sale.minuteOfDay).isLessThan(24 * 60)
        }
    }

    @Test
    fun someDaysHaveNoSales() {
        val activeDays = sales.map { it.dayOffset }.toSet()

        assertThat(activeDays.size).isLessThan(DemoSalesPlan.DAY_SPAN)
    }

    @Test
    fun everySaleHasValidLines() {
        sales.forEach { sale ->
            assertThat(sale.lines).isNotEmpty()
            sale.lines.forEach { line ->
                assertThat(line.productIndex).isAtLeast(0)
                assertThat(line.productIndex).isLessThan(DemoCatalog.products.size)
                assertThat(line.qty).isGreaterThan(0.0)
                assertThat(line.itemDiscount).isAtLeast(0L)
                line.unitIndex?.let { unitIndex ->
                    assertThat(unitIndex).isAtLeast(0)
                    assertThat(unitIndex).isLessThan(DemoCatalog.products[line.productIndex].units.size)
                }
                line.negotiatedUnitPrice?.let { price -> assertThat(price).isGreaterThan(0L) }
            }
        }
    }

    @Test
    fun multiplePaymentMethodsAreUsedIncludingOneKasbon() {
        val methods = sales.map { it.paymentMethod }.toSet()

        assertThat(methods).contains(PaymentMethod.CASH)
        assertThat(methods).contains(PaymentMethod.QRIS)
        assertThat(methods).contains(PaymentMethod.TRANSFER)
        assertThat(methods).contains(PaymentMethod.SPLIT)
        assertThat(methods).contains(PaymentMethod.KASBON)
        assertThat(sales.count { it.paymentMethod == PaymentMethod.KASBON }).isEqualTo(1)
    }

    @Test
    fun kasbonSaleRequiresCustomerAndPartialSettlement() {
        val kasbon = sales[DemoSalesPlan.KASBON_SALE_INDEX]

        assertThat(kasbon.paymentMethod).isEqualTo(PaymentMethod.KASBON)
        assertThat(kasbon.settlement).isEqualTo(DemoSettlement.PARTIAL_HALF)
        assertThat(kasbon.customerIndex).isNotNull()
        assertThat(kasbon.dueOffsetDays).isNotNull()
    }

    @Test
    fun cashSalesTenderRoundedChangeWhileFullSettlementsDoNot() {
        val cashSales = sales.filter { it.paymentMethod == PaymentMethod.CASH }

        assertThat(cashSales).isNotEmpty()
        cashSales.forEach { sale ->
            assertThat(sale.settlement).isEqualTo(DemoSettlement.CASH_ROUNDED)
        }
        sales.filter { it.paymentMethod != PaymentMethod.CASH &&
            it.settlement != DemoSettlement.PARTIAL_HALF }
            .forEach { sale -> assertThat(sale.settlement).isEqualTo(DemoSettlement.FULL) }
    }

    @Test
    fun negotiatedAndWholesaleLinesArePresent() {
        val negotiated = sales[DemoSalesPlan.NEGOTIATED_SALE_INDEX].lines
            .any { it.negotiatedUnitPrice != null }
        val wholesale = sales[DemoSalesPlan.WHOLESALE_SALE_INDEX].lines
            .any { it.unitIndex != null }

        assertThat(negotiated).isTrue()
        assertThat(wholesale).isTrue()
    }

    @Test
    fun returnTargetSaleHasAReturnableLine() {
        val target = sales[DemoSalesPlan.RETURN_SALE_INDEX]
        val line = target.lines.getOrNull(DemoSalesPlan.RETURN_LINE_INDEX)

        assertThat(line).isNotNull()
        assertThat(line!!.qty).isAtLeast(DemoSalesPlan.RETURN_QTY)
    }

    @Test
    fun salesGenerationIsDeterministic() {
        assertThat(DemoSalesPlan.build()).isEqualTo(sales)
    }

    @Test
    fun initialStockRestoresTheDesiredEndingStock() {
        val netBaseQty = DemoStockPlan.netBaseQtyByProduct(sales)

        assertThat(netBaseQty).hasSize(DemoCatalog.products.size)
        DemoCatalog.products.forEachIndexed { index, spec ->
            val initialStock = DemoStockPlan.initialStock(spec, netBaseQty[index])
            val endingStock = initialStock - netBaseQty[index]
            assertThat(endingStock).isWithin(1e-9).of(spec.desiredRemainingStock)
        }
    }

    @Test
    fun soldOutAndOversoldProductsHavePositiveOpeningStock() {
        val netBaseQty = DemoStockPlan.netBaseQtyByProduct(sales)
        val berasOpening = DemoStockPlan.initialStock(
            DemoCatalog.products[DemoCatalog.berasIndex],
            netBaseQty[DemoCatalog.berasIndex],
        )
        val minyakOpening = DemoStockPlan.initialStock(
            DemoCatalog.products[DemoCatalog.minyakGorengIndex],
            netBaseQty[DemoCatalog.minyakGorengIndex],
        )

        assertThat(berasOpening).isGreaterThan(0.0)
        assertThat(minyakOpening).isGreaterThan(0.0)
    }

    @Test
    fun localDayStartTruncatesTimeOfDay() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val millis = 1_700_000_000_000L
        val start = DemoDayStart.localDayStartMillis(millis, timeZone)
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = start }

        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(calendar.get(Calendar.SECOND)).isEqualTo(0)
        assertThat(calendar.get(Calendar.MILLISECOND)).isEqualTo(0)
        assertThat(start).isAtMost(millis)
    }

    @Test
    fun saleMillisPlacesSaleBehindTheAnchorByDayOffset() {
        val anchorDayStart = 1_700_000_000_000L
        val sale = DemoSaleSpec(
            dayOffset = 2,
            minuteOfDay = 60,
            lines = listOf(DemoLineSpec(productIndex = 0, qty = 1.0)),
            paymentMethod = PaymentMethod.CASH,
            settlement = DemoSettlement.CASH_ROUNDED,
        )
        val expected = anchorDayStart -
            2 * DemoSchedule.MILLIS_PER_DAY +
            60 * DemoSchedule.MILLIS_PER_MINUTE

        assertThat(DemoSchedule.saleMillis(anchorDayStart, sale)).isEqualTo(expected)
    }
}
