package id.umkm.pos.pro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import id.umkm.pos.pro.ui.COMPACT_NAVIGATION_TEST_TAG
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.localizedLabel
import id.umkm.pos.pro.ui.navigation.PosDestination
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PosAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun ensureOnboardingCompleted() {
        val app = ApplicationProvider.getApplicationContext<PosApplication>()
        app.container.onboardingStore.markOnboardingCompleted()
    }

    @Test
    fun activeDestinationLabelAndOtherDestinationsAreReachable() {
        val startButton = composeRule.onAllNodesWithText("Mulai")
        if (startButton.fetchSemanticsNodes().isNotEmpty()) {
            startButton.onFirst().performClick()
            composeRule.waitForIdle()
        }

        val activeDestination = PosDestination.KASIR
        val activeLabel = activeDestination.localizedLabel(IdStrings)
        val isCompactNavigation = composeRule.onAllNodesWithTag(COMPACT_NAVIGATION_TEST_TAG)
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (isCompactNavigation) {
            composeRule.onNode(
                hasText(activeLabel) and hasAnyAncestor(hasTestTag(COMPACT_NAVIGATION_TEST_TAG)),
            ).assertIsDisplayed()
            PosDestination.entries
                .filter { destination -> destination != activeDestination }
                .forEach { destination ->
                    composeRule.onNodeWithContentDescription(destination.localizedLabel(IdStrings))
                        .assertIsDisplayed()
                }
        } else {
            PosDestination.entries.forEach { destination ->
                composeRule.onAllNodesWithText(destination.localizedLabel(IdStrings))
                    .onFirst()
                    .assertIsDisplayed()
            }
        }
    }
}
