package id.umkm.pos.pro.ui.shift

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.ShiftCashSummary
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SwitchCashierSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val noOpActions = ShiftActions(
        onCashierNameChange = {},
        onStartingCashChange = {},
        onActualEndingCashChange = {},
        onNextCashierNameChange = {},
        onNextStartingCashChange = {},
        onVarianceAcknowledgedChange = {},
        onOpenShift = {},
        onCloseShift = {},
        onSwitchShift = {},
        onDismissForm = {},
        onDismissHistory = {},
        onRetryHistory = {},
        onMessageConsumed = {},
    )

    private fun showSheet(state: ShiftUiState) {
        composeRule.setContent {
            UmkmPosTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)),
                ) {
                    SwitchCashierSheet(state = state, actions = noOpActions)
                }
            }
        }
    }

    @Test
    fun phoneNoShiftFormShowsCashierAndStartingCashFields() {
        showSheet(
            ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                cashierNameInput = "Dini",
            ),
        )

        composeRule.onNodeWithTag(SHIFT_SHEET_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_CASHIER_NAME_FIELD_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftStartingCashLabel).assertIsDisplayed()
        composeRule.onAllNodesWithText(IdStrings.shiftOpenSubmit).onFirst().assertIsDisplayed()
    }

    @Test
    fun phoneActiveShiftFormShowsSummaryVarianceAndCloseActions() {
        val activeShift = CashierShift(
            id = 1L,
            cashierName = "Dini",
            openedAt = 1_000L,
            closedAt = null,
            startingCash = 100_000L,
            expectedEndingCash = null,
            actualEndingCash = null,
        )
        showSheet(
            ShiftUiState(
                mode = ShiftSheetMode.SHIFT_FORM,
                activeShift = activeShift,
                summary = ShiftCashSummary(
                    startingCash = 100_000L,
                    cashSales = 40_000L,
                    cashDebtPayments = 10_000L,
                    cashRefunds = 0L,
                    transactionCount = 3,
                ),
                actualEndingCashInput = "145000",
            ),
        )

        composeRule.onNodeWithTag(SHIFT_SHEET_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHIFT_ACTUAL_CASH_FIELD_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftVarianceLabel).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftVarianceAcknowledge).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftCloseAndSwitch).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftCloseOnly).assertIsDisplayed()
    }
}
