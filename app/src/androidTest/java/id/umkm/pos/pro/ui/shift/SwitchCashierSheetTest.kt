package id.umkm.pos.pro.ui.shift

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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

    private fun testActions(
        onSelectStaff: (Long) -> Unit = {},
        onSelectNextStaff: (Long) -> Unit = {},
        onSwitchOperator: (Long) -> Unit = {},
        onCloseShift: () -> Unit = {},
        onSwitchShift: () -> Unit = {},
    ): ShiftActions = ShiftActions(
        onSelectStaff = onSelectStaff,
        onStartingCashChange = {},
        onActualEndingCashChange = {},
        onSelectNextStaff = onSelectNextStaff,
        onNextStartingCashChange = {},
        onVarianceAcknowledgedChange = {},
        onOpenShift = {},
        onCloseShift = onCloseShift,
        onSwitchShift = onSwitchShift,
        onSwitchOperator = onSwitchOperator,
        onDismissForm = {},
        onDismissHistory = {},
        onRetryHistory = {},
        onMessageConsumed = {},
    )

    private val noOpActions = testActions()

    private fun showSheet(
        state: ShiftUiState,
        actions: ShiftActions = noOpActions,
        size: DpSize = DpSize(360.dp, 800.dp),
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                        SwitchCashierSheet(state = state, actions = actions)
                    }
                }
            }
        }
    }

    private fun idleOperatorState(): ShiftUiState = ShiftUiState(
        mode = ShiftSheetMode.SHIFT_FORM,
        activeShift = activeShift,
        summary = summary,
        staffOptions = listOf(dini, budi),
        activeStaff = dini,
    )

    @Test
    fun phoneOpenShiftFormShowsStaffPickerWithoutAFreeTextCashierField() {
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
        composeRule.onAllNodesWithText(IdStrings.shiftStartingCashLabel).onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_PRIMARY_ACTION_TEST_TAG)
            .assert(hasText(IdStrings.shiftOpenSubmit))
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
        composeRule.onNodeWithTag(SHIFT_PRIMARY_ACTION_TEST_TAG)
            .assert(hasText(IdStrings.shiftOpenSubmit))
    }

    @Test
    fun phoneOpenShiftFormSelectsAStaffRow() {
        val selectedStaffIds = mutableListOf<Long>()
        showSheet(
            state = ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                staffOptions = listOf(dini, budi),
                selectedStaffId = dini.id,
            ),
            actions = testActions(onSelectStaff = { staffId -> selectedStaffIds += staffId }),
        )

        composeRule.onNodeWithTag(SHIFT_STAFF_ROW_TEST_TAG_PREFIX + budi.id).performClick()

        assertThat(selectedStaffIds).containsExactly(budi.id)
    }

    @Test
    fun phoneActiveShiftShowsTheOperatorAndCloseTabs() {
        showSheet(idleOperatorState())

        composeRule.onNodeWithTag(SHIFT_SHEET_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_OPERATOR_TAB_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_CLOSE_TAB_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Budi").assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_PRIMARY_ACTION_TEST_TAG)
            .assert(hasText(IdStrings.shiftSwitchOperatorAction))
        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG).assertDoesNotExist()

        composeRule.onNodeWithTag(SHIFT_CLOSE_TAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(SHIFT_OPERATOR_TAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun phoneOperatorTabShowsOnDutyCashierAndOnlyOtherStaff() {
        val selectedStaffIds = mutableListOf<Long>()
        showSheet(
            state = idleOperatorState(),
            actions = testActions(onSelectStaff = { staffId -> selectedStaffIds += staffId }),
        )

        composeRule.onNodeWithText(IdStrings.staffOnDutyLabel).assertIsDisplayed()
        composeRule.onAllNodesWithText("Dini").assertCountEquals(1)
        composeRule.onNodeWithTag(SHIFT_STAFF_ROW_TEST_TAG_PREFIX + budi.id).assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_STAFF_ROW_TEST_TAG_PREFIX + dini.id).assertDoesNotExist()

        composeRule.onNodeWithTag(SHIFT_STAFF_ROW_TEST_TAG_PREFIX + budi.id).performClick()

        assertThat(selectedStaffIds).containsExactly(budi.id)
    }

    @Test
    fun phoneCloseTabShowsSummaryVarianceAndCloseActions() {
        showSheet(
            idleOperatorState().copy(actualEndingCashInput = "145000"),
        )

        composeRule.onNodeWithTag(SHIFT_CLOSE_TAB_TEST_TAG).performClick()

        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftVarianceLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftVarianceAcknowledge).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_PRIMARY_ACTION_TEST_TAG)
            .assert(hasText(IdStrings.shiftCloseOnly))
        composeRule.onNodeWithTag(SHIFT_STAFF_ROW_TEST_TAG_PREFIX + budi.id).assertDoesNotExist()
        composeRule.onNodeWithText(IdStrings.shiftNextStartingCashLabel).assertDoesNotExist()

        composeRule.onNodeWithTag(SHIFT_HANDOVER_TOGGLE_TEST_TAG).performScrollTo().performClick()

        composeRule.onNodeWithText(IdStrings.shiftNextCashierTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_STAFF_ROW_TEST_TAG_PREFIX + budi.id)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_PRIMARY_ACTION_TEST_TAG)
            .assert(hasText(IdStrings.shiftCloseAndSwitch))
    }

    @Test
    fun phoneCloseTabHidesZeroDebtAndRefundRows() {
        showSheet(
            idleOperatorState().copy(
                summary = summary.copy(cashDebtPayments = 0L, cashRefunds = 0L),
            ),
        )

        composeRule.onNodeWithTag(SHIFT_CLOSE_TAB_TEST_TAG).performClick()

        composeRule.onNodeWithText(IdStrings.shiftCashSalesLabel).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftCashDebtPaymentsLabel).assertDoesNotExist()
        composeRule.onNodeWithText(IdStrings.shiftCashRefundsLabel).assertDoesNotExist()
    }

    @Test
    fun phoneCloseTabStaysUsableAtOneHundredAndThirtyFivePercentFontScale() {
        showSheet(
            state = idleOperatorState().copy(actualEndingCashInput = "150000"),
            fontScale = 1.35f,
        )

        composeRule.onNodeWithTag(SHIFT_CLOSE_TAB_TEST_TAG).performClick()

        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_PRIMARY_ACTION_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun tabletShowsTheShiftSheet() {
        showSheet(
            state = idleOperatorState(),
            size = DpSize(1000.dp, 800.dp),
        )

        composeRule.onNodeWithTag(SHIFT_SHEET_TEST_TAG).assertIsDisplayed()
    }
}
