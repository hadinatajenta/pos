package id.umkm.pos.pro.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.SettingsRepository
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.toUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShiftUiState())
    val state: StateFlow<ShiftUiState> = _state.asStateFlow()

    private var defaultCashierName: String = ""
    private var loadedSummaryShiftId: Long? = null

    init {
        observeActiveShift()
        observeSettings()
    }

    fun onOpenShiftForm() {
        viewModelScope.launch {
            val activeShift = try {
                shiftRepository.getActiveShift()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        mode = ShiftSheetMode.SHIFT_FORM,
                        submitErrorMessage = AppError.Storage(error.message ?: "").toUiText(),
                    )
                }
                return@launch
            }
            applyActiveShift(activeShift)
            if (activeShift != null && !_state.value.isSummaryLoading) {
                loadSummary(activeShift.id)
            }
            val recentCashierNames = try {
                shiftRepository.getRecentCashierNames(RECENT_CASHIER_LIMIT)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                emptyList()
            }
            _state.update { current ->
                current.copy(
                    mode = ShiftSheetMode.SHIFT_FORM,
                    cashierNameInput = if (activeShift == null) defaultCashierName else "",
                    startingCashInput = "",
                    actualEndingCashInput = "",
                    nextCashierNameInput = "",
                    nextStartingCashInput = "",
                    isVarianceAcknowledged = false,
                    validationMessage = null,
                    submitErrorMessage = null,
                    recentCashierNames = recentCashierNames,
                )
            }
        }
    }

    fun onDismissForm() {
        _state.update {
            it.copy(
                mode = ShiftSheetMode.HIDDEN,
                validationMessage = null,
                submitErrorMessage = null,
                isVarianceAcknowledged = false,
            )
        }
    }

    fun onCashierNameChange(value: String) {
        _state.update { it.copy(cashierNameInput = value.take(MAX_CASHIER_NAME_LENGTH), validationMessage = null) }
    }

    fun onStartingCashChange(value: String) {
        _state.update { it.copy(startingCashInput = value.filter(Char::isDigit), validationMessage = null) }
    }

    fun onActualEndingCashChange(value: String) {
        val digits = value.filter(Char::isDigit)
        _state.update { current ->
            val shouldSyncNextStartingCash =
                current.nextStartingCashInput.isEmpty() ||
                    current.nextStartingCashInput == current.actualEndingCashInput
            current.copy(
                actualEndingCashInput = digits,
                nextStartingCashInput = if (shouldSyncNextStartingCash) {
                    digits
                } else {
                    current.nextStartingCashInput
                },
                isVarianceAcknowledged = false,
                validationMessage = null,
            )
        }
    }

    fun onNextCashierNameChange(value: String) {
        _state.update {
            it.copy(
                nextCashierNameInput = value.take(MAX_CASHIER_NAME_LENGTH),
                validationMessage = null,
            )
        }
    }

    fun onNextStartingCashChange(value: String) {
        _state.update {
            it.copy(nextStartingCashInput = value.filter(Char::isDigit), validationMessage = null)
        }
    }

    fun onVarianceAcknowledgedChange(isAcknowledged: Boolean) {
        _state.update { it.copy(isVarianceAcknowledged = isAcknowledged, validationMessage = null) }
    }

    fun onOpenShift() {
        val current = _state.value
        if (current.isSubmitting) return
        if (current.activeShift != null) {
            onOpenShiftForm()
            return
        }
        val cashierName = current.cashierNameInput.trim()
        if (cashierName.isEmpty()) {
            setValidation { strings -> strings.shiftValidationCashierName }
            return
        }
        val startingCash = current.startingCashInput.toLongOrNull()
        if (startingCash == null) {
            setValidation { strings -> strings.shiftValidationStartingCash }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, validationMessage = null, submitErrorMessage = null) }
            when (val result = shiftRepository.openShift(cashierName, startingCash)) {
                is AppResult.Success -> {
                    loadedSummaryShiftId = null
                    _state.update {
                        it.copy(
                            mode = ShiftSheetMode.HIDDEN,
                            activeShift = result.value,
                            summary = null,
                            isSubmitting = false,
                            cashierNameInput = "",
                            startingCashInput = "",
                            message = UiText.from { strings -> strings.shiftOpenSuccess(result.value.cashierName) },
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, submitErrorMessage = result.error.toUiText())
                }
            }
        }
    }

    fun onCloseShift() {
        val current = _state.value
        val activeShift = current.activeShift ?: return
        if (current.isSubmitting) return
        val actualEndingCash = resolveActualEndingCash(current) ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, validationMessage = null, submitErrorMessage = null) }
            when (val result = shiftRepository.closeShift(activeShift.id, actualEndingCash)) {
                is AppResult.Success -> {
                    loadedSummaryShiftId = null
                    _state.update {
                        it.copy(
                            mode = ShiftSheetMode.HIDDEN,
                            activeShift = null,
                            summary = null,
                            isSubmitting = false,
                            actualEndingCashInput = "",
                            nextCashierNameInput = "",
                            nextStartingCashInput = "",
                            isVarianceAcknowledged = false,
                            message = UiText.from { strings ->
                                strings.shiftCloseSuccess(result.value.cashierName)
                            },
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, submitErrorMessage = result.error.toUiText())
                }
            }
        }
    }

    fun onSwitchShift() {
        val current = _state.value
        val activeShift = current.activeShift ?: return
        if (current.isSubmitting) return
        val actualEndingCash = resolveActualEndingCash(current) ?: return
        val nextCashierName = current.nextCashierNameInput.trim()
        if (nextCashierName.isEmpty()) {
            setValidation { strings -> strings.shiftValidationNextCashier }
            return
        }
        val nextStartingCash = current.nextStartingCashInput.toLongOrNull()
        if (nextStartingCash == null) {
            setValidation { strings -> strings.shiftValidationNextStartingCash }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, validationMessage = null, submitErrorMessage = null) }
            val result = shiftRepository.switchShift(
                closeShiftId = activeShift.id,
                actualEndingCash = actualEndingCash,
                nextCashierName = nextCashierName,
                nextStartingCash = nextStartingCash,
            )
            when (result) {
                is AppResult.Success -> {
                    loadedSummaryShiftId = null
                    _state.update {
                        it.copy(
                            mode = ShiftSheetMode.HIDDEN,
                            activeShift = result.value.openedShift,
                            summary = null,
                            isSubmitting = false,
                            actualEndingCashInput = "",
                            nextCashierNameInput = "",
                            nextStartingCashInput = "",
                            isVarianceAcknowledged = false,
                            message = UiText.from { strings ->
                                strings.shiftSwitchSuccess(
                                    result.value.closedShift.cashierName,
                                    result.value.openedShift.cashierName,
                                )
                            },
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, submitErrorMessage = result.error.toUiText())
                }
            }
        }
    }

    fun onOpenHistory() {
        _state.update { it.copy(mode = ShiftSheetMode.HISTORY, historyErrorMessage = null) }
        loadHistory()
    }

    fun onDismissHistory() {
        _state.update { it.copy(mode = ShiftSheetMode.HIDDEN, historyErrorMessage = null) }
    }

    fun onRetryHistory() {
        loadHistory()
    }

    fun onMessageConsumed() {
        _state.update { it.copy(message = null) }
    }

    private fun observeActiveShift() {
        viewModelScope.launch {
            shiftRepository.observeActiveShift().collect { shift -> applyActiveShift(shift) }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                defaultCashierName = settings.cashierName?.trim().orEmpty()
                _state.update { current ->
                    if (current.mode == ShiftSheetMode.SHIFT_FORM &&
                        current.activeShift == null &&
                        current.cashierNameInput.isBlank()
                    ) {
                        current.copy(cashierNameInput = defaultCashierName)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun applyActiveShift(shift: CashierShift?) {
        val previousShiftId = _state.value.activeShift?.id
        _state.update { it.copy(activeShift = shift) }
        if (shift == null) {
            loadedSummaryShiftId = null
            _state.update {
                it.copy(summary = null, isSummaryLoading = false, summaryErrorMessage = null)
            }
            return
        }
        if (shift.id != previousShiftId || shift.id != loadedSummaryShiftId) {
            loadSummary(shift.id)
        }
    }

    private fun loadSummary(shiftId: Long) {
        loadedSummaryShiftId = shiftId
        _state.update {
            it.copy(
                summary = if (it.activeShift?.id == shiftId) it.summary else null,
                isSummaryLoading = true,
                summaryErrorMessage = null,
            )
        }
        viewModelScope.launch {
            val result = try {
                shiftRepository.getShiftSummary(shiftId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AppResult.Failure(AppError.Storage(error.message ?: ""))
            }
            if (_state.value.activeShift?.id != shiftId) return@launch
            when (result) {
                is AppResult.Success -> _state.update {
                    it.copy(summary = result.value, isSummaryLoading = false, summaryErrorMessage = null)
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isSummaryLoading = false, summaryErrorMessage = result.error.toUiText())
                }
            }
        }
    }

    private fun loadHistory() {
        _state.update { it.copy(isHistoryLoading = true, historyErrorMessage = null) }
        viewModelScope.launch {
            val shifts = try {
                shiftRepository.getShiftHistory(HISTORY_LIMIT)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isHistoryLoading = false,
                        historyErrorMessage = AppError.Storage(error.message ?: "").toUiText(),
                    )
                }
                return@launch
            }
            val activeShiftId = _state.value.activeShift?.id
            val liveSummary = try {
                activeShiftId?.let { shiftId ->
                    when (val summaryResult = shiftRepository.getShiftSummary(shiftId)) {
                        is AppResult.Success -> summaryResult.value
                        is AppResult.Failure -> null
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            _state.update {
                it.copy(
                    history = shifts.map { shift ->
                        ShiftHistoryEntry(
                            shift = shift,
                            liveExpectedCash = if (shift.id == activeShiftId) liveSummary?.expectedEndingCash else null,
                        )
                    },
                    isHistoryLoading = false,
                    historyErrorMessage = null,
                )
            }
        }
    }

    private fun resolveActualEndingCash(current: ShiftUiState): Long? {
        val summary = current.summary
        if (summary == null) {
            setValidation { strings -> strings.shiftSummaryUnavailable }
            return null
        }
        val actualEndingCash = current.actualEndingCashInput.toLongOrNull()
        if (actualEndingCash == null) {
            setValidation { strings -> strings.shiftValidationActualCash }
            return null
        }
        if (actualEndingCash != summary.expectedEndingCash && !current.isVarianceAcknowledged) {
            setValidation { strings -> strings.shiftValidationVarianceAck }
            return null
        }
        return actualEndingCash
    }

    private fun setValidation(message: (id.umkm.pos.pro.ui.common.PosStrings) -> String) {
        _state.update {
            it.copy(validationMessage = UiText.from(message), submitErrorMessage = null)
        }
    }

    private companion object {
        const val RECENT_CASHIER_LIMIT = 5
        const val HISTORY_LIMIT = 30
        const val MAX_CASHIER_NAME_LENGTH = 60
    }
}
