package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.PricedCartLine
import id.umkm.pos.pro.core.model.PricingContext
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit

private data class LineComputation(
    val pricedLine: PricedCartLine,
    val hppContribution: Double,
)

class DefaultPriceEngine : PriceEngine {

    override fun priceLine(line: CartLine, product: Product, units: List<ProductUnit>): PricedCartLine {
        val purchasedQuantity = line.qty.coerceAtLeast(0.0)
        val selectedUnit = line.unitId?.let { unitId -> units.firstOrNull { it.id == unitId } }
        val conversionFactor = resolveConversionFactor(line, units)
        val negotiatedUnitPrice = line.negotiatedUnitPrice?.takeIf { it >= 0L }
        val wholesalePrice = findEligibleWholesalePrice(units, conversionFactor, purchasedQuantity)

        val unitPrice: Long
        val isNegotiated: Boolean
        val isWholesale: Boolean
        when {
            negotiatedUnitPrice != null -> {
                unitPrice = negotiatedUnitPrice
                isNegotiated = true
                isWholesale = false
            }

            wholesalePrice != null -> {
                unitPrice = wholesalePrice
                isNegotiated = false
                isWholesale = true
            }

            selectedUnit != null -> {
                unitPrice = selectedUnit.unitPrice
                isNegotiated = false
                isWholesale = false
            }

            else -> {
                unitPrice = product.basePrice
                isNegotiated = false
                isWholesale = false
            }
        }

        val itemDiscount = line.itemDiscount.coerceAtLeast(0L)
        val itemSubtotal = (roundHalfUp(purchasedQuantity * unitPrice) - itemDiscount).coerceAtLeast(0L)
        val unitHpp = roundHalfUp(conversionFactor * product.baseHpp)

        return PricedCartLine(
            line = line,
            product = product,
            unitPrice = unitPrice,
            isNegotiated = isNegotiated,
            isWholesale = isWholesale,
            itemSubtotal = itemSubtotal,
            unitHpp = unitHpp,
            conversionFactor = conversionFactor,
        )
    }

    override fun priceCart(
        lines: List<CartLine>,
        products: Map<Long, Product>,
        unitsByProduct: Map<Long, List<ProductUnit>>,
        context: PricingContext,
    ): PricedCart {
        val computations = lines.map { line ->
            val product = products[line.productId]
                ?: throw IllegalArgumentException(
                    "cart line ${line.lineId} references unknown product ${line.productId}",
                )
            val units = unitsByProduct[line.productId].orEmpty()
            val baseQuantity = line.qty.coerceAtLeast(0.0) * resolveConversionFactor(line, units)
            LineComputation(
                pricedLine = priceLine(line, product, units),
                hppContribution = baseQuantity * product.baseHpp,
            )
        }
        val pricedLines = computations.map { it.pricedLine }

        val subtotal = pricedLines.sumOf { it.itemSubtotal }
        val invoiceDiscount = context.invoiceDiscount.coerceAtLeast(0L)
        val invoiceNet = (subtotal - invoiceDiscount).coerceAtLeast(0L)
        val taxRatePercent = context.taxRatePercent.coerceAtLeast(0)
        val taxTotal = floorToWhole(invoiceNet.toDouble() * taxRatePercent / PERCENT_SCALE)
        val rounding = computeRoundingAdjustment(invoiceNet + taxTotal, context.roundingIncrement)
        val grandTotal = invoiceNet + taxTotal + rounding
        val totalHpp = floorToWhole(
            computations.fold(0.0) { accumulatedHpp, computation -> accumulatedHpp + computation.hppContribution },
        )
        val netProfit = invoiceNet - totalHpp

        return PricedCart(
            lines = pricedLines,
            subtotal = subtotal,
            invoiceDiscount = invoiceDiscount,
            taxTotal = taxTotal,
            rounding = rounding,
            grandTotal = grandTotal,
            totalHpp = totalHpp,
            netProfit = netProfit,
        )
    }

    private fun findEligibleWholesalePrice(
        units: List<ProductUnit>,
        conversionFactor: Double,
        purchasedQuantity: Double,
    ): Long? = units.asSequence()
        .filter { it.conversionFactor == conversionFactor }
        .filter { it.wholesaleMinQty != null && it.wholesalePrice != null }
        .filter { purchasedQuantity >= it.wholesaleMinQty!!.toDouble() }
        .maxByOrNull { it.wholesaleMinQty!! }
        ?.wholesalePrice

    private fun resolveConversionFactor(line: CartLine, units: List<ProductUnit>): Double =
        line.unitId?.let { unitId -> units.firstOrNull { it.id == unitId }?.conversionFactor } ?: BASE_UNIT_CONVERSION_FACTOR

    private fun computeRoundingAdjustment(amount: Long, roundingIncrement: Long): Long {
        if (roundingIncrement <= 0L) return 0L
        val roundedAmount = roundHalfUp(amount.toDouble() / roundingIncrement) * roundingIncrement
        return roundedAmount - amount
    }

    private companion object {
        const val BASE_UNIT_CONVERSION_FACTOR = 1.0
        const val PERCENT_SCALE = 100.0
    }
}
