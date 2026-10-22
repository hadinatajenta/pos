package id.umkm.pos.pro.ui.staff

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class StaffScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val noOpActions = StaffActions(
        onOpenAddEditor = {},
        onOpenRenameEditor = {},
        onNameChange = {},
        onSaveEditor = {},
        onDismissEditor = {},
        onRequestDeactivate = {},
        onConfirmDeactivate = {},
        onDismissDeactivate = {},
        onActivateStaff = {},
        onMessageConsumed = {},
    )

    private fun showScreen(state: StaffUiState) {
        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)),
                ) {
                    StaffScreen(state = state, actions = noOpActions)
                }
            }
        }
    }

    @Test
    fun activeStaffIsListedBeforeInactiveStaff() {
        showScreen(
            StaffUiState(
                staffList = listOf(
                    Staff(id = 2L, name = "Sari", isActive = false),
                    Staff(id = 1L, name = "Dini", isActive = true),
                ),
                activeStaffId = 1L,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag(STAFF_ROW_TEST_TAG_PREFIX + 1L).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_ROW_TEST_TAG_PREFIX + 2L).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.staffActiveSection(1)).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.staffInactiveSection(1)).assertIsDisplayed()
        composeRule.onNodeWithText("Dini").assertIsDisplayed()
        composeRule.onNodeWithText("Sari").assertIsDisplayed()
    }

    @Test
    fun searchFiltersStaffAndCanBeCleared() {
        var state by mutableStateOf(
            StaffUiState(
                staffList = listOf(
                    Staff(id = 1L, name = "Dini", isActive = true),
                    Staff(id = 2L, name = "Sari", isActive = true),
                ),
                activeStaffId = 1L,
                isLoading = false,
            ),
        )
        val actions = StaffActions(
            onOpenAddEditor = {},
            onOpenRenameEditor = {},
            onNameChange = {},
            onSaveEditor = {},
            onDismissEditor = {},
            onRequestDeactivate = {},
            onConfirmDeactivate = {},
            onDismissDeactivate = {},
            onActivateStaff = {},
            onMessageConsumed = {},
            onToggleSearch = { state = state.copy(isSearchVisible = !state.isSearchVisible) },
            onSearchQueryChange = { query -> state = state.copy(searchQuery = query) },
            onClearSearch = { state = state.copy(searchQuery = "") },
        )

        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)),
                ) {
                    StaffScreen(state = state, actions = actions)
                }
            }
        }

        composeRule.onNodeWithTag(STAFF_SEARCH_ACTION_TEST_TAG).performClick()
        composeRule.onNodeWithTag(STAFF_SEARCH_FIELD_TEST_TAG)
            .assertIsDisplayed()
            .performTextInput("Sa")

        composeRule.onNodeWithText("Sari").assertIsDisplayed()
        composeRule.onNodeWithText("Dini").assertDoesNotExist()
    }

    @Test
    fun moreButtonOpensTheStaffMenu() {
        showScreen(
            StaffUiState(
                staffList = listOf(Staff(id = 1L, name = "Dini", isActive = true)),
                activeStaffId = 1L,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag(STAFF_MORE_ACTION_TEST_TAG_PREFIX + 1L).performClick()
        composeRule.onNodeWithText(IdStrings.actionEdit).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.staffDeactivateAction).assertIsDisplayed()
    }

    @Test
    fun emptyStateOffersTheAddAction() {
        showScreen(StaffUiState(isLoading = false))

        composeRule.onNodeWithText(IdStrings.staffEmptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.staffEmptyHint).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_ADD_ACTION_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun addActionOpensTheDialogAndAcceptsAName() {
        var state by mutableStateOf(StaffUiState(isLoading = false))
        val actions = StaffActions(
            onOpenAddEditor = { state = state.copy(editor = StaffEditorState()) },
            onOpenRenameEditor = {},
            onNameChange = { name -> state = state.copy(editor = state.editor?.copy(name = name)) },
            onSaveEditor = {},
            onDismissEditor = { state = state.copy(editor = null) },
            onRequestDeactivate = {},
            onConfirmDeactivate = {},
            onDismissDeactivate = {},
            onActivateStaff = {},
            onMessageConsumed = {},
        )

        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)),
                ) {
                    StaffScreen(state = state, actions = actions)
                }
            }
        }

        composeRule.onNodeWithTag(STAFF_ADD_ACTION_TEST_TAG).performClick()
        composeRule.onNodeWithTag(STAFF_EDITOR_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_NAME_FIELD_TEST_TAG)
            .assertIsDisplayed()
            .performTextInput("Budi")
        composeRule.onNodeWithTag(STAFF_EDITOR_SAVE_TEST_TAG).performClick()

        assertThat(state.editor?.name).isEqualTo("Budi")
    }

    @Test
    fun addDialogRemainsVisibleAtOneHundredThirtyFivePercentFontScale() {
        var state by mutableStateOf(
            StaffUiState(
                staffList = listOf(Staff(id = 1L, name = "Dini", isActive = true)),
                activeStaffId = 1L,
                isLoading = false,
            ),
        )
        val actions = StaffActions(
            onOpenAddEditor = { state = state.copy(editor = StaffEditorState()) },
            onOpenRenameEditor = {},
            onNameChange = { name -> state = state.copy(editor = state.editor?.copy(name = name)) },
            onSaveEditor = {},
            onDismissEditor = { state = state.copy(editor = null) },
            onRequestDeactivate = {},
            onConfirmDeactivate = {},
            onDismissDeactivate = {},
            onActivateStaff = {},
            onMessageConsumed = {},
        )

        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.35f)) {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)),
                    ) {
                        StaffScreen(state = state, actions = actions)
                    }
                }
            }
        }

        composeRule.onNodeWithTag(STAFF_ADD_ACTION_TEST_TAG).performClick()
        composeRule.onNodeWithTag(STAFF_EDITOR_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_NAME_FIELD_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_EDITOR_SAVE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun staffListIsVisibleAtTabletWidth() {
        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(1000.dp, 800.dp)),
                ) {
                    StaffScreen(
                        state = StaffUiState(
                            staffList = listOf(
                                Staff(id = 1L, name = "Dini", isActive = true),
                                Staff(id = 2L, name = "Sari", isActive = false),
                            ),
                            activeStaffId = 1L,
                            isLoading = false,
                        ),
                        actions = noOpActions,
                    )
                }
            }
        }

        composeRule.onNodeWithTag(STAFF_SCREEN_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_ROW_TEST_TAG_PREFIX + 1L).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_ROW_TEST_TAG_PREFIX + 2L).assertIsDisplayed()
    }

    @Test
    fun renameDialogIsPrefilledWithTheStaffName() {
        showScreen(
            StaffUiState(
                staffList = listOf(Staff(id = 1L, name = "Dini", isActive = true)),
                activeStaffId = 1L,
                isLoading = false,
                editor = StaffEditorState(staffId = 1L, name = "Dini"),
            ),
        )

        composeRule.onNodeWithTag(STAFF_EDITOR_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(STAFF_NAME_FIELD_TEST_TAG).assertTextContains("Dini")
    }
}
