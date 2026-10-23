package id.umkm.pos.pro.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.Clock
import id.umkm.pos.pro.core.contract.ProductImageRepository
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.common.sanitizeQuantityInput
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.common.toAppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    private val productImageRepository: ProductImageRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductsUiState())
    val state: StateFlow<ProductsUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<String?>(null)
    private val stockFilter = MutableStateFlow(ProductStockFilter.ALL)
    private val sortOrder = MutableStateFlow(ProductSortOrder.NEWEST)
    private val reloadTrigger = MutableStateFlow(0)
    private var duplicateNameCheckJob: Job? = null
    private var formOpenCounter = 0L

    init {
        observeProducts()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun onCategoryFilterChange(category: String?) {
        val normalizedCategory = category?.trim()?.takeIf { it.isNotEmpty() }
        categoryFilter.value = normalizedCategory
        _state.update { it.copy(categoryFilter = normalizedCategory) }
    }

    fun onStockFilterChange(filter: ProductStockFilter) {
        stockFilter.value = filter
        _state.update { it.copy(stockFilter = filter) }
    }

    fun onSortOrderChange(order: ProductSortOrder) {
        sortOrder.value = order
        _state.update { it.copy(sortOrder = order) }
    }

    fun onRetry() {
        reloadTrigger.update { it + 1 }
    }

    fun onOpenCreateForm() {
        _state.update { it.copy(form = ProductFormState(formInstanceId = nextFormInstanceId())) }
    }

    fun onOpenEditForm(productId: Long) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            if (product == null) {
                _state.update {
                    it.copy(message = UiText.from { strings -> strings.productsNotFoundMessage })
                }
                return@launch
            }
            val unit = productRepository.getUnits(productId).firstOrNull()
            _state.update {
                it.copy(form = product.toForm(unit).copy(formInstanceId = nextFormInstanceId()))
            }
            scheduleDuplicateNameCheck()
        }
    }

    fun onDismissForm() {
        val unsavedImagePath = _state.value.form?.unsavedImagePath
        _state.update { it.copy(form = null, isDiscardConfirmationVisible = false) }
        if (unsavedImagePath == null) return
        viewModelScope.launch { productImageRepository.deleteImage(unsavedImagePath) }
    }

    fun onRequestDismissForm() {
        val form = _state.value.form ?: return
        if (_state.value.isSaving) return
        if (!form.isDirty) {
            onDismissForm()
            return
        }
        _state.update { it.copy(isDiscardConfirmationVisible = true) }
    }

    fun onConfirmDiscardForm() {
        onDismissForm()
    }

    fun onCancelDiscardForm() {
        _state.update { it.copy(isDiscardConfirmationVisible = false) }
    }

    fun onFormFieldChange(field: ProductFormField, value: String) {
        _state.update { current ->
            val form = current.form ?: return@update current
            current.copy(form = form.withField(field, value))
        }
        if (field == ProductFormField.NAME) scheduleDuplicateNameCheck()
    }

    fun onFormIconChange(icon: String?) {
        val normalizedIcon = icon?.takeIf { value -> value.isNotBlank() }
        _state.update { current ->
            val form = current.form ?: return@update current
            current.copy(form = form.copy(icon = normalizedIcon, isDirty = true))
        }
    }

    fun onFormActiveChange(isActive: Boolean) {
        _state.update { current ->
            val form = current.form ?: return@update current
            current.copy(form = form.copy(isActive = isActive, isDirty = true))
        }
    }

    fun onFormAdvancedToggle() {
        _state.update { current ->
            val form = current.form ?: return@update current
            current.copy(form = form.copy(isAdvancedExpanded = !form.isAdvancedExpanded))
        }
    }

    fun onProductImagePicked(sourceUri: String) {
        val form = _state.value.form ?: return
        if (form.isImageImporting) return
        _state.update { current ->
            val currentForm = current.form ?: return@update current
            current.copy(form = currentForm.copy(isImageImporting = true))
        }
        viewModelScope.launch {
            when (val importResult = productImageRepository.importImage(sourceUri)) {
                is AppResult.Success -> applyImportedImage(
                    targetProductId = form.productId,
                    importedImagePath = importResult.value,
                )

                is AppResult.Failure -> _state.update { current ->
                    val currentForm = current.form ?: return@update current
                    current.copy(
                        form = currentForm.copy(isImageImporting = false),
                        message = UiText.from { strings -> strings.productsImageFailed },
                    )
                }
            }
        }
    }

    fun onProductImageRemove() {
        val form = _state.value.form ?: return
        if (form.isImageImporting) return
        val unsavedImagePath = form.unsavedImagePath
        _state.update { current ->
            val currentForm = current.form ?: return@update current
            current.copy(
                form = currentForm.copy(
                    imagePath = null,
                    isImageRemoved = currentForm.persistedImagePath != null,
                    isDirty = true,
                ),
            )
        }
        if (unsavedImagePath == null) return
        viewModelScope.launch { productImageRepository.deleteImage(unsavedImagePath) }
    }

    private suspend fun applyImportedImage(targetProductId: Long?, importedImagePath: String) {
        val form = _state.value.form
        if (form == null || form.productId != targetProductId) {
            productImageRepository.deleteImage(importedImagePath)
            return
        }
        val unsavedImagePath = form.unsavedImagePath
        _state.update { current ->
            val currentForm = current.form ?: return@update current
            current.copy(
                form = currentForm.copy(
                    imagePath = importedImagePath,
                    isImageRemoved = false,
                    isImageImporting = false,
                    isDirty = true,
                ),
            )
        }
        productImageRepository.deleteImage(unsavedImagePath)
    }

    fun onSubmitForm() {
        val form = _state.value.form ?: return
        if (_state.value.isSaving) return
        val errors = validateProductForm(form)
        if (errors.isNotEmpty()) {
            _state.update { current ->
                current.copy(
                    form = current.form?.copy(
                        errors = errors,
                        validationFailures = current.form.validationFailures + 1,
                    ),
                )
            }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val duplicateErrors = findDuplicateFieldErrors(form)
                if (duplicateErrors.isNotEmpty()) {
                    _state.update { current ->
                        val currentForm = current.form ?: return@update current
                        current.copy(
                            isSaving = false,
                            form = currentForm.copy(
                                errors = currentForm.errors + duplicateErrors,
                                validationFailures = currentForm.validationFailures + 1,
                            ),
                        )
                    }
                    return@launch
                }
                saveProduct(form)
                productImageRepository.deleteImage(form.storedImagePathToDiscard)
                _state.update {
                    it.copy(
                        isSaving = false,
                        form = null,
                        isDiscardConfirmationVisible = false,
                        message = UiText.from { strings ->
                            if (form.isEditing) {
                                strings.productsSavedChanges
                            } else {
                                strings.productsSavedNew
                            }
                        },
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        message = UiText.from { strings -> strings.productsSaveFailed },
                    )
                }
            }
        }
    }

    fun onDuplicateProduct(productId: Long) {
        if (_state.value.form != null) return
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            if (product == null) {
                _state.update {
                    it.copy(message = UiText.from { strings -> strings.productsNotFoundMessage })
                }
                return@launch
            }
            val unit = productRepository.getUnits(productId).firstOrNull()
            val duplicatedForm = product.toForm(unit).copy(
                formInstanceId = nextFormInstanceId(),
                productId = null,
                name = nextCopyName(product.name),
                sku = "",
                barcode = "",
                stockQuantity = "0",
                imagePath = null,
                persistedImagePath = null,
                isImageRemoved = false,
                isDirty = true,
            )
            _state.update { it.copy(form = duplicatedForm) }
            scheduleDuplicateNameCheck()
        }
    }

    fun onToggleProductArchive(productId: Long) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId) ?: return@launch
            val updatedProduct = product.copy(isActive = !product.isActive)
            productRepository.upsertProduct(updatedProduct)
            _state.update {
                it.copy(
                    message = UiText.from { strings ->
                        if (updatedProduct.isActive) {
                            strings.productsUnarchivedMessage
                        } else {
                            strings.productsArchivedMessage
                        }
                    },
                )
            }
        }
    }

    fun onDeleteProduct(productId: Long) {
        viewModelScope.launch {
            deleteProductAndAssets(productId)
            _state.update { current ->
                current.copy(
                    form = if (current.form?.productId == productId) null else current.form,
                    isDiscardConfirmationVisible = false,
                    message = UiText.from { strings -> strings.productsDeleted },
                )
            }
        }
    }

    fun onRequestDelete(productId: Long) {
        val productName = _state.value.catalog.successDataOrNull()
            ?.items
            ?.firstOrNull { item -> item.product.id == productId }
            ?.product
            ?.name
        _state.update {
            it.copy(
                pendingDeleteProductId = productId,
                pendingDeleteProductName = productName,
            )
        }
    }

    fun onCancelDelete() {
        _state.update {
            it.copy(
                pendingDeleteProductId = null,
                pendingDeleteProductName = null,
            )
        }
    }

    fun onConfirmDelete() {
        val productId = _state.value.pendingDeleteProductId ?: return
        viewModelScope.launch {
            deleteProductAndAssets(productId)
            _state.update { current ->
                current.copy(
                    pendingDeleteProductId = null,
                    pendingDeleteProductName = null,
                    form = if (current.form?.productId == productId) null else current.form,
                    message = UiText.from { strings -> strings.productsDeleted },
                )
            }
        }
    }

    private suspend fun deleteProductAndAssets(productId: Long) {
        val productImagePath = productRepository.getProductById(productId)?.imagePath
        val openFormImagePath = _state.value.form?.takeIf { form -> form.productId == productId }
            ?.unsavedImagePath
        productRepository.deleteProduct(productId)
        productImageRepository.deleteImage(productImagePath)
        productImageRepository.deleteImage(openFormImagePath)
    }

    private fun scheduleDuplicateNameCheck() {
        duplicateNameCheckJob?.cancel()
        val form = _state.value.form ?: return
        val name = form.name.trim()
        if (name.isEmpty()) {
            _state.update { current ->
                val currentForm = current.form ?: return@update current
                current.copy(form = currentForm.copy(duplicateNameWarning = false))
            }
            return
        }
        duplicateNameCheckJob = viewModelScope.launch {
            delay(DUPLICATE_NAME_CHECK_DELAY_MILLIS)
            val match = productRepository.findProductByName(name, form.productId ?: 0L)
            _state.update { current ->
                val currentForm = current.form ?: return@update current
                if (currentForm.name.trim() != name || currentForm.productId != form.productId) {
                    return@update current
                }
                current.copy(form = currentForm.copy(duplicateNameWarning = match != null))
            }
        }
    }

    private fun nextFormInstanceId(): Long {
        formOpenCounter += 1
        return formOpenCounter
    }

    private suspend fun nextCopyName(originalName: String): String {
        var index = 2
        while (true) {
            val suffix = " $index"
            val candidate = originalName
                .trim()
                .take(PRODUCT_NAME_MAX_LENGTH - suffix.length) + suffix
            if (productRepository.findProductByName(candidate, 0L) == null) return candidate
            index++
        }
    }

    fun onMessageConsumed() {
        _state.update { it.copy(message = null) }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            combine(
                loadedProductsFlow(),
                searchQuery,
                categoryFilter,
                stockFilter,
                sortOrder,
            ) { productsState, query, category, stock, order ->
                productsState.toCatalogState(
                    searchQuery = query,
                    categoryFilter = category,
                    stockFilter = stock,
                    sortOrder = order,
                )
            }
                .collect { catalogState ->
                    _state.update { current -> current.copy(catalog = catalogState) }
                }
        }
    }

    private fun loadedProductsFlow(): Flow<UiState<List<Product>>> =
        reloadTrigger
            .flatMapLatest {
                productRepository.observeProducts(null)
                    .map<List<Product>, UiState<List<Product>>> { products ->
                        if (products.isEmpty()) UiState.Empty else UiState.Success(products)
                    }
                    .catch { error -> emit(UiState.Error(error.toAppError())) }
            }

    private suspend fun findDuplicateFieldErrors(form: ProductFormState): Map<ProductFormField, UiText> {
        val excludedProductId = form.productId ?: 0L
        val errors = mutableMapOf<ProductFormField, UiText>()
        val sku = form.sku.trim()
        if (sku.isNotEmpty() && productRepository.findProductBySku(sku, excludedProductId) != null) {
            errors[ProductFormField.SKU] = UiText.from { strings -> strings.productsDuplicateSku }
        }
        val barcode = form.barcode.trim()
        if (barcode.isNotEmpty() && productRepository.findProductByBarcode(barcode, excludedProductId) != null) {
            errors[ProductFormField.BARCODE] = UiText.from { strings -> strings.productsDuplicateBarcode }
        }
        return errors
    }

    private suspend fun saveProduct(form: ProductFormState): Long {
        val existing = form.productId?.let { productId -> productRepository.getProductById(productId) }
        val product = Product(
            id = form.productId ?: 0L,
            sku = form.sku.trim().ifBlank { existing?.sku ?: "SKU-${clock.nowMillis()}" },
            barcode = form.barcode.trim().ifBlank { null },
            name = form.name.trim(),
            category = form.category.trim().ifBlank { null },
            baseUnit = form.baseUnit.trim(),
            baseHpp = form.baseHpp.trim().toLongOrNull() ?: 0L,
            basePrice = form.basePrice.trim().toLong(),
            stockBaseQty = form.stockQuantity.trim().toDouble(),
            minStockAlert = form.minStockAlert.trim().toDoubleOrNull() ?: 0.0,
            isActive = form.isActive,
            imagePath = form.imagePath,
            icon = form.icon,
        )
        return productRepository.saveProductWithUnit(product, buildVariantUnit(form))
    }

    private suspend fun buildVariantUnit(form: ProductFormState): ProductUnit? {
        if (!form.isAdvancedExpanded || form.variantName.isBlank()) return null
        val variantName = form.variantName.trim()
        val existingUnit = form.productId?.let { productId ->
            productRepository.getUnits(productId)
                .firstOrNull { unit -> unit.variantName.equals(variantName, ignoreCase = true) }
        }
        return ProductUnit(
            id = existingUnit?.id ?: 0L,
            productId = form.productId ?: 0L,
            variantName = variantName,
            conversionFactor = form.conversionFactor.trim().toDouble(),
            customBarcode = existingUnit?.customBarcode,
            unitPrice = form.variantPrice.trim().toLong(),
            wholesaleMinQty = form.wholesaleMinQty.trim().toIntOrNull(),
            wholesalePrice = form.wholesalePrice.trim().toLongOrNull(),
        )
    }
}

