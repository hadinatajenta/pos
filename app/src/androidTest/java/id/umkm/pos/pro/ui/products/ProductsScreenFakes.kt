package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ProductImageRepository
import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class ScreenTestProductRepository : ProductRepository {

    private val products = MutableStateFlow<List<Product>>(emptyList())
    private val units = mutableMapOf<Long, List<ProductUnit>>()

    fun setProducts(values: List<Product>) {
        products.value = values
    }

    fun currentProducts(): List<Product> = products.value

    override fun observeProducts(query: String?): Flow<List<Product>> = products.map { list ->
        val trimmed = query?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            list
        } else {
            list.filter { product -> product.name.contains(trimmed, ignoreCase = true) }
        }
    }

    override suspend fun getProductById(productId: Long): Product? =
        products.value.firstOrNull { product -> product.id == productId }

    override suspend fun findProductByBarcode(barcode: String): Product? =
        products.value.firstOrNull { product -> product.barcode == barcode }

    override suspend fun findProductBySku(sku: String, excludeId: Long): Product? =
        products.value.firstOrNull { product -> product.sku == sku && product.id != excludeId }

    override suspend fun findProductByBarcode(barcode: String, excludeId: Long): Product? =
        products.value.firstOrNull { product -> product.barcode == barcode && product.id != excludeId }

    override suspend fun findProductByName(name: String, excludeId: Long): Product? =
        products.value.firstOrNull { product ->
            product.name.trim() == name.trim() && product.id != excludeId
        }

    override suspend fun saveProductWithUnit(product: Product, unit: ProductUnit?): Long {
        val saved = product.copy(id = product.id.takeIf { it != 0L } ?: nextId())
        products.value = products.value.filterNot { existing -> existing.id == saved.id } + saved
        unit?.let { savedUnit ->
            units[saved.id] = units[saved.id].orEmpty().filterNot { it.id == savedUnit.id } +
                savedUnit.copy(productId = saved.id)
        }
        return saved.id
    }

    override suspend fun upsertProduct(product: Product): Long {
        val saved = product.copy(id = product.id.takeIf { it != 0L } ?: nextId())
        products.value = products.value.filterNot { existing -> existing.id == saved.id } + saved
        return saved.id
    }

    override suspend fun deleteProduct(productId: Long) {
        products.value = products.value.filterNot { product -> product.id == productId }
    }

    override suspend fun getUnits(productId: Long): List<ProductUnit> = units[productId].orEmpty()

    override suspend fun findUnitByBarcode(barcode: String): ProductUnit? =
        units.values.flatten().firstOrNull { unit -> unit.customBarcode == barcode }

    override suspend fun upsertUnit(unit: ProductUnit): Long = unit.id

    override suspend fun deleteUnit(unitId: Long) = Unit

    private fun nextId(): Long = (products.value.maxOfOrNull { product -> product.id } ?: 0L) + 1L
}

internal class ScreenTestProductImageRepository : ProductImageRepository {

    override suspend fun importImage(sourceUri: String): AppResult<String> = AppResult.Success(sourceUri)

    override suspend fun deleteImage(imagePath: String?) = Unit
}
