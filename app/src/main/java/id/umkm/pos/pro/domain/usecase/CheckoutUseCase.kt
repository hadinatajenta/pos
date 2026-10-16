package id.umkm.pos.pro.domain.usecase

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.PriceEngine
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.TransactionRepository
import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.PricingContext
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionItem
import id.umkm.pos.pro.domain.pos.PaymentAllocation
import id.umkm.pos.pro.domain.pos.allocatePayment

data class CheckoutLine(
    val productId: Long,
    val unitId: Long?,
    val qty: Double,
    val negotiatedUnitPrice: Long?,
    val itemDiscount: Long,
)

data class CheckoutRequest(
    val lines: List<CheckoutLine>,
    val customerId: Long?,
    val paymentMethod: PaymentMethod,
    val paidAmount: Long,
    val dueDate: Long?,
    val invoiceDiscount: Long,
    val taxRatePercent: Int,
    val roundingIncrement: Long,
    val notes: String?,
)

data class CheckoutResult(
    val transactionId: Long,
    val invoiceNo: String,
    val pricedCart: PricedCart,
    val allocation: PaymentAllocation,
)

class CheckoutUseCase(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val priceEngine: PriceEngine,
    private val clock: Clock,
) {

    suspend operator fun invoke(request: CheckoutRequest): AppResult<CheckoutResult> {
        if (request.lines.isEmpty()) {
            return AppResult.Failure(AppError.Validation("cart requires at least one line"))
        }

        val products = LinkedHashMap<Long, Product>()
        val unitsByProduct = LinkedHashMap<Long, List<ProductUnit>>()
        for (line in request.lines) {
            if (!products.containsKey(line.productId)) {
                val product = productRepository.getProductById(line.productId)
                    ?: return AppResult.Failure(AppError.NotFound("product ${line.productId} was not found"))
                products[line.productId] = product
                unitsByProduct[line.productId] = productRepository.getUnits(line.productId)
            }
        }

        val cartLines = request.lines.mapIndexed { index, line ->
            CartLine(
                lineId = "L${index + 1}",
                productId = line.productId,
                unitId = line.unitId,
                variantName = resolveVariantName(line, unitsByProduct),
                qty = line.qty,
                negotiatedUnitPrice = line.negotiatedUnitPrice,
                itemDiscount = line.itemDiscount,
            )
        }

        val pricedCart = priceEngine.priceCart(
            lines = cartLines,
            products = products,
            unitsByProduct = unitsByProduct,
            context = PricingContext(
                invoiceDiscount = request.invoiceDiscount,
                taxRatePercent = request.taxRatePercent,
                roundingIncrement = request.roundingIncrement,
            ),
        )

        val allocation = allocatePayment(request.paymentMethod, request.paidAmount, pricedCart.grandTotal)
        val createdAt = clock.nowMillis()
        val invoiceNo = transactionRepository.nextInvoiceNumber(createdAt)
        val itemDiscountTotal = pricedCart.lines.sumOf { it.line.itemDiscount }

        val transaction = Transaction(
            id = 0L,
            invoiceNo = invoiceNo,
            createdAt = createdAt,
            cashierId = null,
            customerId = request.customerId,
            subtotal = pricedCart.subtotal,
            discountTotal = itemDiscountTotal + pricedCart.invoiceDiscount,
            invoiceDiscount = pricedCart.invoiceDiscount,
            taxTotal = pricedCart.taxTotal,
            rounding = pricedCart.rounding,
            grandTotal = pricedCart.grandTotal,
            totalHpp = pricedCart.totalHpp,
            netProfit = pricedCart.netProfit,
            paymentMethod = request.paymentMethod,
            paidAmount = allocation.paidAmount,
            changeAmount = allocation.changeAmount,
            remainingDebt = allocation.remainingDebt,
            dueDate = if (allocation.remainingDebt > 0L) request.dueDate else null,
            status = allocation.status,
            notes = request.notes,
        )

        val items = ArrayList<TransactionItem>(pricedCart.lines.size)
        for (pricedLine in pricedCart.lines) {
            val conversionFactor = pricedLine.conversionFactor
            if (!conversionFactor.isFinite() || conversionFactor <= 0.0) {
                return AppResult.Failure(
                    AppError.Validation("invalid unit conversion factor for ${pricedLine.product.name}"),
                )
            }
            items += TransactionItem(
                id = 0L,
                transactionId = 0L,
                productId = pricedLine.product.id,
                variantName = pricedLine.line.variantName,
                qty = pricedLine.line.qty,
                unitHpp = pricedLine.unitHpp,
                appliedUnitPrice = pricedLine.unitPrice,
                isNegoPrice = pricedLine.isNegotiated,
                isWholesaleApplied = pricedLine.isWholesale,
                itemDiscount = pricedLine.line.itemDiscount,
                subtotal = pricedLine.itemSubtotal,
                conversionFactor = conversionFactor,
            )
        }

        return when (val saved = transactionRepository.saveTransaction(transaction, items)) {
            is AppResult.Success -> AppResult.Success(
                CheckoutResult(
                    transactionId = saved.value,
                    invoiceNo = invoiceNo,
                    pricedCart = pricedCart,
                    allocation = allocation,
                ),
            )
            is AppResult.Failure -> saved
        }
    }

    private fun resolveVariantName(
        line: CheckoutLine,
        unitsByProduct: Map<Long, List<ProductUnit>>,
    ): String? = line.unitId?.let { unitId ->
        unitsByProduct[line.productId]?.firstOrNull { it.id == unitId }?.variantName
    }
}
