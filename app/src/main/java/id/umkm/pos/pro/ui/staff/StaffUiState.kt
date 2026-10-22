package id.umkm.pos.pro.ui.staff

import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.UiText

data class StaffEditorState(
    val staffId: Long? = null,
    val name: String = "",
    val error: UiText? = null,
) {
    val isEditing: Boolean get() = staffId != null
}

data class StaffUiState(
    val staffList: List<Staff> = emptyList(),
    val activeStaffId: Long? = null,
    val isLoading: Boolean = true,
    val editor: StaffEditorState? = null,
    val pendingDeactivation: Staff? = null,
    val isSubmitting: Boolean = false,
    val message: UiText? = null,
    val errorMessage: UiText? = null,
) {
    val activeStaffList: List<Staff> get() = staffList.filter(Staff::isActive)

    val inactiveStaffList: List<Staff> get() = staffList.filterNot(Staff::isActive)

    val hasStaff: Boolean get() = staffList.isNotEmpty()
}

internal fun sortStaff(staffList: List<Staff>): List<Staff> =
    staffList.sortedWith(
        compareByDescending<Staff> { staff -> staff.isActive }
            .thenBy { staff -> staff.name.lowercase() },
    )
