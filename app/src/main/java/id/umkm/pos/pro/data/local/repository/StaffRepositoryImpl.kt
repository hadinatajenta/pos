package id.umkm.pos.pro.data.local.repository

import androidx.room.withTransaction
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.StaffRepository
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.dao.StaffDao
import id.umkm.pos.pro.data.local.entity.StaffEntity
import id.umkm.pos.pro.data.local.mapper.StaffMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class StaffRepositoryImpl(
    private val database: UmkmDatabase,
    private val staffDao: StaffDao,
    private val clock: Clock,
) : StaffRepository {

    override fun observeStaff(): Flow<List<Staff>> =
        staffDao.observeStaff().map { entities -> entities.map(StaffMapper::toModel) }

    override fun observeActiveStaff(): Flow<Staff?> =
        combine(
            staffDao.observeActiveStaffRows(),
            staffDao.observeActiveStaffId(),
        ) { activeStaff, activeStaffId ->
            activeStaff.firstOrNull { staff -> staff.id == activeStaffId }
                ?.let(StaffMapper::toModel)
        }

    override suspend fun getStaff(staffId: Long): Staff? =
        staffDao.getStaff(staffId)?.let(StaffMapper::toModel)

    override suspend fun getActiveStaff(): Staff? {
        val activeStaffId = staffDao.getActiveStaffId() ?: return null
        return staffDao.getStaff(activeStaffId)?.let(StaffMapper::toModel)
    }

    override suspend fun addStaff(name: String): AppResult<Staff> {
        val trimmedName = name.trim()
        validateName(trimmedName)?.let { return AppResult.Failure(it) }
        return guarded {
            database.withTransaction {
                if (staffDao.findStaffByName(trimmedName) != null) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("staff name already exists"),
                    )
                }
                val now = clock.nowMillis()
                val entity = StaffEntity(
                    name = trimmedName,
                    role = SecurityRole.CASHIER,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                )
                val id = staffDao.insertStaff(entity)
                if (staffDao.countActiveStaff() == 1 || staffDao.getActiveStaffId() == null) {
                    staffDao.setActiveStaffId(id)
                }
                AppResult.Success(StaffMapper.toModel(entity.copy(id = id)))
            }
        }
    }

    override suspend fun renameStaff(staffId: Long, name: String): AppResult<Staff> {
        val trimmedName = name.trim()
        validateName(trimmedName)?.let { return AppResult.Failure(it) }
        return guarded {
            database.withTransaction {
                val existing = staffDao.getStaff(staffId)
                    ?: return@withTransaction AppResult.Failure(
                        AppError.NotFound("staff $staffId was not found"),
                    )
                val duplicate = staffDao.findStaffByName(trimmedName)
                if (duplicate != null && duplicate.id != staffId) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("staff name already exists"),
                    )
                }
                val updated = existing.copy(
                    name = trimmedName,
                    updatedAt = clock.nowMillis(),
                )
                staffDao.updateStaff(updated)
                AppResult.Success(StaffMapper.toModel(updated))
            }
        }
    }

    override suspend fun setStaffActive(staffId: Long, isActive: Boolean): AppResult<Staff> =
        guarded {
            database.withTransaction {
                val existing = staffDao.getStaff(staffId)
                    ?: return@withTransaction AppResult.Failure(
                        AppError.NotFound("staff $staffId was not found"),
                    )
                if (!isActive && existing.isActive && staffDao.countActiveStaff() <= 1) {
                    return@withTransaction AppResult.Failure(
                        AppError.Validation("at least one active staff is required"),
                    )
                }
                val updated = existing.copy(
                    isActive = isActive,
                    updatedAt = clock.nowMillis(),
                )
                staffDao.updateStaff(updated)
                if (!isActive && staffDao.getActiveStaffId() == staffId) {
                    staffDao.setActiveStaffId(staffDao.getFirstActiveStaff(staffId)?.id)
                }
                AppResult.Success(StaffMapper.toModel(updated))
            }
        }

    override suspend fun ensureDefaultStaff(): Staff = guarded {
        database.withTransaction {
            if (staffDao.countActiveStaff() > 0) {
                val activeStaffId = staffDao.getActiveStaffId()
                val current = activeStaffId?.let { staffDao.getStaff(it) }
                if (current != null && current.isActive) {
                    return@withTransaction AppResult.Success(StaffMapper.toModel(current))
                }
                val firstActive = staffDao.getFirstActiveStaff(0L)
                    ?: return@withTransaction AppResult.Failure(
                        AppError.Validation("no active staff available"),
                    )
                staffDao.setActiveStaffId(firstActive.id)
                return@withTransaction AppResult.Success(StaffMapper.toModel(firstActive))
            }
            val now = clock.nowMillis()
            val existing = staffDao.findStaffByName(DEFAULT_STAFF_NAME)
            val entity = if (existing != null) {
                existing.copy(isActive = true, updatedAt = now)
            } else {
                StaffEntity(
                    name = DEFAULT_STAFF_NAME,
                    role = SecurityRole.CASHIER,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            val id = if (existing != null) {
                staffDao.updateStaff(entity)
                entity.id
            } else {
                staffDao.insertStaff(entity)
            }
            staffDao.setActiveStaffId(id)
            AppResult.Success(StaffMapper.toModel(entity.copy(id = id)))
        }
    }.successOrThrow()

    private fun validateName(name: String): AppError? = when {
        name.isEmpty() -> AppError.Validation("staff name is required")
        name.length > MAX_NAME_LENGTH -> AppError.Validation("staff name is too long")
        else -> null
    }

    private suspend fun <T> guarded(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppResult.Failure(AppError.Storage(error.message ?: "staff operation failed"))
        }

    private fun <T> AppResult<T>.successOrThrow(): T = when (this) {
        is AppResult.Success -> value
        is AppResult.Failure -> error("default staff initialization failed: ${error.message}")
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
        const val DEFAULT_STAFF_NAME = "Kasir Utama"
    }
}
