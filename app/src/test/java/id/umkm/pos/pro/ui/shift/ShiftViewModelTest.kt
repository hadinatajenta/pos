package id.umkm.pos.pro.ui.shift

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.ShiftSwitchResult
import id.umkm.pos.pro.ui.FakeSettingsRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.IdStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ShiftViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val activeShift = CashierShift(
        id = 1L,
        cashierName = "Dini",
        openedAt = 1_000L,
        closedAt = null,
        startingCash = 100_000L,
        expectedEndingCash = null,
        actualEndingCash = null,
    )

    private val summary = ShiftCashSummary(
        startingCash = 100_000L,
        cashSales = 40_000L,
        cashDebtPayments = 10_000L,
        cashRefunds = 0L,
        transactionCount = 3,
    )

    private fun createViewModel(
        repository: FakeShiftRepository = FakeShiftRepository(activeShift, summary),
    ): ShiftViewModel = ShiftViewModel(
        shiftRepository = repository,
        settingsRepository = FakeSettingsRepository(
            initialSettings = AppSettings(
                storeName = "Toko Uji",
                cashierName = "Dini",
                taxRatePercent = 0,
                roundingIncrement = 0L,
                isPinEnabled = false,
                printerAddress = null,
                qrisImagePath = null,
            ),
        ),
    )

    @Test
    fun openingAFormWithoutAnActiveShiftUsesTheDefaultCashierName() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(repository)

        viewModel.onOpenShiftForm()

        val state = viewModel.state.value
        assertThat(state.mode).isEqualTo(ShiftSheetMode.SHIFT_FORM)
        assertThat(state.activeShift).isNull()
        assertThat(state.cashierNameInput).isEqualTo("Dini")
    }

    @Test
    fun openingAShiftRequiresACashierName() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()

        viewModel.onCashierNameChange(" ")
        viewModel.onStartingCashChange("50000")
        viewModel.onOpenShift()

        assertThat(viewModel.state.value.validationMessage?.resolve(IdStrings))
            .isEqualTo(IdStrings.shiftValidationCashierName)
        assertThat(repository.openedCashierNames).isEmpty()
    }

    @Test
    fun openingAShiftRequiresAStartingCashAmount() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()
        viewModel.onCashierNameChange("Budi")

        viewModel.onOpenShift()

        assertThat(viewModel.state.value.validationMessage?.resolve(IdStrings))
            .isEqualTo(IdStrings.shiftValidationStartingCash)
        assertThat(repository.openedCashierNames).isEmpty()
    }

    @Test
    fun openingAShiftRecordsTheCashierAndStartingCash() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()
        viewModel.onCashierNameChange("Budi")
        viewModel.onStartingCashChange("75000")

        viewModel.onOpenShift()

        assertThat(repository.openedCashierNames).containsExactly("Budi")
        assertThat(repository.openedStartingCash).containsExactly(75_000L)
        val state = viewModel.state.value
        assertThat(state.mode).isEqualTo(ShiftSheetMode.HIDDEN)
        assertThat(state.activeShift?.cashierName).isEqualTo("Budi")
        assertThat(state.message?.resolve(IdStrings)).isEqualTo("Shift Budi dibuka.")
    }

    @Test
    fun closingAShiftRequiresVarianceConfirmation() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(activeShift, summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()
        viewModel.onActualEndingCashChange("145000")

        viewModel.onCloseShift()

        assertThat(viewModel.state.value.validationMessage?.resolve(IdStrings))
            .isEqualTo(IdStrings.shiftValidationVarianceAck)
        assertThat(repository.closedActualCash).isEmpty()
    }

    @Test
    fun closingAShiftRecordsTheActualCashAfterVarianceConfirmation() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeShiftRepository(activeShift, summary)
            val viewModel = createViewModel(repository)
            viewModel.onOpenShiftForm()
            viewModel.onActualEndingCashChange("145000")
            viewModel.onVarianceAcknowledgedChange(true)

            viewModel.onCloseShift()

            assertThat(repository.closedShiftIds).containsExactly(1L)
            assertThat(repository.closedActualCash).containsExactly(145_000L)
            val state = viewModel.state.value
            assertThat(state.mode).isEqualTo(ShiftSheetMode.HIDDEN)
            assertThat(state.activeShift).isNull()
            assertThat(state.message?.resolve(IdStrings)).isEqualTo("Shift Dini ditutup.")
        }

    @Test
    fun switchingACashierOpensTheNextShift() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(activeShift, summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()
        viewModel.onActualEndingCashChange("150000")
        viewModel.onNextCashierNameChange("Budi")
        viewModel.onNextStartingCashChange("200000")

        viewModel.onSwitchShift()

        assertThat(repository.switchCalls).containsExactly(
            SwitchCall(
                closeShiftId = 1L,
                actualEndingCash = 150_000L,
                nextCashierName = "Budi",
                nextStartingCash = 200_000L,
            ),
        )
        val state = viewModel.state.value
        assertThat(state.mode).isEqualTo(ShiftSheetMode.HIDDEN)
        assertThat(state.activeShift?.cashierName).isEqualTo("Budi")
        assertThat(state.message?.resolve(IdStrings))
            .isEqualTo("Shift Dini ditutup. Budi mulai bertugas.")
    }

    @Test
    fun switchingACashierRequiresTheNextCashierName() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(activeShift, summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()
        viewModel.onActualEndingCashChange("150000")
        viewModel.onNextStartingCashChange("200000")

        viewModel.onSwitchShift()

        assertThat(viewModel.state.value.validationMessage?.resolve(IdStrings))
            .isEqualTo(IdStrings.shiftValidationNextCashier)
        assertThat(repository.switchCalls).isEmpty()
    }

    @Test
    fun historyShowsTheActiveShiftWithLiveExpectedCash() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(activeShift, summary)
        val viewModel = createViewModel(repository)

        viewModel.onOpenHistory()

        val entry = viewModel.state.value.history.single()
        assertThat(entry.isActive).isTrue()
        assertThat(entry.expectedEndingCash).isEqualTo(150_000L)
        assertThat(viewModel.state.value.mode).isEqualTo(ShiftSheetMode.HISTORY)
    }

    private data class SwitchCall(
        val closeShiftId: Long,
        val actualEndingCash: Long,
        val nextCashierName: String,
        val nextStartingCash: Long,
    )

    private class FakeShiftRepository(
        initialActiveShift: CashierShift?,
        private val summary: ShiftCashSummary,
    ) : ShiftRepository {

        private val activeShiftState = MutableStateFlow(initialActiveShift)
        private var nextShiftId = (initialActiveShift?.id ?: 0L) + 1L

        val openedCashierNames = mutableListOf<String>()
        val openedStartingCash = mutableListOf<Long>()
        val closedShiftIds = mutableListOf<Long>()
        val closedActualCash = mutableListOf<Long>()
        val switchCalls = mutableListOf<SwitchCall>()

        override fun observeActiveShift(): Flow<CashierShift?> = activeShiftState

        override suspend fun getActiveShift(): CashierShift? = activeShiftState.value

        override suspend fun getRecentCashierNames(limit: Int): List<String> =
            listOfNotNull(activeShiftState.value?.cashierName)

        override suspend fun getShiftHistory(limit: Int): List<CashierShift> =
            listOfNotNull(activeShiftState.value)

        override suspend fun getShiftSummary(shiftId: Long): AppResult<ShiftCashSummary> =
            AppResult.Success(summary)

        override suspend fun openShift(cashierName: String, startingCash: Long): AppResult<CashierShift> {
            openedCashierNames += cashierName
            openedStartingCash += startingCash
            val shift = CashierShift(
                id = nextShiftId++,
                cashierName = cashierName,
                openedAt = 2_000L,
                closedAt = null,
                startingCash = startingCash,
                expectedEndingCash = null,
                actualEndingCash = null,
            )
            activeShiftState.value = shift
            return AppResult.Success(shift)
        }

        override suspend fun closeShift(shiftId: Long, actualEndingCash: Long): AppResult<CashierShift> {
            closedShiftIds += shiftId
            closedActualCash += actualEndingCash
            val shift = checkNotNull(activeShiftState.value)
            val closed = shift.copy(closedAt = 3_000L, actualEndingCash = actualEndingCash)
            activeShiftState.value = null
            return AppResult.Success(closed)
        }

        override suspend fun switchShift(
            closeShiftId: Long,
            actualEndingCash: Long,
            nextCashierName: String,
            nextStartingCash: Long,
        ): AppResult<ShiftSwitchResult> {
            switchCalls += SwitchCall(closeShiftId, actualEndingCash, nextCashierName, nextStartingCash)
            val closed = checkNotNull(activeShiftState.value).copy(
                closedAt = 3_000L,
                actualEndingCash = actualEndingCash,
            )
            val opened = CashierShift(
                id = nextShiftId++,
                cashierName = nextCashierName,
                openedAt = 3_000L,
                closedAt = null,
                startingCash = nextStartingCash,
                expectedEndingCash = null,
                actualEndingCash = null,
            )
            activeShiftState.value = opened
            return AppResult.Success(ShiftSwitchResult(closedShift = closed, openedShift = opened))
        }
    }
}
