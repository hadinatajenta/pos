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
import id.umkm.pos.pro.data.local.entity.DebtPaymentEntity
import id.umkm.pos.pro.data.local.entity.ReturnEntity
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
    fun openInMemoryDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UmkmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ShiftRepositoryImpl(
            database = database,
            shiftDao = database.shiftDao(),
            transactionDao = database.transactionDao(),
            debtPaymentDao = database.debtPaymentDao(),
            returnDao = database.returnDao(),
            clock = clock,
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun openingTheFirstShiftSucceedsAndASecondActiveShiftIsRejected() = runBlocking {
        val first = repository.openShift(cashierName = "Sari", startingCash = 100_000L)

        assertThat(first).isInstanceOf(AppResult.Success::class.java)
        val opened = (first as AppResult.Success<CashierShift>).value
        assertThat(opened.id).isGreaterThan(0L)
        assertThat(opened.cashierName).isEqualTo("Sari")
        assertThat(opened.openedAt).isEqualTo(START_MILLIS)
        assertThat(opened.startingCash).isEqualTo(100_000L)
        assertThat(opened.closedAt).isNull()

        val second = repository.openShift(cashierName = "Budi", startingCash = 50_000L)

        assertThat(second).isInstanceOf(AppResult.Failure::class.java)
        assertThat((second as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(repository.getActiveShift()?.cashierName).isEqualTo("Sari")
        assertThat(repository.getActiveShift()?.id).isEqualTo(opened.id)
    }

    @Test
    fun closingAShiftReconcilesCashSalesCashDebtPaymentsAndRefunds() = runBlocking {
        val opened = repository.openShift(
            cashierName = "Sari",
            startingCash = 50_000L,
        ).successValue()

        clock.currentMillis = START_MILLIS + 1_000L
        insertTransaction(
            invoiceNo = "INV-CASH",
            paymentMethod = PaymentMethod.CASH,
            paidAmount = 100_000L,
            changeAmount = 5_000L,
        )
        insertTransaction(
            invoiceNo = "INV-SPLIT",
            paymentMethod = PaymentMethod.SPLIT,
            paidAmount = 40_000L,
        )
        insertTransaction(
            invoiceNo = "INV-KASBON",
            paymentMethod = PaymentMethod.KASBON,
            paidAmount = 25_000L,
        )
        insertTransaction(
            invoiceNo = "INV-QRIS",
            paymentMethod = PaymentMethod.QRIS,
            paidAmount = 10_000L,
        )
        insertTransaction(
            invoiceNo = "INV-TRANSFER",
            paymentMethod = PaymentMethod.TRANSFER,
            paidAmount = 15_000L,
        )

        val cashDebtTransaction = insertTransaction(
            invoiceNo = "INV-DEBT-CASH",
            paymentMethod = PaymentMethod.KASBON,
            paidAmount = 12_000L,
            createdAt = START_MILLIS - 10_000L,
        )
        val qrisDebtTransaction = insertTransaction(
            invoiceNo = "INV-DEBT-QRIS",
            paymentMethod = PaymentMethod.KASBON,
            paidAmount = 8_000L,
            createdAt = START_MILLIS - 10_000L,
        )
        insertDebtPayment(
            transactionId = cashDebtTransaction,
            paidAt = START_MILLIS + 2_000L,
            amount = 7_000L,
            paymentMethod = PaymentMethod.CASH,
        )
        insertDebtPayment(
            transactionId = qrisDebtTransaction,
            paidAt = START_MILLIS + 2_000L,
            amount = 9_000L,
            paymentMethod = PaymentMethod.QRIS,
        )
        insertReturn(
            transactionId = cashDebtTransaction,
            createdAt = START_MILLIS + 3_000L,
            refundAmount = 3_000L,
        )

        clock.currentMillis = START_MILLIS + 5_000L
        val closed = repository.closeShift(
            shiftId = opened.id,
            actualEndingCash = 214_000L,
        ).successValue()

        assertThat(closed.id).isEqualTo(opened.id)
        assertThat(closed.closedAt).isEqualTo(START_MILLIS + 5_000L)
        assertThat(closed.expectedEndingCash).isEqualTo(214_000L)
        assertThat(closed.actualEndingCash).isEqualTo(214_000L)

        val storedSummary = repository.getShiftSummary(opened.id).successValue()
        assertThat(storedSummary.startingCash).isEqualTo(50_000L)
        assertThat(storedSummary.cashSales).isEqualTo(160_000L)
        assertThat(storedSummary.cashDebtPayments).isEqualTo(7_000L)
        assertThat(storedSummary.cashRefunds).isEqualTo(3_000L)
        assertThat(storedSummary.transactionCount).isEqualTo(5)
        assertThat(storedSummary.expectedEndingCash).isEqualTo(214_000L)
    }

    @Test
    fun closingAnAlreadyClosedShiftFailsAndKeepsTheFirstCount() = runBlocking {
        val opened = repository.openShift(
            cashierName = "Sari",
            startingCash = 20_000L,
        ).successValue()
        clock.currentMillis = START_MILLIS + 1_000L
        repository.closeShift(shiftId = opened.id, actualEndingCash = 20_000L).successValue()

        clock.currentMillis = START_MILLIS + 2_000L
        val secondClose = repository.closeShift(shiftId = opened.id, actualEndingCash = 99_000L)

        assertThat(secondClose).isInstanceOf(AppResult.Failure::class.java)
        assertThat((secondClose as AppResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        val stored = database.shiftDao().getShift(opened.id)
        assertThat(stored?.actualEndingCash).isEqualTo(20_000L)
        assertThat(stored?.closedAt).isEqualTo(START_MILLIS + 1_000L)
    }

    @Test
    fun switchingShiftsClosesTheOldShiftAndOpensTheNextOne() = runBlocking {
        val oldShift = repository.openShift(
            cashierName = "Sari",
            startingCash = 10_000L,
        ).successValue()

        clock.currentMillis = START_MILLIS + 1_000L
        insertTransaction(
            invoiceNo = "INV-SWITCH",
            paymentMethod = PaymentMethod.CASH,
            paidAmount = 30_000L,
        )

        clock.currentMillis = START_MILLIS + 4_000L
        val switched = repository.switchShift(
            closeShiftId = oldShift.id,
            actualEndingCash = 40_000L,
            nextCashierName = "Budi",
            nextStartingCash = 20_000L,
        ).successValue()

        assertThat(switched.closedShift.id).isEqualTo(oldShift.id)
        assertThat(switched.closedShift.closedAt).isEqualTo(START_MILLIS + 4_000L)
        assertThat(switched.closedShift.expectedEndingCash).isEqualTo(40_000L)
        assertThat(switched.closedShift.actualEndingCash).isEqualTo(40_000L)

        assertThat(switched.openedShift.id).isNotEqualTo(oldShift.id)
        assertThat(switched.openedShift.cashierName).isEqualTo("Budi")
        assertThat(switched.openedShift.openedAt).isEqualTo(START_MILLIS + 4_000L)
        assertThat(switched.openedShift.startingCash).isEqualTo(20_000L)
        assertThat(switched.openedShift.closedAt).isNull()

        val active = repository.getActiveShift()
        assertThat(active?.id).isEqualTo(switched.openedShift.id)
        assertThat(active?.cashierName).isEqualTo("Budi")

        val storedOldShift = database.shiftDao().getShift(oldShift.id)
        assertThat(storedOldShift?.expectedEndingCash).isEqualTo(40_000L)
        assertThat(storedOldShift?.actualEndingCash).isEqualTo(40_000L)
        assertThat(repository.getShiftHistory(10).map { shift -> shift.cashierName })
            .containsExactly("Budi", "Sari")
            .inOrder()
    }

    @Test
    fun historyAndRecentCashierNamesAreOrderedByNewestShift() = runBlocking {
        val first = repository.openShift("Sari", 10_000L).successValue()
        clock.currentMillis = START_MILLIS + 1_000L
        repository.closeShift(first.id, 10_000L).successValue()

        clock.currentMillis = START_MILLIS + 2_000L
        val second = repository.openShift("Budi", 20_000L).successValue()
        clock.currentMillis = START_MILLIS + 3_000L
        repository.closeShift(second.id, 20_000L).successValue()

        clock.currentMillis = START_MILLIS + 4_000L
        repository.openShift("Citra", 30_000L).successValue()

        assertThat(repository.getShiftHistory(10).map { shift -> shift.cashierName })
            .containsExactly("Citra", "Budi", "Sari")
            .inOrder()
        assertThat(repository.getRecentCashierNames(10)).containsExactly("Citra", "Budi", "Sari").inOrder()
        assertThat(repository.getRecentCashierNames(2)).containsExactly("Citra", "Budi").inOrder()
    }

    @Test
    fun recentCashierNamesUseTheLatestShiftForDuplicateNames() = runBlocking {
        val first = repository.openShift("Sari", 10_000L).successValue()
        clock.currentMillis = START_MILLIS + 1_000L
        repository.closeShift(first.id, 10_000L).successValue()

        clock.currentMillis = START_MILLIS + 2_000L
        val second = repository.openShift("Budi", 20_000L).successValue()
        clock.currentMillis = START_MILLIS + 3_000L
        repository.closeShift(second.id, 20_000L).successValue()

        clock.currentMillis = START_MILLIS + 4_000L
        repository.openShift("Sari", 30_000L).successValue()

        assertThat(repository.getRecentCashierNames(10))
            .containsExactly("Sari", "Budi")
            .inOrder()
    }

    @Test
    fun noActiveShiftReadsAsNullOnBothQueries() = runBlocking {
        assertThat(repository.getActiveShift()).isNull()
        assertThat(repository.observeActiveShift().first()).isNull()
    }

    private suspend fun insertTransaction(
        invoiceNo: String,
        paymentMethod: PaymentMethod,
        paidAmount: Long,
        changeAmount: Long = 0L,
        createdAt: Long = clock.nowMillis(),
    ): Long = database.transactionDao().insertTransaction(
        TransactionEntity(
            invoiceNo = invoiceNo,
            createdAt = createdAt,
            cashierId = null,
            customerId = null,
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
            remainingDebt = 0L,
            dueDate = null,
            status = TransactionStatus.COMPLETED,
            notes = null,
            cashierName = "Sari",
        ),
    )

    private suspend fun insertDebtPayment(
        transactionId: Long,
        paidAt: Long,
        amount: Long,
        paymentMethod: PaymentMethod,
    ) {
        database.debtPaymentDao().insertDebtPayment(
            DebtPaymentEntity(
                transactionId = transactionId,
                customerId = 1L,
                paidAt = paidAt,
                amount = amount,
                paymentMethod = paymentMethod,
                note = null,
            ),
        )
    }

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

    private fun <T> AppResult<T>.successValue(): T =
        (this as AppResult.Success<T>).value

    private class MutableClock(var currentMillis: Long) : Clock {
        override fun nowMillis(): Long = currentMillis
    }

    private companion object {
        const val START_MILLIS = 1_700_000_000_000L
    }
}
