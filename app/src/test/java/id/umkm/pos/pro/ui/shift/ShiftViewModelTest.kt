package id.umkm.pos.pro.ui.shift

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ShiftRepository
import id.umkm.pos.pro.core.contract.StaffRepository
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.ShiftSwitchResult
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.IdStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ShiftViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dini = Staff(id = 1L, name = "Dini")
    private val budi = Staff(id = 2L, name = "Budi")
    private val citra = Staff(id = 3L, name = "Citra", isActive = false)

    private val activeShift = CashierShift(
        id = 1L,
        cashierName = "Dini",
        openedAt = 1_000L,
        closedAt = null,
        startingCash = 100_000L,
        expectedEndingCash = null,
        actualEndingCash = null,
        openedByStaffId = dini.id,
    )

    private val summary = ShiftCashSummary(
        startingCash = 100_000L,
        cashSales = 40_000L,
        cashDebtPayments = 10_000L,
        cashRefunds = 0L,
        transactionCount = 3,
    )

    private fun createViewModel(
        shiftRepository: FakeShiftRepository = FakeShiftRepository(activeShift, summary),
        staffRepository: FakeStaffRepository = FakeStaffRepository(
            staff = listOf(dini, budi, citra),
            activeStaffId = dini.id,
        ),
    ): ShiftViewModel = ShiftViewModel(
        shiftRepository = shiftRepository,
        staffRepository = staffRepository,
    )

    @Test
    fun openingAFormWithoutAnActiveShiftSelectsTheActiveStaff() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(repository)

        viewModel.onOpenShiftForm()

        val state = viewModel.state.value
        assertThat(state.mode).isEqualTo(ShiftSheetMode.SHIFT_FORM)
        assertThat(state.activeShift).isNull()
        assertThat(state.staffOptions.map(Staff::name)).containsExactly("Dini", "Budi")
        assertThat(state.activeStaff?.id).isEqualTo(dini.id)
        assertThat(state.selectedStaffId).isEqualTo(dini.id)
        assertThat(state.selectedNextStaffId).isNull()
    }

    @Test
    fun openingAShiftWithoutAnyStaffShowsTheCashierValidation() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(
            shiftRepository = repository,
            staffRepository = FakeStaffRepository(seedDefaultOnEnsure = false),
        )
        viewModel.onOpenShiftForm()

        assertThat(viewModel.state.value.staffOptions).isEmpty()
        assertThat(viewModel.state.value.selectedStaffId).isNull()

        viewModel.onStartingCashChange("50000")
        viewModel.onOpenShift()

        assertThat(viewModel.state.value.validationMessage?.resolve(IdStrings))
            .isEqualTo(IdStrings.shiftValidationStaff)
        assertThat(repository.openedStaffIds).isEmpty()
    }

    @Test
    fun openingAShiftRequiresAStartingCashAmount() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()

        viewModel.onOpenShift()

        assertThat(viewModel.state.value.validationMessage?.resolve(IdStrings))
            .isEqualTo(IdStrings.shiftValidationStartingCash)
        assertThat(repository.openedStaffIds).isEmpty()
    }

    @Test
    fun openingAShiftRecordsTheSelectedStaffAndStartingCash() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(initialActiveShift = null, summary = summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()

        viewModel.onSelectStaff(budi.id)
        viewModel.onStartingCashChange("75000")
        viewModel.onOpenShift()

        assertThat(repository.openedStaffIds).containsExactly(budi.id)
        assertThat(repository.openedStartingCash).containsExactly(75_000L)
        val state = viewModel.state.value
        assertThat(state.mode).isEqualTo(ShiftSheetMode.HIDDEN)
        assertThat(state.activeShift?.cashierName).isEqualTo("Budi")
        assertThat(state.activeShift?.openedByStaffId).isEqualTo(budi.id)
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
    fun switchingACashierRequiresTheNextStaffSelection() = runTest(mainDispatcherRule.testDispatcher) {
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
    fun switchingACashierOpensTheNextShiftForTheSelectedStaff() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShiftRepository(activeShift, summary)
        val viewModel = createViewModel(repository)
        viewModel.onOpenShiftForm()
        viewModel.onActualEndingCashChange("150000")
        viewModel.onSelectNextStaff(budi.id)
        viewModel.onNextStartingCashChange("200000")

        viewModel.onSwitchShift()

        assertThat(repository.switchCalls).containsExactly(
            SwitchCall(
                closeShiftId = 1L,
                actualEndingCash = 150_000L,
                nextStaffId = budi.id,
                nextStartingCash = 200_000L,
            ),
        )
        val state = viewModel.state.value
        assertThat(state.mode).isEqualTo(ShiftSheetMode.HIDDEN)
        assertThat(state.activeShift?.cashierName).isEqualTo("Budi")
        assertThat(state.activeShift?.openedByStaffId).isEqualTo(budi.id)
        assertThat(state.message?.resolve(IdStrings))
            .isEqualTo("Shift Dini ditutup. Budi mulai bertugas.")
    }

    @Test
    fun switchingOperatorUpdatesTheActiveStaffAndClosesTheForm() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeShiftRepository(activeShift, summary)
            val viewModel = createViewModel(repository)
            viewModel.onOpenShiftForm()

            viewModel.onSwitchOperator(budi.id)

            assertThat(repository.switchOperatorCalls).containsExactly(budi.id)
            val state = viewModel.state.value
            assertThat(state.mode).isEqualTo(ShiftSheetMode.HIDDEN)
            assertThat(state.activeStaff?.id).isEqualTo(budi.id)
            assertThat(state.activeShift?.cashierName).isEqualTo("Dini")
            assertThat(state.message?.resolve(IdStrings)).isEqualTo("Kasir diganti ke Budi.")
        }

    @Test
    fun switchingOperatorReportsAFailureAndKeepsTheCurrentStaff() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeShiftRepository(activeShift, summary)
            val viewModel = createViewModel(repository)
            viewModel.onOpenShiftForm()

            viewModel.onSwitchOperator(citra.id)

            assertThat(repository.switchOperatorCalls).containsExactly(citra.id)
            val state = viewModel.state.value
            assertThat(state.mode).isEqualTo(ShiftSheetMode.SHIFT_FORM)
            assertThat(state.activeStaff?.id).isEqualTo(dini.id)
            assertThat(state.isSubmitting).isFalse()
            assertThat(state.submitErrorMessage?.resolve(IdStrings)).isEqualTo(IdStrings.errorValidation)
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
        val nextStaffId: Long,
        val nextStartingCash: Long,
    )

    private class FakeShiftRepository(
        initialActiveShift: CashierShift?,
        private val summary: ShiftCashSummary,
    ) : ShiftRepository {

        private val activeShiftState = MutableStateFlow(initialActiveShift)
        private var nextShiftId = (initialActiveShift?.id ?: 0L) + 1L
        private val staff = listOf(
            Staff(id = 1L, name = "Dini"),
            Staff(id = 2L, name = "Budi"),
            Staff(id = 3L, name = "Citra", isActive = false),
        )

        val openedStaffIds = mutableListOf<Long>()
        val openedStartingCash = mutableListOf<Long>()
        val closedShiftIds = mutableListOf<Long>()
        val closedActualCash = mutableListOf<Long>()
        val switchCalls = mutableListOf<SwitchCall>()
        val switchOperatorCalls = mutableListOf<Long>()

        override fun observeActiveShift(): Flow<CashierShift?> = activeShiftState

        override suspend fun getActiveShift(): CashierShift? = activeShiftState.value

        override suspend fun getRecentCashierNames(limit: Int): List<String> =
            listOfNotNull(activeShiftState.value?.cashierName)

        override suspend fun getShiftHistory(limit: Int): List<CashierShift> =
            listOfNotNull(activeShiftState.value)

        override suspend fun getShiftSummary(shiftId: Long): AppResult<ShiftCashSummary> =
            AppResult.Success(summary)

        override suspend fun openShift(staffId: Long, startingCash: Long): AppResult<CashierShift> {
            val member = staff.firstOrNull { it.id == staffId }
                ?: return AppResult.Failure(AppError.NotFound("staff $staffId was not found"))
            openedStaffIds += staffId
            openedStartingCash += startingCash
            val shift = CashierShift(
                id = nextShiftId++,
                cashierName = member.name,
                openedAt = 2_000L,
                closedAt = null,
                startingCash = startingCash,
                expectedEndingCash = null,
                actualEndingCash = null,
                openedByStaffId = member.id,
            )
            activeShiftState.value = shift
            return AppResult.Success(shift)
        }

        override suspend fun closeShift(shiftId: Long, actualEndingCash: Long): AppResult<CashierShift> {
            closedShiftIds += shiftId
            closedActualCash += actualEndingCash
            val shift = checkNotNull(activeShiftState.value)
            val closed = shift.copy(
                closedAt = 3_000L,
                actualEndingCash = actualEndingCash,
                closedByStaffId = shift.openedByStaffId,
                closedByName = shift.cashierName,
            )
            activeShiftState.value = null
            return AppResult.Success(closed)
        }

        override suspend fun switchShift(
            closeShiftId: Long,
            actualEndingCash: Long,
            nextStaffId: Long,
            nextStartingCash: Long,
        ): AppResult<ShiftSwitchResult> {
            switchCalls += SwitchCall(closeShiftId, actualEndingCash, nextStaffId, nextStartingCash)
            val nextMember = staff.firstOrNull { it.id == nextStaffId }
                ?: return AppResult.Failure(AppError.NotFound("staff $nextStaffId was not found"))
            val closed = checkNotNull(activeShiftState.value).copy(
                closedAt = 3_000L,
                actualEndingCash = actualEndingCash,
                closedByStaffId = activeShiftState.value?.openedByStaffId,
                closedByName = activeShiftState.value?.cashierName,
            )
            val opened = CashierShift(
                id = nextShiftId++,
                cashierName = nextMember.name,
                openedAt = 3_000L,
                closedAt = null,
                startingCash = nextStartingCash,
                expectedEndingCash = null,
                actualEndingCash = null,
                openedByStaffId = nextMember.id,
            )
            activeShiftState.value = opened
            return AppResult.Success(ShiftSwitchResult(closedShift = closed, openedShift = opened))
        }

        override suspend fun switchOperator(staffId: Long): AppResult<Staff> {
            switchOperatorCalls += staffId
            if (activeShiftState.value == null) {
                return AppResult.Failure(AppError.Validation("no shift is open"))
            }
            val member = staff.firstOrNull { it.id == staffId }
                ?: return AppResult.Failure(AppError.NotFound("staff $staffId was not found"))
            if (!member.isActive) {
                return AppResult.Failure(AppError.Validation("staff $staffId is inactive"))
            }
            return AppResult.Success(member)
        }
    }

    private class FakeStaffRepository(
        staff: List<Staff> = emptyList(),
        activeStaffId: Long? = staff.firstOrNull { it.isActive }?.id,
        private val seedDefaultOnEnsure: Boolean = true,
    ) : StaffRepository {

        private val staffState = MutableStateFlow(staff)
        private val activeStaffIdState = MutableStateFlow(activeStaffId)

        override fun observeStaff(): Flow<List<Staff>> = staffState

        override fun observeActiveStaff(): Flow<Staff?> =
            combine(staffState, activeStaffIdState) { members, activeId ->
                members.firstOrNull { member -> member.id == activeId && member.isActive }
            }

        override suspend fun getStaff(staffId: Long): Staff? =
            staffState.value.firstOrNull { it.id == staffId }

        override suspend fun getActiveStaff(): Staff? =
            staffState.value.firstOrNull { it.id == activeStaffIdState.value && it.isActive }

        override suspend fun addStaff(name: String): AppResult<Staff> {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                return AppResult.Failure(AppError.Validation("staff name is required"))
            }
            if (staffState.value.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                return AppResult.Failure(AppError.Validation("staff name already exists"))
            }
            val member = Staff(
                id = (staffState.value.maxOfOrNull(Staff::id) ?: 0L) + 1L,
                name = trimmedName,
            )
            staffState.value = staffState.value + member
            if (activeStaffIdState.value == null) activeStaffIdState.value = member.id
            return AppResult.Success(member)
        }

        override suspend fun renameStaff(staffId: Long, name: String): AppResult<Staff> {
            val existing = staffState.value.firstOrNull { it.id == staffId }
                ?: return AppResult.Failure(AppError.NotFound("staff $staffId was not found"))
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                return AppResult.Failure(AppError.Validation("staff name is required"))
            }
            if (staffState.value.any { it.id != staffId && it.name.equals(trimmedName, ignoreCase = true) }) {
                return AppResult.Failure(AppError.Validation("staff name already exists"))
            }
            val updated = existing.copy(name = trimmedName)
            staffState.value = staffState.value.map { member ->
                if (member.id == staffId) updated else member
            }
            return AppResult.Success(updated)
        }

        override suspend fun setStaffActive(staffId: Long, isActive: Boolean): AppResult<Staff> {
            val existing = staffState.value.firstOrNull { it.id == staffId }
                ?: return AppResult.Failure(AppError.NotFound("staff $staffId was not found"))
            val updated = existing.copy(isActive = isActive)
            staffState.value = staffState.value.map { member ->
                if (member.id == staffId) updated else member
            }
            if (!isActive && activeStaffIdState.value == staffId) {
                activeStaffIdState.value = staffState.value.firstOrNull {
                    it.isActive && it.id != staffId
                }?.id
            }
            return AppResult.Success(updated)
        }

        override suspend fun ensureDefaultStaff(): Staff {
            val current = getActiveStaff()
            if (current != null) return current
            val existingDefault = staffState.value.firstOrNull { it.name == "Kasir Utama" }
            val member = if (existingDefault != null) {
                existingDefault.copy(isActive = true)
            } else {
                Staff(
                    id = (staffState.value.maxOfOrNull(Staff::id) ?: 0L) + 1L,
                    name = "Kasir Utama",
                )
            }
            if (seedDefaultOnEnsure) {
                staffState.value = staffState.value
                    .filterNot { it.id == member.id } + member
                activeStaffIdState.value = member.id
            }
            return member
        }
    }
}
