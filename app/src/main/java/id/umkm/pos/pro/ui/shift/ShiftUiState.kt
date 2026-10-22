package id.umkm.pos.pro.ui.shift

import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.UiText

enum class ShiftSheetMode {
    HIDDEN,
    SHIFT_FORM,
    HISTORY,
}

data class ShiftHistoryEntry(
    val shift: CashierShift,
    val liveExpectedCash: Long? = null,
) {
    val isActive: Boolean get() = shift.closedAt == null

    val expectedEndingCash: Long?
        get() = liveExpectedCash ?: shift.expectedEndingCash

    val variance: Long?
        get() = shift.actualEndingCash?.let { actual ->
            expectedEndingCash?.let { expected -> actual - expected }
        }
}

data class ShiftUiState(
    val mode: ShiftSheetMode = ShiftSheetMode.HIDDEN,
    val activeShift: CashierShift? = null,
    val summary: ShiftCashSummary? = null,
    val isSummaryLoading: Boolean = false,
    val summaryErrorMessage: UiText? = null,
    val staffOptions: List<Staff> = emptyList(),
    val activeStaff: Staff? = null,
    val selectedStaffId: Long? = null,
    val selectedNextStaffId: Long? = null,
    val history: List<ShiftHistoryEntry> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val historyErrorMessage: UiText? = null,
    val startingCashInput: String = "",
    val actualEndingCashInput: String = "",
    val nextStartingCashInput: String = "",
    val isVarianceAcknowledged: Boolean = false,
    val isSubmitting: Boolean = false,
    val validationMessage: UiText? = null,
    val submitErrorMessage: UiText? = null,
    val message: UiText? = null,
) {
    val expectedEndingCash: Long? get() = summary?.expectedEndingCash

    val actualEndingCash: Long? get() = actualEndingCashInput.toLongOrNull()

    val variance: Long?
        get() = actualEndingCash?.let { actual ->
            expectedEndingCash?.let { expected -> actual - expected }
        }

    val isVarianceBalanced: Boolean get() = variance == 0L

    val selectedStaff: Staff? get() = staffOptions.firstOrNull { it.id == selectedStaffId }

    val selectedNextStaff: Staff? get() = staffOptions.firstOrNull { it.id == selectedNextStaffId }

    val isCloseInputReady: Boolean
        get() = activeShift != null &&
            summary != null &&
            actualEndingCash != null &&
            (isVarianceBalanced || isVarianceAcknowledged) &&
            !isSubmitting

    val isSwitchInputReady: Boolean
        get() = isCloseInputReady &&
            selectedNextStaffId != null &&
            nextStartingCashInput.toLongOrNull() != null

    val isOpenInputReady: Boolean
        get() = activeShift == null &&
            selectedStaffId != null &&
            startingCashInput.toLongOrNull() != null &&
            !isSubmitting
}
