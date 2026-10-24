package id.umkm.pos.pro.ui.cashier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CashierPaymentSuggestionsTest {

    @Test
    fun suggestionsRoundUpToHelpfulDenominations() {
        assertThat(quickCashSuggestions(114_000L))
            .containsExactly(120_000L, 150_000L, 200_000L)
            .inOrder()
    }

    @Test
    fun suggestionsStayOrderedAndAboveTheTotal() {
        val suggestions = quickCashSuggestions(55_000L)

        assertThat(suggestions).isNotEmpty()
        assertThat(suggestions).isInOrder()
        assertThat(suggestions).containsNoDuplicates()
        suggestions.forEach { amount -> assertThat(amount).isGreaterThan(55_000L) }
    }

    @Test
    fun everySuggestionCoversTheTotal() {
        listOf(0L, 4_000L, 18_500L, 99_999L, 1_250_000L).forEach { total ->
            quickCashSuggestions(total).forEach { amount ->
                assertThat(amount).isAtLeast(total)
            }
        }
    }
}
