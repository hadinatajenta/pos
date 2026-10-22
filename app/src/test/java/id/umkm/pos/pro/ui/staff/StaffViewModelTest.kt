package id.umkm.pos.pro.ui.staff

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.IdStrings
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StaffViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun addingStaffTrimsTheNameAndReportsSuccess() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(emptyList())
            val viewModel = StaffViewModel(repository)

            viewModel.onOpenAddEditor()
            viewModel.onNameChange("  Budi  ")
            viewModel.onSaveEditor()

            assertThat(repository.addedNames).containsExactly("Budi")
            assertThat(repository.staffList.map(Staff::name)).contains("Budi")
            assertThat(viewModel.state.value.editor).isNull()
            assertThat(viewModel.state.value.message?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffSaved)
        }

    @Test
    fun addingBlankStaffNameShowsRequiredErrorWithoutTouchingTheRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(emptyList())
            val viewModel = StaffViewModel(repository)

            viewModel.onOpenAddEditor()
            viewModel.onNameChange("   ")
            viewModel.onSaveEditor()

            assertThat(repository.addedNames).isEmpty()
            assertThat(viewModel.state.value.editor?.error?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffNameRequired)
        }

    @Test
    fun addingAStaffNameLongerThanSixtyCharactersShowsLengthError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(emptyList())
            val viewModel = StaffViewModel(repository)

            viewModel.onOpenAddEditor()
            viewModel.onNameChange("a".repeat(61))
            viewModel.onSaveEditor()

            assertThat(repository.addedNames).isEmpty()
            assertThat(viewModel.state.value.editor?.error?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffNameTooLong)
        }

    @Test
    fun renamingToAnExistingNameShowsLocalizedDuplicateError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(
                listOf(
                    Staff(id = 1L, name = "Dini", isActive = true),
                    Staff(id = 2L, name = "Budi", isActive = true),
                ),
            )
            val viewModel = StaffViewModel(repository)
            val budi = viewModel.state.value.staffList.first { staff -> staff.id == 2L }

            viewModel.onOpenRenameEditor(budi)
            viewModel.onNameChange("Dini")
            viewModel.onSaveEditor()

            assertThat(repository.staffList.first { staff -> staff.id == 2L }.name)
                .isEqualTo("Budi")
            assertThat(viewModel.state.value.editor?.error?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffNameDuplicate)
            assertThat(viewModel.state.value.editor?.name).isEqualTo("Dini")
        }

    @Test
    fun renamingAStaffMemberUpdatesThePrefilledDialogName() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(
                listOf(Staff(id = 1L, name = "Dini", isActive = true)),
            )
            val viewModel = StaffViewModel(repository)

            viewModel.onOpenRenameEditor(viewModel.state.value.staffList.single())
            viewModel.onNameChange("Dini Baru")
            viewModel.onSaveEditor()

            val renamed = repository.renamedStaff.single()
            assertThat(renamed.staffId).isEqualTo(1L)
            assertThat(renamed.name).isEqualTo("Dini Baru")
            assertThat(viewModel.state.value.staffList.single().name).isEqualTo("Dini Baru")
            assertThat(viewModel.state.value.message?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffSaved)
        }

    @Test
    fun deactivatingTheLastActiveStaffIsRejectedLocally() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(
                listOf(Staff(id = 1L, name = "Dini", isActive = true)),
            )
            val viewModel = StaffViewModel(repository)

            viewModel.onRequestDeactivate(viewModel.state.value.staffList.single())

            assertThat(viewModel.state.value.pendingDeactivation).isNull()
            assertThat(viewModel.state.value.errorMessage?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffLastActiveError)
            assertThat(repository.activeChanges).isEmpty()
        }

    @Test
    fun deactivatingAStaffMemberRequiresConfirmationBeforeUpdating() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(
                listOf(
                    Staff(id = 1L, name = "Dini", isActive = true),
                    Staff(id = 2L, name = "Budi", isActive = true),
                ),
            )
            val viewModel = StaffViewModel(repository)
            val dini = viewModel.state.value.staffList.first { staff -> staff.id == 1L }

            viewModel.onRequestDeactivate(dini)

            assertThat(viewModel.state.value.pendingDeactivation?.id).isEqualTo(1L)
            assertThat(repository.activeChanges).isEmpty()

            viewModel.onConfirmDeactivate()

            assertThat(repository.activeChanges)
                .containsExactly(ActiveChange(staffId = 1L, isActive = false))
            assertThat(viewModel.state.value.pendingDeactivation).isNull()
            assertThat(viewModel.state.value.message?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffDeactivated)
        }

    @Test
    fun reactivatingAStaffMemberUpdatesTheStatusImmediately() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(
                listOf(
                    Staff(id = 1L, name = "Dini", isActive = true),
                    Staff(id = 2L, name = "Sari", isActive = false),
                ),
            )
            val viewModel = StaffViewModel(repository)
            val sari = viewModel.state.value.staffList.first { staff -> staff.id == 2L }

            viewModel.onActivateStaff(sari)

            assertThat(repository.activeChanges)
                .containsExactly(ActiveChange(staffId = 2L, isActive = true))
            assertThat(viewModel.state.value.staffList.first { it.id == 2L }.isActive).isTrue()
            assertThat(viewModel.state.value.message?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffReactivated)
        }

    @Test
    fun repositoryLastActiveValidationIsMappedToLocalizedError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(
                listOf(
                    Staff(id = 1L, name = "Dini", isActive = true),
                    Staff(id = 2L, name = "Budi", isActive = true),
                ),
            )
            repository.setActiveFailure = AppError.Validation("at least one active staff is required")
            val viewModel = StaffViewModel(repository)
            val dini = viewModel.state.value.staffList.first { staff -> staff.id == 1L }

            viewModel.onRequestDeactivate(dini)
            viewModel.onConfirmDeactivate()

            assertThat(viewModel.state.value.errorMessage?.resolve(IdStrings))
                .isEqualTo(IdStrings.staffLastActiveError)
        }

    @Test
    fun consumingMessagesClearsBothSuccessAndErrorFeedback() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeStaffRepository(
                listOf(Staff(id = 1L, name = "Dini", isActive = true)),
            )
            val viewModel = StaffViewModel(repository)
            viewModel.onRequestDeactivate(viewModel.state.value.staffList.single())

            assertThat(viewModel.state.value.errorMessage).isNotNull()
            viewModel.onMessageConsumed()

            assertThat(viewModel.state.value.errorMessage).isNull()
            assertThat(viewModel.state.value.message).isNull()
        }
}

