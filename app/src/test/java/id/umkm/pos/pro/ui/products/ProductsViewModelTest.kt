package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.domain.usecase.FakeClock
import id.umkm.pos.pro.ui.FakeCatalogRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.uiSampleProduct
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProductsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock(currentMillis = 10_000L)
    private val products = FakeCatalogRepository()

    private fun createViewModel(): ProductsViewModel = ProductsViewModel(
        productRepository = products,
        clock = clock,
    )

    private fun itemsOf(state: ProductsUiState): List<ProductListItem> =
        state.products.successDataOrNull().orEmpty()

    @Test
    fun loadingAnEmptyCatalogReportsEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        assertThat(viewModel.state.value.products).isEqualTo(UiState.Empty)
    }

    @Test
    fun searchQueryFiltersTheCatalog() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu"),
                uiSampleProduct(id = 2L, name = "Teh Manis"),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Teh")

        val items = itemsOf(viewModel.state.value)
        assertThat(items.map { it.product.name }).containsExactly("Teh Manis")
        assertThat(viewModel.state.value.searchQuery).isEqualTo("Teh")
    }

    @Test
    fun savingWithoutRequiredFieldsReportsInlineErrors() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onOpenCreateForm()
        viewModel.onSubmitForm()

        val errors = viewModel.state.value.form?.errors.orEmpty()
        assertThat(errors).containsKey(ProductFormField.NAME)
        assertThat(errors).containsKey(ProductFormField.BASE_PRICE)
        assertThat(errors).containsKey(ProductFormField.BASE_HPP)
        assertThat(products.currentProducts()).isEmpty()
    }

    @Test
    fun savingAValidProductPersistsItAndClosesTheForm() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        viewModel.onFormFieldChange(ProductFormField.NAME, "Kopi Susu")
        viewModel.onFormFieldChange(ProductFormField.BASE_PRICE, "18000")
        viewModel.onFormFieldChange(ProductFormField.BASE_HPP, "9000")
        viewModel.onFormFieldChange(ProductFormField.STOCK_QUANTITY, "12")
        viewModel.onFormFieldChange(ProductFormField.MIN_STOCK_ALERT, "3")

        viewModel.onSubmitForm()

        val saved = products.currentProducts().single()
        assertThat(saved.name).isEqualTo("Kopi Susu")
        assertThat(saved.basePrice).isEqualTo(18_000L)
        assertThat(saved.baseHpp).isEqualTo(9_000L)
        assertThat(saved.stockBaseQty).isEqualTo(12.0)
        assertThat(saved.sku).isEqualTo("SKU-10000")
        assertThat(viewModel.state.value.form).isNull()
        assertThat(viewModel.state.value.message).isEqualTo("Produk disimpan")
    }

    @Test
    fun stockFilterNarrowsTheCatalogToTheMatchingStatus() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu", stockBaseQty = 100.0, minStockAlert = 5.0),
                uiSampleProduct(id = 2L, name = "Teh Manis", stockBaseQty = 2.0, minStockAlert = 5.0),
                uiSampleProduct(id = 3L, name = "Gula Pasir", stockBaseQty = 0.0, minStockAlert = 5.0),
                uiSampleProduct(id = 4L, name = "Sabun Cuci", stockBaseQty = -1.0, minStockAlert = 5.0),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onStockFilterChange(ProductStockFilter.IN_STOCK)
        assertThat(itemsOf(viewModel.state.value).map { it.product.name }).containsExactly("Kopi Susu")

        viewModel.onStockFilterChange(ProductStockFilter.LOW_STOCK)
        assertThat(itemsOf(viewModel.state.value).map { it.product.name }).containsExactly("Teh Manis")

        viewModel.onStockFilterChange(ProductStockFilter.OUT_OF_STOCK)
        assertThat(itemsOf(viewModel.state.value).map { it.product.name }).containsExactly("Gula Pasir", "Sabun Cuci")

        viewModel.onStockFilterChange(ProductStockFilter.ALL)
        assertThat(itemsOf(viewModel.state.value)).hasSize(4)
        assertThat(viewModel.state.value.stockFilter).isEqualTo(ProductStockFilter.ALL)
    }

    @Test
    fun stockFilterWithoutMatchesReportsEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(listOf(uiSampleProduct(id = 1L, name = "Kopi Susu", stockBaseQty = 100.0)))
        val viewModel = createViewModel()

        viewModel.onStockFilterChange(ProductStockFilter.OUT_OF_STOCK)

        assertThat(viewModel.state.value.products).isEqualTo(UiState.Empty)
    }

    @Test
    fun savingWithAUnitAlsoPersistsTheVariantUnit() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        viewModel.onFormFieldChange(ProductFormField.NAME, "Kopi Susu")
        viewModel.onFormFieldChange(ProductFormField.BASE_PRICE, "18000")
        viewModel.onFormFieldChange(ProductFormField.BASE_HPP, "9000")
        viewModel.onFormAdvancedToggle()
        viewModel.onFormFieldChange(ProductFormField.VARIANT_NAME, "Dus")
        viewModel.onFormFieldChange(ProductFormField.CONVERSION_FACTOR, "40")
        viewModel.onFormFieldChange(ProductFormField.VARIANT_PRICE, "78000")
        viewModel.onFormFieldChange(ProductFormField.WHOLESALE_MIN_QTY, "5")
        viewModel.onFormFieldChange(ProductFormField.WHOLESALE_PRICE, "75000")

        viewModel.onSubmitForm()

        val savedProduct = products.currentProducts().single()
        val savedUnit = products.unitsFor(savedProduct.id).single()
        assertThat(savedUnit.variantName).isEqualTo("Dus")
        assertThat(savedUnit.conversionFactor).isEqualTo(40.0)
        assertThat(savedUnit.wholesaleMinQty).isEqualTo(5)
        assertThat(savedUnit.wholesalePrice).isEqualTo(75_000L)
    }

    @Test
    fun editingPrefillsTheFormAndTheFirstVariantUnit() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(listOf(uiSampleProduct(id = 1L, name = "Kopi Susu", basePrice = 18_000L)))
        products.setUnits(
            productId = 1L,
            values = listOf(
                ProductUnit(
                    id = 10L,
                    productId = 1L,
                    variantName = "Dus",
                    conversionFactor = 40.0,
                    customBarcode = null,
                    unitPrice = 78_000L,
                    wholesaleMinQty = 5,
                    wholesalePrice = 75_000L,
                ),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onOpenEditForm(1L)

        val form = viewModel.state.value.form
        assertThat(form).isNotNull()
        assertThat(form?.isEditing).isTrue()
        assertThat(form?.name).isEqualTo("Kopi Susu")
        assertThat(form?.basePrice).isEqualTo("18000")
        assertThat(form?.isAdvancedExpanded).isTrue()
        assertThat(form?.variantName).isEqualTo("Dus")
        assertThat(form?.conversionFactor).isEqualTo("40")
        assertThat(form?.wholesaleMinQty).isEqualTo("5")
    }

    @Test
    fun editingAnUnknownProductReportsAMessage() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onOpenEditForm(404L)

        assertThat(viewModel.state.value.form).isNull()
        assertThat(viewModel.state.value.message).isEqualTo("Produk tidak ditemukan")
    }

    @Test
    fun deletingAProductRemovesItFromTheCatalog() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(listOf(uiSampleProduct(id = 1L, name = "Kopi Susu")))
        val viewModel = createViewModel()

        viewModel.onRequestDelete(1L)
        assertThat(viewModel.state.value.pendingDeleteProductId).isEqualTo(1L)

        viewModel.onConfirmDelete()

        assertThat(products.currentProducts()).isEmpty()
        assertThat(viewModel.state.value.pendingDeleteProductId).isNull()
        assertThat(viewModel.state.value.message).isEqualTo("Produk dihapus")
    }

    @Test
    fun dismissingTheFormLeavesTheCatalogUntouched() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()

        viewModel.onDismissForm()

        assertThat(viewModel.state.value.form).isNull()
        assertThat(products.currentProducts()).isEmpty()
    }
}
