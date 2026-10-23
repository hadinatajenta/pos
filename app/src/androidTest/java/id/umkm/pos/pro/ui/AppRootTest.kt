package id.umkm.pos.pro.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.navigation.PosDestination
import id.umkm.pos.pro.ui.localizedLabel
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import id.umkm.pos.pro.ui.welcome.WELCOME_SCREEN_TEST_TAG
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppRootTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun resetOnboardingState() {
        AppRootFakeContainer.isOnboardingCompleted = true
        AppRootFakeContainer.resetActiveShift()
    }

    private fun showConstrainedAppRoot(windowWidth: Dp) {
        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalAppContainer provides AppRootFakeContainer) {
                    Box(modifier = Modifier.requiredWidth(windowWidth).fillMaxHeight()) {
                        AppRoot()
                    }
                }
            }
        }
    }

    private fun showAppRoot() {
        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalAppContainer provides AppRootFakeContainer) {
                    AppRoot()
                }
            }
        }
    }

    private fun assertNavigationIsUsable(testTag: String) {
        composeRule.onNodeWithTag(testTag).assertIsDisplayed()
        val activeDestination = PosDestination.KASIR
        composeRule.onAllNodesWithText(activeDestination.localizedLabel(IdStrings)).onFirst().assertIsDisplayed()
        PosDestination.entries
            .filter { destination -> destination != activeDestination }
            .forEach { destination ->
                val label = destination.localizedLabel(IdStrings)
                composeRule.onAllNodes(
                    hasText(label) and hasAnyAncestor(hasTestTag(testTag)),
                ).assertCountEquals(0)
                composeRule.onNodeWithContentDescription(label).assertIsDisplayed()
            }
    }

    @Test
    fun compactWindowShowsBottomNavigationOnly() {
        showConstrainedAppRoot(windowWidth = 360.dp)

        assertNavigationIsUsable(COMPACT_NAVIGATION_TEST_TAG)
        composeRule.onNodeWithTag(EXPANDED_NAVIGATION_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun wideConstraintsKeepTheLeftRailOnly() {
        showConstrainedAppRoot(windowWidth = 800.dp)

        assertNavigationIsUsable(EXPANDED_NAVIGATION_TEST_TAG)
        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun expandedDeviceKeepsTheLeftRailOnly() {
        assumeTrue(composeRule.activity.resources.configuration.screenWidthDp >= 600)

        showAppRoot()

        assertNavigationIsUsable(EXPANDED_NAVIGATION_TEST_TAG)
        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertDoesNotExist()
    }


    @Test
    fun cashierIsTheDefaultDestination() {
        showConstrainedAppRoot(windowWidth = 360.dp)

        composeRule.onNodeWithText("Cari produk").assertIsDisplayed()
    }

    @Test
    fun welcomeScreenIsShownUntilOnboardingIsCompleted() {
        AppRootFakeContainer.isOnboardingCompleted = false

        showAppRoot()

        composeRule.onNodeWithTag(WELCOME_SCREEN_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun startButtonCompletesOnboardingAndRevealsTheApp() {
        AppRootFakeContainer.isOnboardingCompleted = false

        showAppRoot()
        composeRule.onNodeWithText("Mulai").performClick()

        composeRule.onNodeWithTag(WELCOME_SCREEN_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("Cari produk").assertIsDisplayed()
    }
}
