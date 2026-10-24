package id.umkm.pos.pro.ui.cashier

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import org.junit.Rule
import org.junit.Test

class CashierCartTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleProduct = Product(
        id = 1L,
        sku = "KOPI-01",
        barcode = null,
        name = "Kopi Susu",
        category = "Minuman",
        baseUnit = "cup",
        baseHpp = 1_000L,
        basePrice = 5_000L,
        stockBaseQty = 10.0,
        minStockAlert = 2.0,
        isActive = true,
    )

    private val sampleCartLine = CashierCartLine(
        lineId = "line-1",
        productId = sampleProduct.id,
        unitId = null,
        productName = sampleProduct.name,
        variantName = null,
        unitLabel = sampleProduct.baseUnit,
        quantity = 1.0,
        unitPrice = sampleProduct.basePrice,
        itemDiscount = 0L,
        lineSubtotal = sampleProduct.basePrice,
        isNegotiated = false,
        isWholesale = false,
        categoryName = sampleProduct.category.orEmpty(),
        stock = sampleProduct.stockBaseQty,
    )

    private val samplePricedCart = PricedCart(
        lines = emptyList(),
        subtotal = 5_000L,
        invoiceDiscount = 0L,
        taxTotal = 0L,
        rounding = 0L,
        grandTotal = 5_000L,
        totalHpp = 1_000L,
        netProfit = 4_000L,
    )

    private val sampleActiveShift = CashierShift(
        id = 1L,
        cashierName = "Dini",
        openedAt = 1_000L,
        closedAt = null,
        startingCash = 100_000L,
        expectedEndingCash = null,
        actualEndingCash = null,
    )

    private fun testActions(
        onOpenQuickShift: (onOpened: () -> Unit) -> Unit = {},
        onOpenShift: (Int, () -> Unit) -> Unit = { _, _ -> },
    ): CashierActions = CashierActions(
        onSearchQueryChange = {},
        onCatalogCategoryChange = {},
        onCatalogAvailableOnlyChange = {},
        onRetryCatalog = {},
        onAddProduct = {},
        onDecreaseProductQuantity = {},
        onIncreaseQuantity = {},
        onDecreaseQuantity = {},
        onRemoveLine = {},
        onOpenCartSheet = {},
        onCloseCartSheet = {},
        onOpenLineEditor = {},
        onCloseLineEditor = {},
        onLineEditorUnitChange = {},
        onLineEditorPriceChange = {},
        onLineEditorDiscountChange = {},
        onApplyLineEditor = {},
        onOpenPaymentSheet = {},
        onClosePaymentSheet = {},
        onPaymentMethodChange = {},
        onAmountReceivedChange = {},
        onConfirmCharge = {},
        onPrintReceipt = {},
        onNewSale = {},
        onMessageConsumed = {},
        onOpenShift = onOpenShift,
        onOpenQuickShift = onOpenQuickShift,
    )

    @Test
    fun cartWithoutShiftShowsNoShiftCtaAndInvokesQuickLauncher() {
        var quickShiftInvoked = false
        val actions = testActions(
            onOpenQuickShift = { quickShiftInvoked = true },
        )
        val state = CashierUiState(
            cartLines = listOf(sampleCartLine),
            pricedCart = samplePricedCart,
            activeShift = null,
        )

        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalPosStrings provides IdStrings) {
                    CashierCartPage(
                        state = state,
                        actions = actions,
                        onBack = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(IdStrings.cashierShiftNotOpen).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftQuickOpenSubmit).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.cashierShiftSwitchAction).assertDoesNotExist()

        composeRule.onNodeWithText(IdStrings.shiftQuickOpenSubmit).performClick()

        composeRule.runOnIdle {
            assertThat(quickShiftInvoked).isTrue()
        }
    }

    @Test
    fun cartWithActiveShiftShowsSwitchCashierAction() {
        var openShiftItemCount = -1
        val actions = testActions(
            onOpenShift = { count, _ -> openShiftItemCount = count },
        )
        val state = CashierUiState(
            cartLines = listOf(sampleCartLine),
            pricedCart = samplePricedCart,
            activeShift = sampleActiveShift,
        )

        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalPosStrings provides IdStrings) {
                    CashierCartPage(
                        state = state,
                        actions = actions,
                        onBack = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(IdStrings.cashierShiftSwitchAction).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.cashierShiftNotOpen).assertDoesNotExist()
        composeRule.onNodeWithText(IdStrings.shiftQuickOpenSubmit).assertDoesNotExist()

        composeRule.onNodeWithText(IdStrings.cashierShiftSwitchAction).performClick()

        composeRule.runOnIdle {
            assertThat(openShiftItemCount).isEqualTo(1)
        }
    }

    @Test
    fun tabletCartPaneShowsNoShiftAndSwitchShiftActions() {
        var quickShiftInvoked = false
        val actions = testActions(
            onOpenQuickShift = { quickShiftInvoked = true },
        )
        val noShiftState = CashierUiState(
            cartLines = listOf(sampleCartLine),
            pricedCart = samplePricedCart,
            activeShift = null,
        )

        composeRule.setContent {
            UmkmPosTheme {
                CompositionLocalProvider(LocalPosStrings provides IdStrings) {
                    CashierCartPane(
                        state = noShiftState,
                        actions = actions,
                    )
                }
            }
        }

        composeRule.onNodeWithText(IdStrings.cashierShiftNotOpen).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.shiftQuickOpenSubmit).assertIsDisplayed()

        composeRule.onNodeWithText(IdStrings.shiftQuickOpenSubmit).performClick()
        composeRule.runOnIdle {
            assertThat(quickShiftInvoked).isTrue()
        }
    }
}
