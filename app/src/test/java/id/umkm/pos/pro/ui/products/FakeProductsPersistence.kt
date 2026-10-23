package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal data class SavedProduct(
    val product: Product,
    val unit: ProductUnit?,
)

internal class FakeProductsPersistence : ProductRepository {

    private val productsFlow = MutableStateFlow<List<Product>>(emptyList())

    var products: List<Product>
        get() = productsFlow.value
        set(value) {
            productsFlow.value = value
        }

    var units: List<ProductUnit> = emptyList()
    var saveFailure: Throwable? = null

    val saveCalls = mutableListOf<SavedProduct>()
    var upsertProductCallCount = 0
    var upsertUnitCallCount = 0

    override fun observeProducts(query: String?): Flow<List<Product>> = productsFlow

    override suspend fun getProductById(productId: Long): Product? =
        products.firstOrNull { product -> product.id == productId }

    override suspend fun findProductByBarcode(barcode: String): Product? =
        products.firstOrNull { product -> product.barcode == barcode }

    override suspend fun findProductBySku(sku: String, excludeId: Long): Product? =
        products.firstOrNull { product -> product.sku == sku && product.id != excludeId }

    override suspend fun findProductByBarcode(barcode: String, excludeId: Long): Product? =
        products.firstOrNull { product -> product.barcode == barcode && product.id != excludeId }

    override suspend fun findProductByName(name: String, excludeId: Long): Product? =
        products.firstOrNull { product ->
            product.name.trim() == name.trim() && product.id != excludeId
        }

    override suspend fun saveProductWithUnit(product: Product, unit: ProductUnit?): Long {
        saveFailure?.let { failure -> throw failure }
        val productId = product.id.takeIf { id -> id != 0L } ?: nextProductId()
        val savedProduct = product.copy(id = productId)
        val savedUnit = unit?.copy(productId = productId)
        saveCalls += SavedProduct(product = savedProduct, unit = savedUnit)
        storeProduct(savedProduct)
        if (savedUnit != null) storeUnit(savedUnit)
        return productId
    }

    override suspend fun upsertProduct(product: Product): Long {
        upsertProductCallCount++
        val productId = product.id.takeIf { id -> id != 0L } ?: nextProductId()
        storeProduct(product.copy(id = productId))
        return productId
    }

    override suspend fun deleteProduct(productId: Long) {
        products = products.filterNot { product -> product.id == productId }
        units = units.filterNot { unit -> unit.productId == productId }
    }

    override suspend fun getUnits(productId: Long): List<ProductUnit> =
        units.filter { unit -> unit.productId == productId }

    override suspend fun findUnitByBarcode(barcode: String): ProductUnit? =
        units.firstOrNull { unit -> unit.customBarcode == barcode }

    override suspend fun upsertUnit(unit: ProductUnit): Long {
        upsertUnitCallCount++
        storeUnit(unit)
        return unit.id
    }

    override suspend fun deleteUnit(unitId: Long) {
        units = units.filterNot { unit -> unit.id == unitId }
    }

    private fun storeProduct(product: Product) {
        products = products.filterNot { existing -> existing.id == product.id } + product
    }

    private fun storeUnit(unit: ProductUnit) {
        units = units.filterNot { existing ->
            existing.id == unit.id && unit.id != 0L
        } + unit
    }

    private fun nextProductId(): Long = (products.maxOfOrNull { product -> product.id } ?: 0L) + 1L
}