private fun UiState<List<Product>>.toCatalogState(
    searchQuery: String,
    categoryFilter: String?,
    stockFilter: ProductStockFilter,
    sortOrder: ProductSortOrder,
): UiState<ProductsCatalog> {
    if (this is UiState.Error) return UiState.Error(error)
    if (this == UiState.Loading) return UiState.Loading
    val products = successDataOrNull() ?: return UiState.Empty
    return UiState.Success(
        buildProductsCatalog(
            productItems = products.map { product ->
                ProductListItem(product = product, stockStatus = resolveStockStatus(product))
            },
            categoryFilter = categoryFilter,
            sortOrder = sortOrder,
            stockFilter = stockFilter,
            searchQuery = searchQuery,
        ),
    )
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
    imagePath = imagePath,
    icon = icon,
    persistedPricing = ProductPricingSnapshot(
        baseHpp = baseHpp,
        basePrice = basePrice,
        unitPrice = unit?.unitPrice,
        wholesalePrice = unit?.wholesalePrice,
    ),
    persistedImagePath = imagePath,
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
    return updated.copy(errors = updated.errors - field, isDirty = true)
}

private fun String.digitsOnly(): String = filter { character -> character.isDigit() }

private const val DUPLICATE_NAME_CHECK_DELAY_MILLIS = 250L
