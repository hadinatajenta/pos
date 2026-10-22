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
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val editor: StaffEditorState? = null,
    val pendingDeactivation: Staff? = null,
    val isSubmitting: Boolean = false,
    val message: UiText? = null,
    val errorMessage: UiText? = null,
) {
    val filteredStaffList: List<Staff>
        get() {
            val query = searchQuery.trim()
            return if (query.isEmpty()) {
                staffList
            } else {
                staffList.filter { staff -> staff.name.contains(query, ignoreCase = true) }
            }
        }

    val activeStaffList: List<Staff> get() = filteredStaffList.filter(Staff::isActive)

    val inactiveStaffList: List<Staff> get() = filteredStaffList.filterNot(Staff::isActive)

    val hasStaff: Boolean get() = staffList.isNotEmpty()

    val hasVisibleStaff: Boolean get() = filteredStaffList.isNotEmpty()

    val isSearching: Boolean get() = searchQuery.isNotBlank()
}

internal fun sortStaff(staffList: List<Staff>): List<Staff> =
    staffList.sortedWith(
        compareByDescending<Staff> { staff -> staff.isActive }
            .thenBy { staff -> staff.name.lowercase() },
    )
