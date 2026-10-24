package id.umkm.pos.pro.data.seed

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.PricingContext
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.domain.pos.DefaultPriceEngine
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.resolveStockStatus
import org.junit.Test

class DemoCatalogTest {

    private val priceEngine = DefaultPriceEngine()

    @Test
    fun catalogDefinesFourteenProducts() {
        assertThat(DemoCatalog.products).hasSize(14)
    }

    @Test
    fun productsAreMarkedBySkuPrefix() {
        DemoCatalog.products.forEach { spec ->
            assertThat(DemoMarkers.isDemoSku(spec.sku)).isTrue()
        }
    }

    @Test
    fun catalogDataDoesNotExposeTheDemoWord() {
        DemoCatalog.products.forEach { spec ->
            assertThat(spec.name.lowercase()).doesNotContain("demo")
            assertThat(spec.category.lowercase()).doesNotContain("demo")
            assertThat(spec.sku.lowercase()).doesNotContain("demo")
        }
    }

    @Test
    fun skusAndBarcodesAreUnique() {
        val skus = DemoCatalog.products.map { it.sku }
        val barcodes = DemoCatalog.products.mapNotNull { it.barcode }

        assertThat(skus.toSet()).hasSize(skus.size)
        assertThat(barcodes.toSet()).hasSize(barcodes.size)
    }

    @Test
    fun pricesAndCostsArePositiveWithNonNegativeMargin() {
        DemoCatalog.products.forEach { spec ->
            assertThat(spec.baseHpp).isGreaterThan(0L)
            assertThat(spec.basePrice).isGreaterThan(0L)
            assertThat(spec.baseHpp).isLessThan(spec.basePrice)
            assertThat(spec.minStockAlert).isGreaterThan(0.0)
            assertThat(spec.name).isNotEmpty()
            assertThat(spec.baseUnit).isNotEmpty()
            assertThat(spec.category).isNotEmpty()
        }
    }

    @Test
    fun unitSpecsHavePositiveConversionAndPairedWholesaleFields() {
        DemoCatalog.products.flatMap { it.units }.forEach { unit ->
            assertThat(unit.variantName).isNotEmpty()
            assertThat(unit.conversionFactor).isGreaterThan(1.0)
            assertThat(unit.unitPrice).isGreaterThan(0L)

            val wholesaleMinQty = unit.wholesaleMinQty
            val wholesalePrice = unit.wholesalePrice
            assertThat(wholesaleMinQty == null).isEqualTo(wholesalePrice == null)
            if (wholesaleMinQty != null && wholesalePrice != null) {
                assertThat(wholesaleMinQty).isGreaterThan(0)
                assertThat(wholesalePrice).isGreaterThan(0L)
                assertThat(wholesalePrice).isLessThan(unit.unitPrice)
            }
        }
    }

    @Test
    fun stockIntentsCoverEveryStatusWithExactlyOneOversold() {
        val statuses = DemoCatalog.products.map { spec ->
            resolveStockStatus(spec.toDemoProduct(stock = spec.desiredRemainingStock))
        }

        assertThat(statuses).contains(StockStatus.IN_STOCK)
        assertThat(statuses).contains(StockStatus.LOW_STOCK)
        assertThat(statuses).contains(StockStatus.OVERSOLD)
        assertThat(statuses.count { it == StockStatus.OVERSOLD }).isEqualTo(1)
    }

    @Test
    fun priceEnginePricesEveryCatalogProductAtBasePrice() {
        DemoCatalog.products.forEachIndexed { index, spec ->
            val product = spec.toDemoProduct(stock = spec.desiredRemainingStock, id = index + 1L)

            val pricedCart = priceEngine.priceCart(
                lines = listOf(
                    CartLine(
                        lineId = "L1",
                        productId = product.id,
                        unitId = null,
                        variantName = null,
                        qty = 1.0,
                        negotiatedUnitPrice = null,
                        itemDiscount = 0L,
                    ),
                ),
                products = mapOf(product.id to product),
                unitsByProduct = emptyMap(),
                context = PricingContext(),
            )

            assertThat(pricedCart.lines.single().unitPrice).isEqualTo(spec.basePrice)
            assertThat(pricedCart.totalHpp).isEqualTo(spec.baseHpp)
        }
    }

    @Test
    fun namedProductsAreResolvable() {
        assertThat(DemoCatalog.kopiSusuIndex).isEqualTo(DemoCatalog.indexOfName("Kopi Susu"))
        assertThat(DemoCatalog.indomieIndex).isEqualTo(DemoCatalog.indexOfName("Indomie Goreng"))
        assertThat(DemoCatalog.minyakGorengIndex).isEqualTo(DemoCatalog.indexOfName("Minyak Goreng"))
        assertThat(DemoCatalog.berasIndex).isEqualTo(DemoCatalog.indexOfName("Beras 5kg"))
    }
}

private fun DemoProductSpec.toDemoProduct(stock: Double, id: Long = 1L): Product = Product(
    id = id,
    sku = sku,
    barcode = barcode,
    name = name,
    category = DemoMarkers.categoryFor(category),
    baseUnit = baseUnit,
    baseHpp = baseHpp,
    basePrice = basePrice,
    stockBaseQty = stock,
    minStockAlert = minStockAlert,
    isActive = true,
)
