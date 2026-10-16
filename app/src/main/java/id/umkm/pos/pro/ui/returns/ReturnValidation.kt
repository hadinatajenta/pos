package id.umkm.pos.pro.ui.returns

import id.umkm.pos.pro.domain.pos.ReturnLineRequest

private const val INVALID_QUANTITY_MESSAGE = "Enter a valid quantity."
private const val POSITIVE_QUANTITY_MESSAGE = "Enter a quantity greater than zero."
private const val EXCEEDS_PURCHASED_MESSAGE = "The return quantity cannot exceed the purchased quantity."
private const val NOTHING_SELECTED_MESSAGE = "Choose at least one item to return."

data class ReturnValidation(
    val lineErrors: Map<Long, String>,
    val formError: String?,
) {
    val isValid: Boolean get() = lineErrors.isEmpty() && formError == null
}

fun validateReturn(lines: List<ReturnLineInput>): ReturnValidation {
    val lineErrors = LinkedHashMap<Long, String>()
    var returnedLineCount = 0
    for (line in lines) {
        val trimmedQuantity = line.returnQty.trim()
        if (trimmedQuantity.isEmpty()) continue
        val parsedQuantity = trimmedQuantity.toDoubleOrNull()
        if (parsedQuantity == null || !parsedQuantity.isFinite()) {
            lineErrors[line.transactionItemId] = INVALID_QUANTITY_MESSAGE
        } else if (parsedQuantity <= 0.0) {
            lineErrors[line.transactionItemId] = POSITIVE_QUANTITY_MESSAGE
        } else if (parsedQuantity > line.purchasedQty) {
            lineErrors[line.transactionItemId] = EXCEEDS_PURCHASED_MESSAGE
        } else {
            returnedLineCount += 1
        }
    }
    val formError = if (returnedLineCount == 0 && lineErrors.isEmpty()) NOTHING_SELECTED_MESSAGE else null
    return ReturnValidation(lineErrors = lineErrors, formError = formError)
}

fun toReturnLineRequests(lines: List<ReturnLineInput>): List<ReturnLineRequest> =
    lines.mapNotNull { line ->
        val parsedQuantity = line.returnQty.trim().toDoubleOrNull()
        if (parsedQuantity == null || !parsedQuantity.isFinite() || parsedQuantity <= 0.0) {
            null
        } else {
            ReturnLineRequest(
                transactionItemId = line.transactionItemId,
                returnedQty = parsedQuantity,
                conversionFactor = null,
            )
        }
    }
