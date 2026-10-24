package id.umkm.pos.pro.data.local.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.data.local.UmkmDatabase
import id.umkm.pos.pro.data.local.entity.ReturnEntity
import id.umkm.pos.pro.data.local.entity.SettingsEntity
import id.umkm.pos.pro.data.local.entity.StaffEntity
import id.umkm.pos.pro.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShiftRepositoryImplTest {

    private lateinit var database: UmkmDatabase
    private lateinit var repository: ShiftRepositoryImpl
    private val clock = MutableClock(START_MILLIS)

    @Before
    fun openInMemoryDatabase() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UmkmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.settingsDao().upsertSettings(defaultSettings())
        repository = ShiftRepositoryImpl(
            database = database,
            shiftDao = database.shiftDao(),
            staffDao = database.staffDao(),
            transactionDao = database.transactionDao(),
            returnDao = database.returnDao(),
            clock = clock,
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun openingTheFirstShiftSucceedsAndASecondActiveShiftIsRejected() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val budiId = seedStaff("Budi")

        val first = repository.openShift(staffId = sariId, startingCash = 100_000L)

        assertThat(first).isInstanceOf(AppResult.Success::class.java)
        val opened = first.successValue()
        assertThat(opened.id).isGreaterThan(0L)
        assertThat(opened.cashierName).isEqualTo("Sari")
        assertThat(opened.openedAt).isEqualTo(START_MILLIS)
        assertThat(opened.startingCash).isEqualTo(100_000L)
        assertThat(opened.closedAt).isNull()
        assertThat(opened.openedByStaffId).isEqualTo(sariId)
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(sariId)

        val second = repository.openShift(staffId = budiId, startingCash = 50_000L)

        assertThat(second).isInstanceOf(AppResult.Failure::class.java)
        assertThat((second as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(repository.getActiveShift()?.cashierName).isEqualTo("Sari")
        assertThat(repository.getActiveShift()?.id).isEqualTo(opened.id)
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(sariId)
    }

    @Test
    fun openingAShiftRejectsAnInactiveOrUnknownStaff() = runBlocking<Unit> {
        val inactiveId = seedStaff("Sari", isActive = false)

        val inactive = repository.openShift(staffId = inactiveId, startingCash = 10_000L)

        assertThat(inactive).isInstanceOf(AppResult.Failure::class.java)
        assertThat((inactive as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(repository.getActiveShift()).isNull()

        val unknown = repository.openShift(staffId = 999L, startingCash = 10_000L)

        assertThat(unknown).isInstanceOf(AppResult.Failure::class.java)
        assertThat((unknown as AppResult.Failure).error).isInstanceOf(AppError.NotFound::class.java)
    }

    @Test
    fun closingAShiftReconcilesCashSalesCashDebtPaymentsAndRefunds() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val opened = repository.openShift(staffId = sariId, startingCash = 50_000L).successValue()

        clock.currentMillis = START_MILLIS + 1_000L
        insertTransaction(
            invoiceNo = "INV-CASH",
            paymentMethod = PaymentMethod.CASH,
            paidAmount = 100_000L,
            changeAmount = 5_000L,
            shiftId = opened.id,
            staffId = sariId,
        )
        insertTransaction(
            invoiceNo = "INV-QRIS",
            paymentMethod = PaymentMethod.QRIS,
            paidAmount = 10_000L,
            shiftId = opened.id,
            staffId = sariId,
        )
        insertTransaction(
            invoiceNo = "INV-TRANSFER",
            paymentMethod = PaymentMethod.TRANSFER,
            paidAmount = 15_000L,
            shiftId = opened.id,
            staffId = sariId,
        )

        insertReturn(
            transactionId = 1L,
            createdAt = START_MILLIS + 3_000L,
            refundAmount = 3_000L,
        )

        clock.currentMillis = START_MILLIS + 5_000L
        val closed = repository.closeShift(
            shiftId = opened.id,
            actualEndingCash = 142_000L,
        ).successValue()

        assertThat(closed.id).isEqualTo(opened.id)
        assertThat(closed.closedAt).isEqualTo(START_MILLIS + 5_000L)
        assertThat(closed.expectedEndingCash).isEqualTo(142_000L)
        assertThat(closed.actualEndingCash).isEqualTo(142_000L)
        assertThat(closed.openedByStaffId).isEqualTo(sariId)
        assertThat(closed.closedByStaffId).isEqualTo(sariId)
        assertThat(closed.closedByName).isEqualTo("Sari")
        assertThat(database.staffDao().getActiveStaffId()).isNull()

        val storedSummary = repository.getShiftSummary(opened.id).successValue()
        assertThat(storedSummary.startingCash).isEqualTo(50_000L)
        assertThat(storedSummary.cashSales).isEqualTo(95_000L)
        assertThat(storedSummary.cashRefunds).isEqualTo(3_000L)
        assertThat(storedSummary.transactionCount).isEqualTo(3)
        assertThat(storedSummary.expectedEndingCash).isEqualTo(142_000L)

        val storedClosed = database.shiftDao().getShift(opened.id)
        assertThat(storedClosed?.closedByStaffId).isEqualTo(sariId)
        assertThat(storedClosed?.closedByName).isEqualTo("Sari")

        val linkedTransaction = database.transactionDao().getTransaction(1L)
        assertThat(linkedTransaction?.shiftId).isEqualTo(opened.id)
        assertThat(linkedTransaction?.staffId).isEqualTo(sariId)
    }

    @Test
    fun closingAnAlreadyClosedShiftFailsAndKeepsTheFirstCount() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val opened = repository.openShift(staffId = sariId, startingCash = 20_000L).successValue()
        clock.currentMillis = START_MILLIS + 1_000L
        repository.closeShift(shiftId = opened.id, actualEndingCash = 20_000L).successValue()

        clock.currentMillis = START_MILLIS + 2_000L
        val secondClose = repository.closeShift(shiftId = opened.id, actualEndingCash = 99_000L)

        assertThat(secondClose).isInstanceOf(AppResult.Failure::class.java)
        assertThat((secondClose as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        val stored = database.shiftDao().getShift(opened.id)
        assertThat(stored?.actualEndingCash).isEqualTo(20_000L)
        assertThat(stored?.closedAt).isEqualTo(START_MILLIS + 1_000L)
        assertThat(stored?.openedByStaffId).isEqualTo(sariId)
    }

    @Test
    fun switchingShiftsClosesTheOldShiftAndOpensTheNextOne() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val budiId = seedStaff("Budi")
        val oldShift = repository.openShift(staffId = sariId, startingCash = 10_000L).successValue()

        clock.currentMillis = START_MILLIS + 1_000L
        insertTransaction(
            invoiceNo = "INV-SWITCH",
            paymentMethod = PaymentMethod.CASH,
            paidAmount = 30_000L,
            shiftId = oldShift.id,
            staffId = sariId,
        )

        clock.currentMillis = START_MILLIS + 4_000L
        val switched = repository.switchShift(
            closeShiftId = oldShift.id,
            actualEndingCash = 40_000L,
            nextStaffId = budiId,
            nextStartingCash = 20_000L,
        ).successValue()

        assertThat(switched.closedShift.id).isEqualTo(oldShift.id)
        assertThat(switched.closedShift.closedAt).isEqualTo(START_MILLIS + 4_000L)
        assertThat(switched.closedShift.expectedEndingCash).isEqualTo(40_000L)
        assertThat(switched.closedShift.actualEndingCash).isEqualTo(40_000L)
        assertThat(switched.closedShift.openedByStaffId).isEqualTo(sariId)
        assertThat(switched.closedShift.closedByStaffId).isEqualTo(sariId)
        assertThat(switched.closedShift.closedByName).isEqualTo("Sari")

        assertThat(switched.openedShift.id).isNotEqualTo(oldShift.id)
        assertThat(switched.openedShift.cashierName).isEqualTo("Budi")
        assertThat(switched.openedShift.openedAt).isEqualTo(START_MILLIS + 4_000L)
        assertThat(switched.openedShift.startingCash).isEqualTo(20_000L)
        assertThat(switched.openedShift.closedAt).isNull()
        assertThat(switched.openedShift.openedByStaffId).isEqualTo(budiId)

        val active = repository.getActiveShift()
        assertThat(active?.id).isEqualTo(switched.openedShift.id)
        assertThat(active?.cashierName).isEqualTo("Budi")
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(budiId)

        val storedOldShift = database.shiftDao().getShift(oldShift.id)
        assertThat(storedOldShift?.expectedEndingCash).isEqualTo(40_000L)
        assertThat(storedOldShift?.actualEndingCash).isEqualTo(40_000L)
        assertThat(storedOldShift?.openedByStaffId).isEqualTo(sariId)
        assertThat(storedOldShift?.closedByStaffId).isEqualTo(sariId)
        assertThat(storedOldShift?.closedByName).isEqualTo("Sari")
        assertThat(repository.getShiftHistory(10).map { shift -> shift.cashierName })
            .containsExactly("Budi", "Sari")
            .inOrder()
    }

    @Test
    fun switchingShiftsRejectsAnInactiveNextStaffAndKeepsTheCurrentShiftOpen() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val inactiveId = seedStaff("Citra", isActive = false)
        val oldShift = repository.openShift(staffId = sariId, startingCash = 10_000L).successValue()

        val switched = repository.switchShift(
            closeShiftId = oldShift.id,
            actualEndingCash = 10_000L,
            nextStaffId = inactiveId,
            nextStartingCash = 20_000L,
        )

        assertThat(switched).isInstanceOf(AppResult.Failure::class.java)
        assertThat((switched as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(repository.getActiveShift()?.id).isEqualTo(oldShift.id)
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(sariId)
    }

    @Test
    fun switchingOperatorRequiresAnActiveShift() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val budiId = seedStaff("Budi")

        val switched = repository.switchOperator(budiId)

        assertThat(switched).isInstanceOf(AppResult.Failure::class.java)
        assertThat((switched as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(database.staffDao().getActiveStaffId()).isNull()
        assertThat(repository.getActiveShift()).isNull()
        assertThat(database.staffDao().getStaff(sariId)?.name).isEqualTo("Sari")
    }

    @Test
    fun switchingOperatorSetsTheActiveStaffWithoutReopeningTheShift() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val budiId = seedStaff("Budi")
        val opened = repository.openShift(staffId = sariId, startingCash = 10_000L).successValue()

        val switched = repository.switchOperator(budiId).successValue()

        assertThat(switched.id).isEqualTo(budiId)
        assertThat(switched.name).isEqualTo("Budi")
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(budiId)
        val activeShift = repository.getActiveShift()
        assertThat(activeShift?.id).isEqualTo(opened.id)
        assertThat(activeShift?.cashierName).isEqualTo("Sari")
        assertThat(activeShift?.openedByStaffId).isEqualTo(sariId)
    }

    @Test
    fun switchingOperatorRejectsAnInactiveStaff() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val inactiveId = seedStaff("Budi", isActive = false)
        repository.openShift(staffId = sariId, startingCash = 10_000L).successValue()

        val switched = repository.switchOperator(inactiveId)

        assertThat(switched).isInstanceOf(AppResult.Failure::class.java)
        assertThat((switched as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(database.staffDao().getActiveStaffId()).isEqualTo(sariId)
    }

    @Test
    fun historyAndRecentCashierNamesAreOrderedByNewestShift() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val budiId = seedStaff("Budi")
        val citraId = seedStaff("Citra")

        val first = repository.openShift(sariId, 10_000L).successValue()
        clock.currentMillis = START_MILLIS + 1_000L
        repository.closeShift(first.id, 10_000L).successValue()

        clock.currentMillis = START_MILLIS + 2_000L
        val second = repository.openShift(budiId, 20_000L).successValue()
        clock.currentMillis = START_MILLIS + 3_000L
        repository.closeShift(second.id, 20_000L).successValue()

        clock.currentMillis = START_MILLIS + 4_000L
        repository.openShift(citraId, 30_000L).successValue()

        assertThat(repository.getShiftHistory(10).map { shift -> shift.cashierName })
            .containsExactly("Citra", "Budi", "Sari")
            .inOrder()
        assertThat(repository.getRecentCashierNames(10)).containsExactly("Citra", "Budi", "Sari").inOrder()
        assertThat(repository.getRecentCashierNames(2)).containsExactly("Citra", "Budi").inOrder()
    }

    @Test
    fun recentCashierNamesUseTheLatestShiftForDuplicateNames() = runBlocking<Unit> {
        val sariId = seedStaff("Sari")
        val budiId = seedStaff("Budi")

        val first = repository.openShift(sariId, 10_000L).successValue()
        clock.currentMillis = START_MILLIS + 1_000L
        repository.closeShift(first.id, 10_000L).successValue()

        clock.currentMillis = START_MILLIS + 2_000L
        val second = repository.openShift(budiId, 20_000L).successValue()
        clock.currentMillis = START_MILLIS + 3_000L
        repository.closeShift(second.id, 20_000L).successValue()

        clock.currentMillis = START_MILLIS + 4_000L
        repository.openShift(sariId, 30_000L).successValue()

        assertThat(repository.getRecentCashierNames(10))
            .containsExactly("Sari", "Budi")
            .inOrder()
    }

    @Test
    fun noActiveShiftReadsAsNullOnBothQueries() = runBlocking<Unit> {
        assertThat(repository.getActiveShift()).isNull()
        assertThat(repository.observeActiveShift().first()).isNull()
    }

    private suspend fun seedStaff(name: String, isActive: Boolean = true): Long =
        database.staffDao().insertStaff(
            StaffEntity(
                name = name,
                isActive = isActive,
                createdAt = START_MILLIS,
                updatedAt = START_MILLIS,
            ),
        )

    private suspend fun insertTransaction(
        invoiceNo: String,
        paymentMethod: PaymentMethod,
        paidAmount: Long,
        changeAmount: Long = 0L,
        createdAt: Long = clock.nowMillis(),
        shiftId: Long? = null,
        staffId: Long? = null,
    ): Long = database.transactionDao().insertTransaction(
        TransactionEntity(
            invoiceNo = invoiceNo,
            createdAt = createdAt,
            cashierId = null,
            subtotal = paidAmount,
            discountTotal = 0L,
            invoiceDiscount = 0L,
            taxTotal = 0L,
            rounding = 0L,
            grandTotal = paidAmount,
            totalHpp = 0L,
            netProfit = paidAmount,
            paymentMethod = paymentMethod,
            paidAmount = paidAmount,
            changeAmount = changeAmount,
            status = TransactionStatus.COMPLETED,
            notes = null,
            cashierName = "Sari",
            shiftId = shiftId,
            staffId = staffId,
        ),
    )

    private suspend fun insertReturn(
        transactionId: Long,
        createdAt: Long,
        refundAmount: Long,
    ) {
        database.returnDao().insertReturn(
            ReturnEntity(
                transactionId = transactionId,
                createdAt = createdAt,
                refundAmount = refundAmount,
                totalHppReversed = 0L,
                note = null,
            ),
        )
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
