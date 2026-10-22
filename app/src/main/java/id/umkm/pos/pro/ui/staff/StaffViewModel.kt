package id.umkm.pos.pro.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.StaffRepository
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.toUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StaffViewModel(
    private val staffRepository: StaffRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StaffUiState())
    val state: StateFlow<StaffUiState> = _state.asStateFlow()

    init {
        observeStaff()
        observeActiveStaff()
    }

    fun onToggleSearch() {
        _state.update { current ->
            current.copy(
                isSearchVisible = !current.isSearchVisible,
                searchQuery = if (current.isSearchVisible) "" else current.searchQuery,
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onClearSearch() {
        _state.update { it.copy(searchQuery = "") }
    }

    fun onOpenAddEditor() {
        if (_state.value.isSubmitting) return
        _state.update {
            it.copy(
                editor = StaffEditorState(),
                errorMessage = null,
            )
        }
    }

    fun onOpenRenameEditor(staff: Staff) {
        if (_state.value.isSubmitting) return
        _state.update {
            it.copy(
                editor = StaffEditorState(staffId = staff.id, name = staff.name),
                errorMessage = null,
            )
        }
    }

    fun onNameChange(name: String) {
        _state.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(editor = editor.copy(name = name, error = null))
        }
    }

    fun onDismissEditor() {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(editor = null) }
    }

    fun onSaveEditor() {
        val current = _state.value
        val editor = current.editor ?: return
        if (current.isSubmitting) return

        val trimmedName = editor.name.trim()
        val validationError = when {
            trimmedName.isEmpty() -> UiText.from { strings -> strings.staffNameRequired }
            trimmedName.length > MAX_NAME_LENGTH -> UiText.from { strings -> strings.staffNameTooLong }
            else -> null
        }
        if (validationError != null) {
            _state.update { state ->
                state.copy(editor = state.editor?.copy(error = validationError))
            }
            return
        }

        _state.update { state ->
            state.copy(
                isSubmitting = true,
                errorMessage = null,
                editor = state.editor?.copy(error = null),
            )
        }
        viewModelScope.launch {
            val result = try {
                val staffId = editor.staffId
                if (staffId != null) {
                    staffRepository.renameStaff(staffId, trimmedName)
                } else {
                    staffRepository.addStaff(trimmedName)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AppResult.Failure(AppError.Storage(error.message ?: ""))
            }
            when (result) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        editor = null,
                        message = UiText.from { strings -> strings.staffSaved },
                    )
                }

                is AppResult.Failure -> _state.update { state ->
                    state.copy(
                        isSubmitting = false,
                        editor = state.editor?.copy(error = result.error.toStaffUiText()),
                    )
                }
            }
        }
    }

    fun onRequestDeactivate(staff: Staff) {
        val current = _state.value
        if (current.isSubmitting || !staff.isActive) return
        if (current.activeStaffList.size <= 1) {
            _state.update {
                it.copy(
                    errorMessage = UiText.from { strings -> strings.staffLastActiveError },
                    message = null,
                )
            }
            return
        }
        _state.update {
            it.copy(
                pendingDeactivation = staff,
                errorMessage = null,
            )
        }
    }

    fun onConfirmDeactivate() {
        val pending = _state.value.pendingDeactivation ?: return
        if (_state.value.isSubmitting) return
        _state.update {
            it.copy(
                pendingDeactivation = null,
                isSubmitting = true,
                errorMessage = null,
            )
        }
        setStaffActive(pending.id, isActive = false)
    }

    fun onDismissDeactivate() {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(pendingDeactivation = null) }
    }

    fun onActivateStaff(staff: Staff) {
        val current = _state.value
        if (current.isSubmitting || staff.isActive) return
        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        setStaffActive(staff.id, isActive = true)
    }

    fun onMessageConsumed() {
        _state.update { it.copy(message = null, errorMessage = null) }
    }

    private fun setStaffActive(staffId: Long, isActive: Boolean) {
        viewModelScope.launch {
            val result = try {
                staffRepository.setStaffActive(staffId, isActive)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AppResult.Failure(AppError.Storage(error.message ?: ""))
            }
            when (result) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        message = UiText.from { strings ->
                            if (isActive) strings.staffReactivated else strings.staffDeactivated
                        },
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.error.toStaffUiText(),
                    )
                }
            }
        }
    }

    private fun observeStaff() {
        viewModelScope.launch {
            try {
                staffRepository.observeStaff().collect { staffList ->
                    _state.update {
                        it.copy(
                            staffList = sortStaff(staffList),
                            isLoading = false,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = AppError.Storage(error.message ?: "").toUiText(),
                    )
                }
            }
        }
    }

    private fun observeActiveStaff() {
        viewModelScope.launch {
            try {
                staffRepository.observeActiveStaff().collect { staff ->
                    _state.update { it.copy(activeStaffId = staff?.id) }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update {
                    it.copy(errorMessage = AppError.Storage(error.message ?: "").toUiText())
                }
            }
        }
    }

    private fun AppError.toStaffUiText(): UiText = when (this) {
        is AppError.Validation -> when {
            message.contains("at least one active", ignoreCase = true) ->
                UiText.from { strings -> strings.staffLastActiveError }

            message.contains("already exists", ignoreCase = true) ->
                UiText.from { strings -> strings.staffNameDuplicate }

            message.contains("too long", ignoreCase = true) ->
                UiText.from { strings -> strings.staffNameTooLong }

            message.contains("required", ignoreCase = true) ->
                UiText.from { strings -> strings.staffNameRequired }

            else -> UiText.from { strings -> strings.errorValidation }
        }

        else -> toUiText()
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
    }
}
