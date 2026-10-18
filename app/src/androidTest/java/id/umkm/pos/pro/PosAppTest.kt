package id.umkm.pos.pro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
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
    fun primaryDestinationsAreDisplayed() {
        val startButton = composeRule.onAllNodesWithText("Mulai")
        if (startButton.fetchSemanticsNodes().isNotEmpty()) {
            startButton.onFirst().performClick()
            composeRule.waitForIdle()
        }

        composeRule.onAllNodesWithText("Kasir").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Produk").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Transaksi").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Laporan").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Lainnya").onFirst().assertIsDisplayed()
    }
}
