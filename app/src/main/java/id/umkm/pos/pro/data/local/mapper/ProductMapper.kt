package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.data.local.entity.ProductEntity
import id.umkm.pos.pro.data.local.entity.ProductUnitEntity

object ProductMapper {

    fun toEntity(product: Product): ProductEntity = ProductEntity(
        id = product.id,
        sku = product.sku,
        barcode = product.barcode,
        name = product.name,
        category = product.category,
        baseUnit = product.baseUnit,
        baseHpp = product.baseHpp,
        basePrice = product.basePrice,
        stockBaseQty = product.stockBaseQty,
        minStockAlert = product.minStockAlert,
        isActive = product.isActive,
        imagePath = product.imagePath,
    )

    fun toModel(entity: ProductEntity): Product = Product(
        id = entity.id,
        sku = entity.sku,
        barcode = entity.barcode,
        name = entity.name,
        category = entity.category,
        baseUnit = entity.baseUnit,
        baseHpp = entity.baseHpp,
        basePrice = entity.basePrice,
        stockBaseQty = entity.stockBaseQty,
        minStockAlert = entity.minStockAlert,
        isActive = entity.isActive,
        imagePath = entity.imagePath,
    )

    fun toEntity(unit: ProductUnit): ProductUnitEntity = ProductUnitEntity(
        id = unit.id,
        productId = unit.productId,
        variantName = unit.variantName,
        conversionFactor = unit.conversionFactor,
        customBarcode = unit.customBarcode,
        unitPrice = unit.unitPrice,
        wholesaleMinQty = unit.wholesaleMinQty,
        wholesalePrice = unit.wholesalePrice,
    )

    fun toModel(entity: ProductUnitEntity): ProductUnit = ProductUnit(
        id = entity.id,
        productId = entity.productId,
        variantName = entity.variantName,
        conversionFactor = entity.conversionFactor,
        customBarcode = entity.customBarcode,
        unitPrice = entity.unitPrice,
        wholesaleMinQty = entity.wholesaleMinQty,
        wholesalePrice = entity.wholesalePrice,
    )
}
