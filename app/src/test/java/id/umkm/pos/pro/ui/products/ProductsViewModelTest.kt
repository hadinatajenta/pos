package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.domain.usecase.FakeClock
import id.umkm.pos.pro.ui.FakeCatalogRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.IdStrings
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
    private val images = FakeProductImageRepository()

    private fun createViewModel(): ProductsViewModel = ProductsViewModel(
        productRepository = products,
        productImageRepository = images,
        clock = clock,
    )

    private fun itemsOf(state: ProductsUiState): List<ProductListItem> =
        state.catalog.successDataOrNull()?.items.orEmpty()

    private fun categoriesOf(state: ProductsUiState): List<String> =
        state.catalog.successDataOrNull()?.categories.orEmpty()

    private fun runPendingCoroutines() {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun loadingAnEmptyCatalogReportsEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        runPendingCoroutines()

        assertThat(viewModel.state.value.catalog).isEqualTo(UiState.Empty)
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
        runPendingCoroutines()

        val items = itemsOf(viewModel.state.value)
        assertThat(items.map { item -> item.product.name }).containsExactly("Teh Manis")
        assertThat(viewModel.state.value.searchQuery).isEqualTo("Teh")
    }

    @Test
    fun catalogWithoutSearchMatchesReportsEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(listOf(uiSampleProduct(id = 1L, name = "Kopi Susu")))
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Teh")
        runPendingCoroutines()

        assertThat(viewModel.state.value.catalog).isEqualTo(UiState.Empty)
        assertThat(viewModel.state.value.searchQuery).isEqualTo("Teh")
    }

    @Test
    fun categoryFilterNarrowsTheCatalogToTheSelectedCategory() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu").copy(category = "Minuman"),
                uiSampleProduct(id = 2L, name = "Roti Cokelat").copy(category = "Makanan"),
                uiSampleProduct(id = 3L, name = "Air Mineral").copy(category = "Minuman"),
                uiSampleProduct(id = 4L, name = "Sabun Cuci").copy(category = null),
            ),
        )
        val viewModel = createViewModel()

        runPendingCoroutines()

        assertThat(categoriesOf(viewModel.state.value)).containsExactly("Makanan", "Minuman").inOrder()

        viewModel.onCategoryFilterChange("Minuman")
        runPendingCoroutines()
        assertThat(itemsOf(viewModel.state.value).map { item -> item.product.name })
            .containsExactly("Kopi Susu", "Air Mineral")

        viewModel.onCategoryFilterChange(null)
        runPendingCoroutines()
        assertThat(itemsOf(viewModel.state.value)).hasSize(4)
        assertThat(viewModel.state.value.categoryFilter).isNull()
    }

    @Test
    fun categoryFilterWithoutMatchesKeepsTheCatalogVisible() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(listOf(uiSampleProduct(id = 1L, name = "Kopi Susu").copy(category = "Minuman")))
        val viewModel = createViewModel()

        viewModel.onCategoryFilterChange("Peralatan")
        runPendingCoroutines()

        assertThat(itemsOf(viewModel.state.value)).isEmpty()
        assertThat(viewModel.state.value.catalog.successDataOrNull()).isNotNull()
        assertThat(categoriesOf(viewModel.state.value)).containsExactly("Minuman")
        assertThat(viewModel.state.value.categoryFilter).isEqualTo("Peralatan")
    }

    @Test
    fun sortOrderOrdersTheVisibleProducts() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 5L, name = "Teh Manis", stockBaseQty = 18.0),
                uiSampleProduct(id = 1L, name = "Air Mineral", stockBaseQty = 2.0),
                uiSampleProduct(id = 9L, name = "Kopi Susu", stockBaseQty = 40.0),
            ),
        )
        val viewModel = createViewModel()

        runPendingCoroutines()

        assertThat(itemsOf(viewModel.state.value).map { item -> item.product.name })
            .containsExactly("Kopi Susu", "Teh Manis", "Air Mineral")
            .inOrder()

        viewModel.onSortOrderChange(ProductSortOrder.NAME_ASC)
        runPendingCoroutines()
        assertThat(itemsOf(viewModel.state.value).map { item -> item.product.name })
            .containsExactly("Air Mineral", "Kopi Susu", "Teh Manis")
            .inOrder()

        viewModel.onSortOrderChange(ProductSortOrder.LOWEST_STOCK)
        runPendingCoroutines()
        assertThat(itemsOf(viewModel.state.value).map { item -> item.product.stockBaseQty })
            .containsExactly(2.0, 18.0, 40.0)
            .inOrder()

        assertThat(viewModel.state.value.sortOrder).isEqualTo(ProductSortOrder.LOWEST_STOCK)
    }

    @Test
    fun catalogStatsCountStockAlertsFromTheLoadedProducts() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu", stockBaseQty = 100.0, minStockAlert = 5.0)
                    .copy(category = "Minuman"),
                uiSampleProduct(id = 2L, name = "Teh Manis", stockBaseQty = 2.0, minStockAlert = 5.0)
                    .copy(category = "Minuman"),
                uiSampleProduct(id = 3L, name = "Gula Pasir", stockBaseQty = 0.0, minStockAlert = 5.0)
                    .copy(category = "Bahan"),
                uiSampleProduct(id = 4L, name = "Sabun Cuci", stockBaseQty = -1.0, minStockAlert = 5.0)
                    .copy(category = null),
            ),
        )
        val viewModel = createViewModel()

        runPendingCoroutines()

        val stats = requireNotNull(viewModel.state.value.catalog.successDataOrNull()).stats

        assertThat(stats.totalCount).isEqualTo(4)
        assertThat(stats.categoryCount).isEqualTo(2)
        assertThat(stats.lowStockCount).isEqualTo(1)
        assertThat(stats.outOfStockCount).isEqualTo(2)
        assertThat(stats.uncategorizedCount).isEqualTo(1)
    }

    @Test
    fun stockFilterNarrowsTheCatalogToLowStockAndOutOfStockProducts() = runTest(mainDispatcherRule.testDispatcher) {
        products.setProducts(
            listOf(
                uiSampleProduct(id = 1L, name = "Kopi Susu", stockBaseQty = 100.0, minStockAlert = 5.0),
                uiSampleProduct(id = 2L, name = "Teh Manis", stockBaseQty = 2.0, minStockAlert = 5.0),
                uiSampleProduct(id = 3L, name = "Gula Pasir", stockBaseQty = 0.0, minStockAlert = 5.0),
                uiSampleProduct(id = 4L, name = "Sabun Cuci", stockBaseQty = -1.0, minStockAlert = 5.0),
            ),
        )
        val viewModel = createViewModel()

        runPendingCoroutines()

        viewModel.onStockFilterChange(ProductStockFilter.LOW_STOCK)
        runPendingCoroutines()
        assertThat(viewModel.state.value.stockFilter).isEqualTo(ProductStockFilter.LOW_STOCK)
        assertThat(itemsOf(viewModel.state.value).map { item -> item.product.name })
            .containsExactly("Teh Manis")
        assertThat(viewModel.state.value.catalog.successDataOrNull()?.stats?.totalCount).isEqualTo(4)

        viewModel.onStockFilterChange(ProductStockFilter.OUT_OF_STOCK)
        runPendingCoroutines()
        assertThat(viewModel.state.value.stockFilter).isEqualTo(ProductStockFilter.OUT_OF_STOCK)
        assertThat(itemsOf(viewModel.state.value).map { item -> item.product.name })
            .containsExactly("Sabun Cuci", "Gula Pasir")
            .inOrder()

        viewModel.onStockFilterChange(ProductStockFilter.ALL)
        runPendingCoroutines()
        assertThat(viewModel.state.value.stockFilter).isEqualTo(ProductStockFilter.ALL)
        assertThat(itemsOf(viewModel.state.value)).hasSize(4)
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
    fun failingValidationSignalsTheFormToFocusTheFirstError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        assertThat(viewModel.state.value.form?.validationFailures).isEqualTo(0L)

        viewModel.onSubmitForm()
        assertThat(viewModel.state.value.form?.validationFailures).isEqualTo(1L)

        viewModel.onSubmitForm()
        assertThat(viewModel.state.value.form?.validationFailures).isEqualTo(2L)
    }

    @Test
    fun anEmptyLowStockThresholdIsSavedAsZero() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        viewModel.onFormFieldChange(ProductFormField.NAME, "Kopi Susu")
        viewModel.onFormFieldChange(ProductFormField.BASE_PRICE, "18000")
        viewModel.onFormFieldChange(ProductFormField.BASE_HPP, "9000")
        viewModel.onFormFieldChange(ProductFormField.STOCK_QUANTITY, "12")
        viewModel.onFormFieldChange(ProductFormField.MIN_STOCK_ALERT, "")

        viewModel.onSubmitForm()

        val saved = products.currentProducts().single()
        assertThat(saved.minStockAlert).isEqualTo(0.0)
    }

    @Test
    fun correctingAFieldClearsOnlyItsError() = runTest(mainDispatcherRule.testDispatcher) {        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        viewModel.onSubmitForm()
        val reportedCount = viewModel.state.value.form?.errors.orEmpty().size

        viewModel.onFormFieldChange(ProductFormField.NAME, "Kopi Susu")

        val errors = viewModel.state.value.form?.errors.orEmpty()
        assertThat(errors).doesNotContainKey(ProductFormField.NAME)
        assertThat(errors).hasSize(reportedCount - 1)
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
        assertThat(viewModel.state.value.message?.resolve(IdStrings)).isEqualTo("Produk disimpan")
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
        assertThat(viewModel.state.value.message?.resolve(IdStrings)).isEqualTo("Produk tidak ditemukan")
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
        assertThat(viewModel.state.value.message?.resolve(IdStrings)).isEqualTo("Produk dihapus")
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
