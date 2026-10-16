package id.umkm.pos.pro.data.seed

object DemoStockPlan {

    fun netBaseQtyByProduct(sales: List<DemoSaleSpec>): List<Double> {
        val totals = MutableList(DemoCatalog.products.size) { 0.0 }
        sales.forEach { sale ->
            sale.lines.forEach { line ->
                totals[line.productIndex] += soldBaseQty(line)
            }
        }
        val returnLine = sales[DemoSalesPlan.RETURN_SALE_INDEX].lines[DemoSalesPlan.RETURN_LINE_INDEX]
        totals[returnLine.productIndex] -= DemoSalesPlan.RETURN_QTY * conversionFactorFor(returnLine)
        return totals
    }

    fun initialStock(spec: DemoProductSpec, netBaseQty: Double): Double =
        spec.desiredRemainingStock + netBaseQty

    fun soldBaseQty(line: DemoLineSpec): Double = line.qty * conversionFactorFor(line)

    fun conversionFactorFor(line: DemoLineSpec): Double =
        line.unitIndex?.let { unitIndex ->
            DemoCatalog.products[line.productIndex].units[unitIndex].conversionFactor
        } ?: 1.0
}
