package id.umkm.pos.pro.ui.shift

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SwitchCashierSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dini = Staff(id = 1L, name = "Dini")
    private val budi = Staff(id = 2L, name = "Budi")

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

    private val noOpActions = ShiftActions(
        onSelectStaff = {},
        onStartingCashChange = {},
        onActualEndingCashChange = {},
        onSelectNextStaff = {},
        onNextStartingCashChange = {},
        onVarianceAcknowledgedChange = {},
        onOpenShift = {},
        onCloseShift = {},
        onSwitchShift = {},
        onSwitchOperator = {},
        onDismissForm = {},
        onDismissHistory = {},
        onRetryHistory = {},
        onMessageConsumed = {},
    )

    private fun showSheet(
        state: ShiftUiState,
        actions: ShiftActions = noOpActions,
    ) {
        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)),
                ) {
                    SwitchCashierSheet(state = state, actions = actions)
                }
            }
        }
    }

    @Test
    fun phoneOpenShiftFormShowsStaffChipsWithoutAFreeTextCashierField() {
        showSheet(
            ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                staffOptions = listOf(dini, budi),
                selectedStaffId = dini.id,
            ),
        )

        composeRule.onNodeWithTag(SHIFT_SHEET_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_CASHIER_NAME_FIELD_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Dini").assertIsDisplayed()
        composeRule.onNodeWithText("Budi").assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftStartingCashLabel).assertIsDisplayed()
        composeRule.onAllNodesWithText(IdStrings.shiftOpenSubmit).onFirst().assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
    }

    @Test
    fun phoneOpenShiftFormWithoutStaffShowsTheStaffEmptyState() {
        showSheet(
            ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                staffOptions = emptyList(),
            ),
        )

        composeRule.onNodeWithText(IdStrings.shiftNoStaffTitle).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftNoStaffHint).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftManageStaffAction).assertIsDisplayed()
        composeRule.onAllNodesWithText(IdStrings.shiftOpenSubmit).onFirst().assertIsDisplayed()
    }

    @Test
    fun phoneOpenShiftFormSelectsAStaffChip() {
        val selectedStaffIds = mutableListOf<Long>()
        showSheet(
            state = ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                staffOptions = listOf(dini, budi),
                selectedStaffId = dini.id,
            ),
            actions = ShiftActions(
                onSelectStaff = { staffId -> selectedStaffIds += staffId },
                onStartingCashChange = {},
                onActualEndingCashChange = {},
                onSelectNextStaff = {},
                onNextStartingCashChange = {},
                onVarianceAcknowledgedChange = {},
                onOpenShift = {},
                onCloseShift = {},
                onSwitchShift = {},
                onSwitchOperator = {},
                onDismissForm = {},
                onDismissHistory = {},
                onRetryHistory = {},
                onMessageConsumed = {},
            ),
        )

        composeRule.onNodeWithText("Budi").performClick()

        assertThat(selectedStaffIds).containsExactly(budi.id)
    }

    @Test
    fun phoneActiveShiftShowsTheOperatorAndCloseTabs() {
        showSheet(
            ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                activeShift = activeShift,
                summary = summary,
                staffOptions = listOf(dini, budi),
                activeStaff = dini,
            ),
        )

        composeRule.onNodeWithTag(SHIFT_SHEET_TEST_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithText(IdStrings.shiftOperatorTab).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText(IdStrings.shiftCloseTab).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Budi").assertIsDisplayed()
        composeRule.onAllNodesWithText(IdStrings.shiftSwitchOperatorAction).onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun phoneCloseTabShowsSummaryVarianceAndCloseActions() {
        showSheet(
            ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                activeShift = activeShift,
                summary = summary,
                staffOptions = listOf(dini, budi),
                activeStaff = dini,
                actualEndingCashInput = "145000",
            ),
        )

        composeRule.onNode(hasText(IdStrings.shiftCloseTab) and hasClickAction()).performClick()

        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftVarianceLabel).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftVarianceAcknowledge).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftCloseAndSwitch).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftCloseOnly).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftNextCashierTitle).performScrollTo().assertIsDisplayed()
    }
}
