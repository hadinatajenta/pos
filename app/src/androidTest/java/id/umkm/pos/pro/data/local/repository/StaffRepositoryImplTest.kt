package id.umkm.pos.pro.data.local.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaffRepositoryImplTest {

    private lateinit var database: UmkmDatabase
    private lateinit var repository: StaffRepositoryImpl
    private val clock = MutableClock(START_MILLIS)

    @Before
    fun openInMemoryDatabase() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UmkmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.settingsDao().upsertSettings(defaultSettings())
        repository = StaffRepositoryImpl(
            database = database,
            staffDao = database.staffDao(),
            clock = clock,
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun theFirstAddedStaffBecomesActiveAndLaterAdditionsKeepThatSelection() = runBlocking<Unit> {
        val sari = repository.addStaff("  Sari  ").successValue()
        val budi = repository.addStaff("Budi").successValue()

        assertThat(sari.name).isEqualTo("Sari")
        assertThat(sari.role).isEqualTo(SecurityRole.CASHIER)
        assertThat(sari.isActive).isTrue()
        assertThat(budi.isActive).isTrue()
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(sari.id)
        assertThat(repository.getActiveStaff()?.id).isEqualTo(sari.id)
        assertThat(repository.observeStaff().first().map(Staff::name))
            .containsExactly("Budi", "Sari")
    }

    @Test
    fun duplicateStaffNamesAreRejectedIgnoringCase() = runBlocking<Unit> {
        repository.addStaff("Sari").successValue()

        val duplicateAdd = repository.addStaff("sari")
        val upperCaseAdd = repository.addStaff("SARI")

        assertThat(duplicateAdd).isInstanceOf(AppResult.Failure::class.java)
        assertThat((duplicateAdd as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(upperCaseAdd).isInstanceOf(AppResult.Failure::class.java)
        assertThat(repository.observeStaff().first()).hasSize(1)
    }

    @Test
    fun renamingRejectsAnotherStaffsNameButAllowsChangingTheCurrentStaffsCase() = runBlocking<Unit> {
        val sari = repository.addStaff("Sari").successValue()
        val budi = repository.addStaff("Budi").successValue()

        val duplicateRename = repository.renameStaff(budi.id, "sari")
        val caseOnlyRename = repository.renameStaff(sari.id, "SARI")

        assertThat(duplicateRename).isInstanceOf(AppResult.Failure::class.java)
        assertThat((duplicateRename as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(caseOnlyRename.successValue().name).isEqualTo("SARI")
        assertThat(repository.getStaff(budi.id)?.name).isEqualTo("Budi")
        assertThat(repository.getStaff(sari.id)?.name).isEqualTo("SARI")
    }

    @Test
    fun renamingUpdatesTheStaffAndRejectsAnUnknownStaff() = runBlocking<Unit> {
        val sari = repository.addStaff("Sari").successValue()

        val renamed = repository.renameStaff(sari.id, " Sari Pagi ").successValue()
        val unknown = repository.renameStaff(999L, "Budi")

        assertThat(renamed.id).isEqualTo(sari.id)
        assertThat(renamed.name).isEqualTo("Sari Pagi")
        assertThat(repository.getStaff(sari.id)?.name).isEqualTo("Sari Pagi")
        assertThat(unknown).isInstanceOf(AppResult.Failure::class.java)
        assertThat((unknown as AppResult.Failure).error).isInstanceOf(AppError.NotFound::class.java)
    }

    @Test
    fun deactivatingTheLastActiveStaffIsRejected() = runBlocking<Unit> {
        val sari = repository.addStaff("Sari").successValue()
        val budi = repository.addStaff("Budi").successValue()
        repository.setStaffActive(budi.id, false).successValue()

        val deactivation = repository.setStaffActive(sari.id, false)

        assertThat(deactivation).isInstanceOf(AppResult.Failure::class.java)
        assertThat((deactivation as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(repository.getStaff(sari.id)?.isActive).isTrue()
        assertThat(repository.getActiveStaff()?.id).isEqualTo(sari.id)
    }

    @Test
    fun deactivatingTheActiveStaffMovesTheActiveSelectionToAnotherActiveStaff() = runBlocking<Unit> {
        val sari = repository.addStaff("Sari").successValue()
        val budi = repository.addStaff("Budi").successValue()

        repository.setStaffActive(sari.id, false).successValue()

        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(budi.id)
        assertThat(repository.getActiveStaff()?.id).isEqualTo(budi.id)
        assertThat(repository.observeActiveStaff().first()?.id).isEqualTo(budi.id)
        assertThat(repository.getStaff(sari.id)?.isActive).isFalse()
    }

    @Test
    fun reactivatingAStaffDoesNotStealTheCurrentActiveSelection() = runBlocking<Unit> {
        val sari = repository.addStaff("Sari").successValue()
        val budi = repository.addStaff("Budi").successValue()
        repository.setStaffActive(sari.id, false).successValue()
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(budi.id)

        repository.setStaffActive(sari.id, true).successValue()

        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(budi.id)
        assertThat(repository.getStaff(sari.id)?.isActive).isTrue()
    }

    @Test
    fun ensureDefaultStaffSeedsOneActiveStaffAndIsIdempotent() = runBlocking<Unit> {
        val seeded = repository.ensureDefaultStaff()
        val repeated = repository.ensureDefaultStaff()

        assertThat(seeded.id).isEqualTo(repeated.id)
        assertThat(seeded.name).isEqualTo("Kasir Utama")
        assertThat(seeded.isActive).isTrue()
        assertThat(repository.observeStaff().first()).hasSize(1)
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(seeded.id)
        assertThat(repository.observeActiveStaff().first()?.id).isEqualTo(seeded.id)
    }

    @Test
    fun ensureDefaultStaffKeepsAnExistingActiveSelection() = runBlocking<Unit> {
        val budi = repository.addStaff("Budi").successValue()

        val ensured = repository.ensureDefaultStaff()

        assertThat(ensured.id).isEqualTo(budi.id)
        assertThat(repository.observeStaff().first()).hasSize(1)
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(budi.id)
    }

    @Test
    fun observeActiveStaffFollowsTheActiveSelection() = runBlocking<Unit> {
        val sari = repository.addStaff("Sari").successValue()
        val budi = repository.addStaff("Budi").successValue()

        assertThat(repository.observeActiveStaff().first()?.id).isEqualTo(sari.id)

        database.staffDao().setActiveStaffId(budi.id)

        val active = repository.observeActiveStaff().first { staff -> staff?.id == budi.id }
        assertThat(active?.name).isEqualTo("Budi")
    }

    @Test
    fun blankAndOverlongNamesAreRejected() = runBlocking<Unit> {
        val blank = repository.addStaff("   ")
        val overlong = repository.addStaff("A".repeat(61))

        assertThat(blank).isInstanceOf(AppResult.Failure::class.java)
        assertThat((blank as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(overlong).isInstanceOf(AppResult.Failure::class.java)
        assertThat(repository.observeStaff().first()).isEmpty()
    }

    private fun defaultSettings(): SettingsEntity = SettingsEntity(
        storeName = "Toko Uji",
        cashierName = null,
        taxRatePercent = 0,
        roundingIncrement = 0L,
        isPinEnabled = false,
        printerAddress = null,
        qrisImagePath = null,
        qrisPayload = null,
    )

    private fun <T> AppResult<T>.successValue(): T =
        (this as AppResult.Success<T>).value

    private class MutableClock(var currentMillis: Long) : Clock {
        override fun nowMillis(): Long = currentMillis
    }

    private companion object {
        const val START_MILLIS = 1_700_000_000_000L
    }
}
