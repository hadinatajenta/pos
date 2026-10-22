package id.umkm.pos.pro.ui.staff

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.StaffRepository
import id.umkm.pos.pro.core.model.Staff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal data class ActiveChange(
    val staffId: Long,
    val isActive: Boolean,
)

internal data class RenameCall(
    val staffId: Long,
    val name: String,
)

internal class FakeStaffRepository(
    initialStaff: List<Staff>,
) : StaffRepository {

    private val staffState = MutableStateFlow(initialStaff)
    private val activeStaffId = MutableStateFlow(
        initialStaff.firstOrNull(Staff::isActive)?.id,
    )

    var setActiveFailure: AppError? = null

    val addedNames = mutableListOf<String>()
    val renamedStaff = mutableListOf<RenameCall>()
    val activeChanges = mutableListOf<ActiveChange>()

    val staffList: List<Staff> get() = staffState.value

    override fun observeStaff(): Flow<List<Staff>> = staffState

    override fun observeActiveStaff(): Flow<Staff?> =
        combine(staffState, activeStaffId) { staffList, activeId ->
            staffList.firstOrNull { staff -> staff.id == activeId }
        }

    override suspend fun getStaff(staffId: Long): Staff? =
        staffState.value.firstOrNull { staff -> staff.id == staffId }

    override suspend fun getActiveStaff(): Staff? =
        staffState.value.firstOrNull { staff -> staff.id == activeStaffId.value }

    override suspend fun addStaff(name: String): AppResult<Staff> {
        addedNames += name
        if (staffState.value.any { staff -> staff.name.equals(name, ignoreCase = true) }) {
            return AppResult.Failure(AppError.Validation("staff name already exists"))
        }
        val staff = Staff(
            id = (staffState.value.maxOfOrNull(Staff::id) ?: 0L) + 1L,
            name = name,
            isActive = true,
        )
        staffState.value = staffState.value + staff
        if (activeStaffId.value == null) {
            activeStaffId.value = staff.id
        }
        return AppResult.Success(staff)
    }

    override suspend fun renameStaff(staffId: Long, name: String): AppResult<Staff> {
        renamedStaff += RenameCall(staffId = staffId, name = name)
        val existing = staffState.value.firstOrNull { staff -> staff.id == staffId }
            ?: return AppResult.Failure(AppError.NotFound("staff $staffId was not found"))
        val duplicate = staffState.value.firstOrNull { staff ->
            staff.id != staffId && staff.name.equals(name, ignoreCase = true)
        }
        if (duplicate != null) {
            return AppResult.Failure(AppError.Validation("staff name already exists"))
        }
        val updated = existing.copy(name = name)
        staffState.value = staffState.value.map { staff ->
            if (staff.id == staffId) updated else staff
        }
        return AppResult.Success(updated)
    }

    override suspend fun setStaffActive(
        staffId: Long,
        isActive: Boolean,
    ): AppResult<Staff> {
        setActiveFailure?.let { failure -> return AppResult.Failure(failure) }
        val existing = staffState.value.firstOrNull { staff -> staff.id == staffId }
            ?: return AppResult.Failure(AppError.NotFound("staff $staffId was not found"))
        if (!isActive && existing.isActive && staffState.value.count(Staff::isActive) <= 1) {
            return AppResult.Failure(AppError.Validation("at least one active staff is required"))
        }
        activeChanges += ActiveChange(staffId = staffId, isActive = isActive)
        val updated = existing.copy(isActive = isActive)
        staffState.value = staffState.value.map { staff ->
            if (staff.id == staffId) updated else staff
        }
        if (!isActive && activeStaffId.value == staffId) {
            activeStaffId.value = staffState.value
                .firstOrNull { staff -> staff.isActive && staff.id != staffId }
                ?.id
        }
        return AppResult.Success(updated)
    }

    override suspend fun ensureDefaultStaff(): Staff =
        staffState.value.firstOrNull(Staff::isActive)
            ?: error("no active staff available")
}
