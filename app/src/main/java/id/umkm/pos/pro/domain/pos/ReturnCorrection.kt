package id.umkm.pos.pro.domain.pos

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.TransactionItem

data class ReturnLineRequest(
    val transactionItemId: Long,
    val returnedQty: Double,
    val conversionFactor: Double = 1.0,
)

data class ReturnLineCorrection(
    val transactionItemId: Long,
    val productId: Long,
    val variantName: String?,
    val returnedQty: Double,
    val baseQtyRestocked: Double,
    val hppReversed: Long,
    val refundAmount: Long,
)

data class ReturnCorrection(
    val lines: List<ReturnLineCorrection>,
    val totalRefund: Long,
    val totalHppReversed: Long,
    val totalBaseQtyRestocked: Double,
)

data class ExchangeSettlement(
    val refundToCustomer: Long,
    val amountDueFromCustomer: Long,
)

fun correctReturn(
    items: List<TransactionItem>,
    requests: List<ReturnLineRequest>,
    productsById: Map<Long, Product>,
): AppResult<ReturnCorrection> {
    val itemsById = items.associateBy { it.id }
    val corrections = ArrayList<ReturnLineCorrection>(requests.size)

    for (request in requests) {
        val originalItem = itemsById[request.transactionItemId]
            ?: return AppResult.Failure(
                AppError.NotFound("transaction item ${request.transactionItemId} not found"),
            )
        val product = productsById[originalItem.productId]
            ?: return AppResult.Failure(AppError.NotFound("product ${originalItem.productId} not found"))

        if (request.returnedQty <= 0.0) {
            return AppResult.Failure(AppError.Validation("returned quantity must be positive"))
        }
        if (request.returnedQty > originalItem.qty) {
            return AppResult.Failure(
                AppError.Validation("returned quantity exceeds purchased quantity for item ${originalItem.id}"),
            )
        }
        if (request.conversionFactor <= 0.0) {
            return AppResult.Failure(
                AppError.Validation("conversion factor must be positive for item ${originalItem.id}"),
            )
        }

        val baseQtyRestocked = request.returnedQty * request.conversionFactor
        val hppReversed = floorToWhole(baseQtyRestocked * product.baseHpp)
        val refundAmount = if (originalItem.qty > 0.0) {
            roundHalfUp(request.returnedQty / originalItem.qty * originalItem.subtotal)
        } else {
            0L
        }

        corrections += ReturnLineCorrection(
            transactionItemId = originalItem.id,
            productId = originalItem.productId,
            variantName = originalItem.variantName,
            returnedQty = request.returnedQty,
            baseQtyRestocked = baseQtyRestocked,
            hppReversed = hppReversed,
            refundAmount = refundAmount,
        )
    }

    return AppResult.Success(
        ReturnCorrection(
            lines = corrections,
            totalRefund = corrections.sumOf { it.refundAmount },
            totalHppReversed = corrections.sumOf { it.hppReversed },
            totalBaseQtyRestocked = corrections.fold(0.0) { accumulated, correction ->
                accumulated + correction.baseQtyRestocked
            },
        ),
    )
}

fun settleExchange(returnCorrection: ReturnCorrection, newSaleGrandTotal: Long): ExchangeSettlement {
    val settlementAmount = newSaleGrandTotal.coerceAtLeast(0L) - returnCorrection.totalRefund
    return if (settlementAmount >= 0L) {
        ExchangeSettlement(refundToCustomer = 0L, amountDueFromCustomer = settlementAmount)
    } else {
        ExchangeSettlement(refundToCustomer = -settlementAmount, amountDueFromCustomer = 0L)
    }
}
