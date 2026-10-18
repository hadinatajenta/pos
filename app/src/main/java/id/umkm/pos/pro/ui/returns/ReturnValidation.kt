package id.umkm.pos.pro.ui.returns

import id.umkm.pos.pro.domain.pos.ReturnLineRequest
import id.umkm.pos.pro.ui.common.UiText

private val invalidQuantityMessage = UiText.from { strings -> strings.returnValidationInvalidQuantity }
private val positiveQuantityMessage = UiText.from { strings -> strings.returnValidationPositiveQuantity }
private val exceedsPurchasedMessage = UiText.from { strings -> strings.returnValidationExceedsPurchased }
private val nothingSelectedMessage = UiText.from { strings -> strings.returnValidationNothingSelected }

data class ReturnValidation(
    val lineErrors: Map<Long, UiText>,
    val formError: UiText?,
) {
    val isValid: Boolean get() = lineErrors.isEmpty() && formError == null
}

fun validateReturn(lines: List<ReturnLineInput>): ReturnValidation {
    val lineErrors = LinkedHashMap<Long, UiText>()
    var returnedLineCount = 0
    for (line in lines) {
        val trimmedQuantity = line.returnQty.trim()
        if (trimmedQuantity.isEmpty()) continue
        val parsedQuantity = trimmedQuantity.toDoubleOrNull()
        if (parsedQuantity == null || !parsedQuantity.isFinite()) {
            lineErrors[line.transactionItemId] = invalidQuantityMessage
        } else if (parsedQuantity <= 0.0) {
            lineErrors[line.transactionItemId] = positiveQuantityMessage
        } else if (parsedQuantity > line.purchasedQty) {
            lineErrors[line.transactionItemId] = exceedsPurchasedMessage
        } else {
            returnedLineCount += 1
        }
    }
    val formError = if (returnedLineCount == 0 && lineErrors.isEmpty()) nothingSelectedMessage else null
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
