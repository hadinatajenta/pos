package id.umkm.pos.pro.ui.products

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.AppRootFakeContainer
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ProductsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val products = ScreenTestProductRepository()
    private val images = ScreenTestProductImageRepository()
    private val clock = object : Clock {
        override fun nowMillis(): Long = 1_000L
    }

    private fun createViewModel(): ProductsViewModel = ProductsViewModel(
        productRepository = products,
        productImageRepository = images,
        clock = clock,
    )

    private fun showScreen(viewModel: ProductsViewModel) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppContainer provides AppRootFakeContainer) {
                UmkmPosTheme {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 900.dp)),
                    ) {
                        val state by viewModel.state.collectAsState()
                        ProductsScreen(state = state, actions = ProductsActions.from(viewModel))
                    }
                }
            }
        }
    }

    private fun sampleProduct(
        id: Long,
        name: String,
        isActive: Boolean = true,
        basePrice: Long = 15_000L,
    ): Product = Product(
        id = id,
        sku = "SKU-$id",
        barcode = null,
        name = name,
        category = "Minuman",
        baseUnit = "pcs",
        baseHpp = 8_000L,
        basePrice = basePrice,
        stockBaseQty = 12.0,
        minStockAlert = 5.0,
        isActive = isActive,
    )

    private fun showLoadedCatalog(viewModel: ProductsViewModel, vararg values: Product) {
        products.setProducts(values.toList())
        showScreen(viewModel)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.catalog.successDataOrNull()?.items?.isNotEmpty() == true
        }
    }

    @Test
    fun deleteDialogNamesTheProduct() {
        val viewModel = createViewModel()
        showLoadedCatalog(viewModel, sampleProduct(id = 1L, name = "Kopi Susu"))

        composeRule.runOnIdle { viewModel.onRequestDelete(1L) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(IdStrings.productsDeleteConfirmTitleNamed("Kopi Susu")).assertIsDisplayed()
        composeRule.onNodeWithText(IdStrings.actionDelete).assertIsDisplayed()
    }

    @Test
    fun tappingACardOpensTheEditForm() {
        val viewModel = createViewModel()
        showLoadedCatalog(viewModel, sampleProduct(id = 1L, name = "Kopi Susu"))

        composeRule.onNodeWithText("Kopi Susu").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.form?.productId == 1L
        }

        assertThat(viewModel.state.value.form?.name).isEqualTo("Kopi Susu")
        composeRule.onNodeWithContentDescription(IdStrings.productsBack).assertIsDisplayed()
    }

    @Test
    fun archivedCardShowsTheArchivedLabel() {
        val viewModel = createViewModel()
        showLoadedCatalog(
            viewModel,
            sampleProduct(id = 1L, name = "Kopi Susu"),
            sampleProduct(id = 2L, name = "Teh Manis", isActive = false),
        )

        composeRule.onNodeWithText("Kopi Susu").assertIsDisplayed()
        composeRule.onNodeWithText("Teh Manis").assertIsDisplayed()
        composeRule.onAllNodesWithText(IdStrings.productsArchivedLabel, substring = true).assertCountEquals(1)
    }

    @Test
    fun changingAFieldThenTappingBackAsksBeforeDiscarding() {
        val viewModel = createViewModel()
        showLoadedCatalog(viewModel, sampleProduct(id = 1L, name = "Kopi Susu"))

        composeRule.runOnIdle { viewModel.onOpenEditForm(1L) }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.form != null }
        composeRule.onNode(hasSetTextAction() and hasText("Kopi Susu")).performTextInput("X")

        composeRule.onNodeWithContentDescription(IdStrings.productsBack).performClick()

        composeRule.onNodeWithText(IdStrings.productsDiscardTitle).assertIsDisplayed()
        assertThat(viewModel.state.value.isDiscardConfirmationVisible).isTrue()
    }

    @Test
    fun keepingTheEditsAfterTheDiscardPromptLeavesTheFormOpen() {
        val viewModel = createViewModel()
        showLoadedCatalog(viewModel, sampleProduct(id = 1L, name = "Kopi Susu"))

        composeRule.runOnIdle { viewModel.onOpenEditForm(1L) }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.form != null }
        composeRule.onNode(hasSetTextAction() and hasText("Kopi Susu")).performTextInput("X")
        composeRule.onNodeWithContentDescription(IdStrings.productsBack).performClick()
        composeRule.onNodeWithText(IdStrings.productsDiscardTitle).assertIsDisplayed()

        composeRule.onNodeWithText(IdStrings.productsDiscardKeepEditing).performClick()

        composeRule.onNodeWithText(IdStrings.productsDiscardTitle).assertDoesNotExist()
        val form = viewModel.state.value.form
        assertThat(form).isNotNull()
        assertThat(form?.name).contains("X")
        assertThat(viewModel.state.value.isDiscardConfirmationVisible).isFalse()
    }
}
