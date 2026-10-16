package id.umkm.pos.pro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class PosAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun compactWindowShowsAllPrimaryDestinations() {
        composeRule.onNodeWithText("Cashier").assertIsDisplayed()
        composeRule.onNodeWithText("Products").assertIsDisplayed()
        composeRule.onNodeWithText("Customers").assertIsDisplayed()
        composeRule.onNodeWithText("Reports").assertIsDisplayed()
        composeRule.onNodeWithText("More").assertIsDisplayed()
    }
}
