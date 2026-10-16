package id.umkm.pos.pro.data.local.repository

import id.umkm.pos.pro.core.contract.ProductRepository
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.data.local.dao.ProductDao
import id.umkm.pos.pro.data.local.mapper.ProductMapper
import id.umkm.pos.pro.data.local.util.toContainsLikePattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(
    private val productDao: ProductDao,
) : ProductRepository {

    override fun observeProducts(query: String?): Flow<List<Product>> =
        productDao.observeProducts(query.toContainsLikePattern())
            .map { entities -> entities.map(ProductMapper::toModel) }

    override suspend fun getProductById(productId: Long): Product? =
        productDao.getProductById(productId)?.let(ProductMapper::toModel)

    override suspend fun findProductByBarcode(barcode: String): Product? =
        productDao.findProductByBarcode(barcode)?.let(ProductMapper::toModel)

    override suspend fun upsertProduct(product: Product): Long {
        val entity = ProductMapper.toEntity(product)
        return if (product.id == 0L) {
            productDao.insertProduct(entity)
        } else {
            productDao.updateProduct(entity)
            product.id
        }
    }

    override suspend fun deleteProduct(productId: Long) {
        productDao.deleteProduct(productId)
    }

    override suspend fun getUnits(productId: Long): List<ProductUnit> =
        productDao.getUnits(productId).map(ProductMapper::toModel)

    override suspend fun findUnitByBarcode(barcode: String): ProductUnit? =
        productDao.findUnitByBarcode(barcode)?.let(ProductMapper::toModel)

    override suspend fun upsertUnit(unit: ProductUnit): Long {
        val entity = ProductMapper.toEntity(unit)
        return if (unit.id == 0L) {
            productDao.insertUnit(entity)
        } else {
            productDao.updateUnit(entity)
            unit.id
        }
    }

    override suspend fun deleteUnit(unitId: Long) {
        productDao.deleteUnit(unitId)
    }
}
