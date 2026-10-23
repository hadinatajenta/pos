package id.umkm.pos.pro.ui.more

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.hub.HubGroup
import id.umkm.pos.pro.ui.components.hub.HubRow
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import org.junit.Rule
import org.junit.Test

class MoreHubTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val defaultActiveShift = CashierShift(
        id = 1L,
        cashierName = "Sari",
        openedAt = 0L,
        closedAt = null,
        startingCash = 100_000L,
        expectedEndingCash = null,
        actualEndingCash = null,
    )

    private fun showHero(
        storeName: String? = "Warung Sari",
        cashierLabel: String? = "Sari",
        isStoreLoading: Boolean = false,
        activeShift: CashierShift? = defaultActiveShift,
        operatorName: String? = "Sari",
        expectedCash: ExpectedCashState = ExpectedCashState.Ready(125_000L),
        onRetryExpectedCash: () -> Unit = {},
    ) {
        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalPosStrings provides IdStrings) {
                    MoreHeroSection(
                        storeName = storeName,
                        cashierLabel = cashierLabel,
                        taxRatePercent = 11,
                        isStoreLoading = isStoreLoading,
                        activeShift = activeShift,
                        operatorName = operatorName,
                        expectedCash = expectedCash,
                        onShiftAction = {},
                        onOpenHistory = {},
                        onRetryExpectedCash = onRetryExpectedCash,
                    )
                }
            }
        }
    }

    @Test
    fun heroShowsStoreIdentityAndReadyCash() {
        showHero()

        composeRule.onNodeWithText("Warung Sari").assertIsDisplayed()
        composeRule.onNodeWithText("Sari • ${IdStrings.moreTaxSummary(11)}").assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.moreShiftActiveSubtitle("Sari")).assertIsDisplayed()
        composeRule.onNodeWithTag(MoreHubTestTags.CASH_VALUE).assertIsDisplayed()
        composeRule.onNodeWithText("Rp 125.000").assertIsDisplayed()
    }

    @Test
    fun failedExpectedCashOffersRetry() {
        var retryCount = 0
        showHero(
            expectedCash = ExpectedCashState.Error,
            onRetryExpectedCash = { retryCount += 1 },
        )

        composeRule.onNodeWithTag(MoreHubTestTags.CASH_ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag(MoreHubTestTags.CASH_RETRY).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.moreCashLoadFailed).assertIsDisplayed()
        composeRule.onNodeWithTag(MoreHubTestTags.CASH_VALUE).assertDoesNotExist()

        composeRule.onNodeWithTag(MoreHubTestTags.CASH_RETRY).performClick()

        composeRule.runOnIdle { assertThat(retryCount).isEqualTo(1) }
    }

    @Test
    fun loadingStoreUsesHonestCopy() {
        showHero(
            storeName = null,
            cashierLabel = null,
            isStoreLoading = true,
        )

        composeRule.onNodeWithText(IdStrings.moreStoreLoading).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.moreStoreUnnamed).assertDoesNotExist()
        composeRule.onNodeWithText("Toko Pookie POS").assertDoesNotExist()
    }

    @Test
    fun hubRowShowsDescriptionAndRunsClick() {
        var isSettingsRowClicked = false
        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalPosStrings provides IdStrings) {
                    HubGroup {
                        HubRow(
                            title = IdStrings.moreHubSettings,
                            description = IdStrings.moreSettingsDescription,
                            iconRes = R.drawable.ic_settings,
                            onClick = { isSettingsRowClicked = true },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText(IdStrings.moreSettingsDescription).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.moreHubSettings).performClick()

        composeRule.runOnIdle { assertThat(isSettingsRowClicked).isTrue() }
    }

    @Test
    fun emptyDetailShowsTheSelectionPrompt() {
        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalPosStrings provides IdStrings) {
                    MoreEmptyDetail()
                }
            }
        }

        composeRule.onNodeWithTag(MoreHubTestTags.EMPTY_DETAIL).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.moreSelectPrompt).assertIsDisplayed()
    }

    @Test
    fun noActiveShiftHidesExpectedCash() {
        showHero(
            cashierLabel = null,
            activeShift = null,
            operatorName = null,
            expectedCash = ExpectedCashState.Loading,
        )

        composeRule.onNodeWithText(IdStrings.moreShiftEmptySubtitle).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.cashierShiftOpenAction).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.moreShiftExpectedCash).assertDoesNotExist()
    }
}
