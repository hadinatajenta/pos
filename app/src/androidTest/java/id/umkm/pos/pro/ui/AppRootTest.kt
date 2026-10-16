package id.umkm.pos.pro.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.navigation.PosDestination
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

    @Test
    fun compactWindowShowsBottomNavigationOnly() {
        showConstrainedAppRoot(windowWidth = 360.dp)

        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(EXPANDED_NAVIGATION_TEST_TAG).assertDoesNotExist()
        PosDestination.entries.forEach { destination ->
            composeRule.onNodeWithText(destination.label).assertIsDisplayed()
        }
    }

    @Test
    fun wideConstraintsSelectTheRailOnly() {
        showConstrainedAppRoot(windowWidth = 800.dp)

        composeRule.onNodeWithTag(EXPANDED_NAVIGATION_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun expandedDeviceShowsTheRailOnly() {
        assumeTrue(composeRule.activity.resources.configuration.screenWidthDp >= 600)

        showAppRoot()

        composeRule.onNodeWithTag(EXPANDED_NAVIGATION_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertDoesNotExist()
        PosDestination.entries.forEach { destination ->
            composeRule.onNodeWithText(destination.label).assertIsDisplayed()
        }
    }

    @Test
    fun dashboardIsTheDefaultDestination() {
        showConstrainedAppRoot(windowWidth = 360.dp)

        composeRule.onNodeWithText("Buka Kasir").assertIsDisplayed()
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
        composeRule.onNodeWithText("Buka Kasir").assertIsDisplayed()
    }
}
