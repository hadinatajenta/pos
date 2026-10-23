package id.umkm.pos.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import id.umkm.pos.pro.data.local.entity.ProductEntity
import id.umkm.pos.pro.data.local.entity.ProductUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query(
        "SELECT * FROM products " +
            "WHERE name LIKE :pattern ESCAPE '\\' " +
            "OR sku LIKE :pattern ESCAPE '\\' " +
            "OR (barcode IS NOT NULL AND barcode LIKE :pattern ESCAPE '\\') " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeProducts(pattern: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku AND id != :excludeId LIMIT 1")
    suspend fun findProductBySku(sku: String, excludeId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND id != :excludeId LIMIT 1")
    suspend fun findProductByBarcode(barcode: String, excludeId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE name = :name AND id != :excludeId LIMIT 1")
    suspend fun findProductByName(name: String, excludeId: Long): ProductEntity?

    @Insert
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Transaction
    suspend fun upsertProductWithUnit(product: ProductEntity, unit: ProductUnitEntity?): Long {
        val productId = if (product.id == 0L) {
            insertProduct(product)
        } else {
            updateProduct(product)
            product.id
        }
        if (unit == null) return productId
        val unitEntity = unit.copy(productId = productId)
        if (unitEntity.id == 0L) {
            insertUnit(unitEntity)
        } else {
            updateUnit(unitEntity)
        }
        return productId
    }

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: Long)

    @Query("SELECT * FROM product_variants_units WHERE product_id = :productId ORDER BY conversion_factor ASC")
    suspend fun getUnits(productId: Long): List<ProductUnitEntity>

    @Query("SELECT * FROM product_variants_units WHERE custom_barcode = :barcode LIMIT 1")
    suspend fun findUnitByBarcode(barcode: String): ProductUnitEntity?

    @Insert
    suspend fun insertUnit(unit: ProductUnitEntity): Long

    @Update
    suspend fun updateUnit(unit: ProductUnitEntity)

    @Query("DELETE FROM product_variants_units WHERE id = :unitId")
    suspend fun deleteUnit(unitId: Long)

    @Query("UPDATE products SET stock_base_qty = stock_base_qty + :delta WHERE id = :productId")
    suspend fun adjustStock(productId: Long, delta: Double)

    @Query("DELETE FROM products WHERE sku LIKE :skuPattern")
    suspend fun deleteProductsBySkuPattern(skuPattern: String): Int
}
