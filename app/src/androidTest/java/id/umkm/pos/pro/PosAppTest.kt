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
        composeRule.onNodeWithText("Beranda").assertIsDisplayed()
        composeRule.onNodeWithText("Kasir").assertIsDisplayed()
        composeRule.onNodeWithText("Produk").assertIsDisplayed()
        composeRule.onNodeWithText("Laporan").assertIsDisplayed()
        composeRule.onNodeWithText("Lainnya").assertIsDisplayed()
    }
}
