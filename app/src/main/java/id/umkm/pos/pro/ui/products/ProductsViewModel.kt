package id.umkm.pos.pro.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.mapList
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.common.sanitizeQuantityInput
import id.umkm.pos.pro.ui.common.toAppError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModel(
    private val productRepository: ProductRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductsUiState())
    val state: StateFlow<ProductsUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val reloadTrigger = MutableStateFlow(0)

    init {
        observeProducts()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun onRetry() {
        reloadTrigger.update { it + 1 }
    }

    fun onOpenCreateForm() {
        _state.update { it.copy(form = ProductFormState()) }
    }

    fun onOpenEditForm(productId: Long) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            if (product == null) {
                _state.update { it.copy(message = "Product was not found") }
                return@launch
            }
            val unit = productRepository.getUnits(productId).firstOrNull()
            _state.update { it.copy(form = product.toForm(unit)) }
        }
    }

    fun onDismissForm() {
        _state.update { it.copy(form = null) }
    }

    fun onFormFieldChange(field: ProductFormField, value: String) {
        _state.update { current ->
            val form = current.form ?: return@update current
            current.copy(form = form.withField(field, value))
        }
    }

    fun onFormActiveChange(isActive: Boolean) {
        _state.update { current ->
            val form = current.form ?: return@update current
            current.copy(form = form.copy(isActive = isActive))
        }
    }

    fun onFormAdvancedToggle() {
        _state.update { current ->
            val form = current.form ?: return@update current
            current.copy(form = form.copy(isAdvancedExpanded = !form.isAdvancedExpanded))
        }
    }

    fun onSubmitForm() {
        val form = _state.value.form ?: return
        if (_state.value.isSaving) return
        val errors = validateProductForm(form)
        if (errors.isNotEmpty()) {
            _state.update { current -> current.copy(form = current.form?.copy(errors = errors)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val productId = saveProduct(form)
            if (productId == null) {
                _state.update { it.copy(isSaving = false, message = "Could not save the product.") }
                return@launch
            }
            saveVariantUnit(form = form, productId = productId)
            _state.update { it.copy(isSaving = false, form = null, message = "Product saved") }
        }
    }

    fun onRequestDelete(productId: Long) {
        _state.update { it.copy(pendingDeleteProductId = productId) }
    }

    fun onCancelDelete() {
        _state.update { it.copy(pendingDeleteProductId = null) }
    }

    fun onConfirmDelete() {
        val productId = _state.value.pendingDeleteProductId ?: return
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
            _state.update { current ->
                current.copy(
                    pendingDeleteProductId = null,
                    form = if (current.form?.productId == productId) null else current.form,
                    message = "Product deleted",
                )
            }
        }
    }

    fun onMessageConsumed() {
        _state.update { it.copy(message = null) }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            combine(searchQuery, reloadTrigger) { query, _ -> query }
                .flatMapLatest { query ->
                    productRepository.observeProducts(query.ifBlank { null })
                        .map<List<Product>, UiState<List<Product>>> { products ->
                            if (products.isEmpty()) UiState.Empty else UiState.Success(products)
                        }
                        .catch { error -> emit(UiState.Error(error.toAppError())) }
                }
                .collect { listState ->
                    _state.update { current ->
                        current.copy(
                            products = listState.mapList { product ->
                                ProductListItem(product = product, stockStatus = resolveStockStatus(product))
                            },
                        )
                    }
                }
        }
    }

    private suspend fun saveProduct(form: ProductFormState): Long? {
        val existing = form.productId?.let { productId -> productRepository.getProductById(productId) }
        val product = Product(
            id = form.productId ?: 0L,
            sku = form.sku.trim().ifBlank { existing?.sku ?: "SKU-${clock.nowMillis()}" },
            barcode = form.barcode.trim().ifBlank { null },
            name = form.name.trim(),
            category = form.category.trim().ifBlank { null },
            baseUnit = form.baseUnit.trim(),
            baseHpp = form.baseHpp.trim().toLong(),
            basePrice = form.basePrice.trim().toLong(),
            stockBaseQty = form.stockQuantity.trim().toDouble(),
            minStockAlert = form.minStockAlert.trim().toDouble(),
            isActive = form.isActive,
        )
        return productRepository.upsertProduct(product)
    }

    private suspend fun saveVariantUnit(form: ProductFormState, productId: Long) {
        if (!form.isAdvancedExpanded || form.variantName.isBlank()) return
        val variantName = form.variantName.trim()
        val existingUnit = productRepository.getUnits(productId)
            .firstOrNull { unit -> unit.variantName.equals(variantName, ignoreCase = true) }
        productRepository.upsertUnit(
            ProductUnit(
                id = existingUnit?.id ?: 0L,
                productId = productId,
                variantName = variantName,
                conversionFactor = form.conversionFactor.trim().toDouble(),
                customBarcode = existingUnit?.customBarcode,
                unitPrice = form.variantPrice.trim().toLong(),
                wholesaleMinQty = form.wholesaleMinQty.trim().toIntOrNull(),
                wholesalePrice = form.wholesalePrice.trim().toLongOrNull(),
            ),
        )
    }
}

private fun Product.toForm(unit: ProductUnit?): ProductFormState = ProductFormState(
    productId = id,
    name = name,
    sku = sku,
    barcode = barcode.orEmpty(),
    category = category.orEmpty(),
    baseUnit = baseUnit,
    basePrice = basePrice.toString(),
    baseHpp = baseHpp.toString(),
    stockQuantity = formatQuantity(stockBaseQty),
    minStockAlert = formatQuantity(minStockAlert),
    isActive = isActive,
    isAdvancedExpanded = unit != null,
    variantName = unit?.variantName.orEmpty(),
    conversionFactor = unit?.let { formatQuantity(it.conversionFactor) }.orEmpty(),
    variantPrice = unit?.unitPrice?.toString().orEmpty(),
    wholesaleMinQty = unit?.wholesaleMinQty?.toString().orEmpty(),
    wholesalePrice = unit?.wholesalePrice?.toString().orEmpty(),
)

private fun ProductFormState.withField(field: ProductFormField, value: String): ProductFormState {
    val updated = when (field) {
        ProductFormField.NAME -> copy(name = value)
        ProductFormField.SKU -> copy(sku = value)
        ProductFormField.BARCODE -> copy(barcode = value)
        ProductFormField.CATEGORY -> copy(category = value)
        ProductFormField.BASE_UNIT -> copy(baseUnit = value)
        ProductFormField.BASE_PRICE -> copy(basePrice = value.digitsOnly())
        ProductFormField.BASE_HPP -> copy(baseHpp = value.digitsOnly())
        ProductFormField.STOCK_QUANTITY -> copy(stockQuantity = sanitizeQuantityInput(value))
        ProductFormField.MIN_STOCK_ALERT -> copy(minStockAlert = sanitizeQuantityInput(value))
        ProductFormField.VARIANT_NAME -> copy(variantName = value)
        ProductFormField.CONVERSION_FACTOR -> copy(conversionFactor = sanitizeQuantityInput(value))
        ProductFormField.VARIANT_PRICE -> copy(variantPrice = value.digitsOnly())
        ProductFormField.WHOLESALE_MIN_QTY -> copy(wholesaleMinQty = value.digitsOnly())
        ProductFormField.WHOLESALE_PRICE -> copy(wholesalePrice = value.digitsOnly())
    }
    return updated.copy(errors = updated.errors - field)
}

private fun String.digitsOnly(): String = filter { character -> character.isDigit() }
