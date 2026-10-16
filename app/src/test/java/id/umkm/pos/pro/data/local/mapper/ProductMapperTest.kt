package id.umkm.pos.pro.data.local.mapper

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import org.junit.Test

class ProductMapperTest {

    @Test
    fun productRoundTripsThroughEntity() {
        val product = Product(
            id = 7L,
            sku = "SKU-001",
            barcode = "8991234567890",
            name = "Kopi Sachet",
            category = "Minuman",
            baseUnit = "pcs",
            baseHpp = 1_500L,
            basePrice = 2_000L,
            stockBaseQty = 120.5,
            minStockAlert = 10.0,
            isActive = true,
        )

        val restored = ProductMapper.toModel(ProductMapper.toEntity(product))

        assertThat(restored).isEqualTo(product)
    }

    @Test
    fun productEntityKeepsMoneyAsLongAndQuantityAsDouble() {
        val product = Product(
            id = 0L,
            sku = "SKU-002",
            barcode = null,
            name = "Gula",
            category = null,
            baseUnit = "kg",
            baseHpp = 12_000L,
            basePrice = 15_000L,
            stockBaseQty = 8.25,
            minStockAlert = 2.0,
            isActive = false,
        )

        val entity = ProductMapper.toEntity(product)

        assertThat(entity.baseHpp).isEqualTo(12_000L)
        assertThat(entity.basePrice).isEqualTo(15_000L)
        assertThat(entity.stockBaseQty).isEqualTo(8.25)
        assertThat(entity.minStockAlert).isEqualTo(2.0)
        assertThat(entity.barcode).isNull()
        assertThat(entity.category).isNull()
        assertThat(entity.isActive).isFalse()
        assertThat(entity.id).isEqualTo(0L)
    }

    @Test
    fun productUnitRoundTripsThroughEntity() {
        val unit = ProductUnit(
            id = 3L,
            productId = 7L,
            variantName = "Dus",
            conversionFactor = 40.0,
            customBarcode = "8990000000012",
            unitPrice = 78_000L,
            wholesaleMinQty = 5,
            wholesalePrice = 75_000L,
        )

        val restored = ProductMapper.toModel(ProductMapper.toEntity(unit))

        assertThat(restored).isEqualTo(unit)
    }

    @Test
    fun productUnitKeepsNullableWholesaleFieldsNull() {
        val unit = ProductUnit(
            id = 0L,
            productId = 7L,
            variantName = "Pcs",
            conversionFactor = 1.0,
            customBarcode = null,
            unitPrice = 2_000L,
            wholesaleMinQty = null,
            wholesalePrice = null,
        )

        val entity = ProductMapper.toEntity(unit)

        assertThat(entity.customBarcode).isNull()
        assertThat(entity.wholesaleMinQty).isNull()
        assertThat(entity.wholesalePrice).isNull()
        assertThat(entity.conversionFactor).isEqualTo(1.0)
        assertThat(entity.unitPrice).isEqualTo(2_000L)
    }
}
