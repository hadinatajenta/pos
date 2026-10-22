package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import id.umkm.pos.pro.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {

    @Insert
    suspend fun insertStaff(staff: StaffEntity): Long

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("SELECT * FROM staffs ORDER BY is_active DESC, name COLLATE NOCASE ASC")
    fun observeStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staffs WHERE is_active = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActiveStaffRows(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staffs WHERE id = :staffId")
    suspend fun getStaff(staffId: Long): StaffEntity?

    @Query("SELECT * FROM staffs WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findStaffByName(name: String): StaffEntity?

    @Query("SELECT COUNT(*) FROM staffs WHERE is_active = 1")
    suspend fun countActiveStaff(): Int

    @Query("SELECT * FROM staffs WHERE is_active = 1 AND id != :excludedId ORDER BY name COLLATE NOCASE ASC LIMIT 1")
    suspend fun getFirstActiveStaff(excludedId: Long): StaffEntity?

    @Query("SELECT active_staff_id FROM app_settings WHERE id = 1")
    suspend fun getActiveStaffId(): Long?

    @Query("SELECT active_staff_id FROM app_settings WHERE id = 1")
    fun observeActiveStaffId(): Flow<Long?>

    @Query("UPDATE app_settings SET active_staff_id = :staffId WHERE id = 1")
    suspend fun setActiveStaffId(staffId: Long?)
}
