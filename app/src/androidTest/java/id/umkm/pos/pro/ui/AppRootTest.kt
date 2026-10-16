package id.umkm.pos.pro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.navigation.PosDestination
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import org.junit.Rule
import org.junit.Test

class AppRootTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showAppRoot(windowWidth: Dp) {
        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalAppContainer provides AppRootFakeContainer) {
                    Box(modifier = Modifier.width(windowWidth).fillMaxHeight()) {
                        AppRoot()
                    }
                }
            }
        }
    }

    @Test
    fun compactWindowShowsBottomNavigationOnly() {
        showAppRoot(windowWidth = 360.dp)

        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(EXPANDED_NAVIGATION_TEST_TAG).assertDoesNotExist()
        PosDestination.entries.forEach { destination ->
            composeRule.onNodeWithText(destination.label).assertIsDisplayed()
        }
    }

    @Test
    fun expandedWindowShowsTheRailOnly() {
        showAppRoot(windowWidth = 800.dp)

        composeRule.onNodeWithTag(EXPANDED_NAVIGATION_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COMPACT_NAVIGATION_TEST_TAG).assertDoesNotExist()
        PosDestination.entries.forEach { destination ->
            composeRule.onNodeWithText(destination.label).assertIsDisplayed()
        }
    }

    @Test
    fun cashierIsTheDefaultDestination() {
        showAppRoot(windowWidth = 360.dp)

        composeRule.onNodeWithText("Cart is empty").assertIsDisplayed()
        composeRule.onNodeWithText("No products yet").assertIsDisplayed()
    }
}
