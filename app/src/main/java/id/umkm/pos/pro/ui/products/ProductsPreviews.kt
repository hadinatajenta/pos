package id.umkm.pos.pro.ui.products

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.ui.common.PreviewAppContainerProvider
import id.umkm.pos.pro.ui.common.resolveStockStatus
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

private fun previewProduct(
    id: Long,
    name: String,
    price: Long,
    stock: Double,
    category: String?,
): Product = Product(
    id = id,
    sku = "SKU-$id",
    barcode = null,
    name = name,
    category = category,
    baseUnit = "pcs",
    baseHpp = price / 2,
    basePrice = price,
    stockBaseQty = stock,
    minStockAlert = 5.0,
    isActive = true,
)

private fun previewProductsState(categoryFilter: String? = null): ProductsUiState {
    val products = listOf(
        previewProduct(id = 1L, name = "Kopi Susu Gula Aren", price = 18_000L, stock = 24.0, category = "Minuman"),
        previewProduct(id = 2L, name = "Roti Cokelat", price = 12_000L, stock = 2.0, category = "Makanan"),
        previewProduct(id = 3L, name = "Air Mineral 600ml", price = 4_000L, stock = 0.0, category = "Minuman"),
        previewProduct(id = 4L, name = "Gula Pasir 1kg", price = 1_250_000L, stock = -3.0, category = "Bahan"),
        previewProduct(id = 5L, name = "Sabun Cuci Piring 1L", price = 12_500L, stock = 48.0, category = null),
    )
    val productItems = products.map { product ->
        ProductListItem(product = product, stockStatus = resolveStockStatus(product))
    }
    return ProductsUiState(
        searchQuery = "",
        categoryFilter = categoryFilter,
        catalog = UiState.Success(
            buildProductsCatalog(
                productItems = productItems,
                categoryFilter = categoryFilter,
                sortOrder = ProductSortOrder.NEWEST,
            ),
        ),
        form = null,
    )
}

private fun previewProductsActions(): ProductsActions = ProductsActions(
    onSearchQueryChange = {},
    onCategoryFilterChange = {},
    onSortOrderChange = {},
    onRetry = {},
    onOpenCreateForm = {},
    onOpenEditForm = {},
    onDismissForm = {},
    onFormFieldChange = { _, _ -> },
    onFormActiveChange = {},
    onFormAdvancedToggle = {},
    onProductImagePicked = {},
    onProductImageRemove = {},
    onSubmitForm = {},
    onRequestDelete = {},
    onCancelDelete = {},
    onConfirmDelete = {},
    onMessageConsumed = {},
)

@Preview(name = "Products compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProductsCompactPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = previewProductsState(),
                actions = previewProductsActions(),
            )
        }
    }
}

@Preview(name = "Products filtered", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProductsFilteredPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = previewProductsState(categoryFilter = "Minuman"),
                actions = previewProductsActions(),
            )
        }
    }
}

@Preview(name = "Products empty", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProductsEmptyPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = ProductsUiState(catalog = UiState.Empty),
                actions = previewProductsActions(),
            )
        }
    }
}

@Preview(name = "Products search empty", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProductsSearchEmptyPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = ProductsUiState(searchQuery = "teh", catalog = UiState.Empty),
                actions = previewProductsActions(),
            )
        }
    }
}

@Preview(name = "Products form", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProductsFormPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = previewProductsState().copy(form = ProductFormState(name = "Kopi Susu", basePrice = "18000")),
                actions = previewProductsActions(),
            )
        }
    }
}

@Preview(name = "Products medium tablet", widthDp = 1000, heightDp = 720, showBackground = true)
@Composable
private fun ProductsMediumTabletPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = previewProductsState(),
                actions = previewProductsActions(),
            )
        }
    }
}

@Preview(name = "Products expanded tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun ProductsExpandedTabletPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ProductsScreen(
                state = previewProductsState().copy(form = ProductFormState(name = "Kopi Susu", basePrice = "18000")),
                actions = previewProductsActions(),
            )
        }
    }
}
