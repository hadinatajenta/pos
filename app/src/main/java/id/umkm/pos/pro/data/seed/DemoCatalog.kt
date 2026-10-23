package id.umkm.pos.pro.data.seed

data class DemoUnitSpec(
    val variantName: String,
    val conversionFactor: Double,
    val unitPrice: Long,
    val customBarcode: String? = null,
    val wholesaleMinQty: Int? = null,
    val wholesalePrice: Long? = null,
)

data class DemoProductSpec(
    val sku: String,
    val barcode: String?,
    val name: String,
    val category: String,
    val baseUnit: String,
    val baseHpp: Long,
    val basePrice: Long,
    val minStockAlert: Double,
    val desiredRemainingStock: Double,
    val units: List<DemoUnitSpec> = emptyList(),
)

data class DemoCustomerSpec(
    val name: String,
    val whatsappNumber: String,
)

object DemoCatalog {

    val products: List<DemoProductSpec> = listOf(
        DemoProductSpec(
            sku = "SMP-001",
            barcode = "8990000000001",
            name = "Kopi Susu",
            category = "Minuman",
            baseUnit = "gelas",
            baseHpp = 5_000L,
            basePrice = 10_000L,
            minStockAlert = 5.0,
            desiredRemainingStock = 24.0,
        ),
        DemoProductSpec(
            sku = "SMP-002",
            barcode = "8990000000002",
            name = "Es Teh Manis",
            category = "Minuman",
            baseUnit = "gelas",
            baseHpp = 2_000L,
            basePrice = 5_000L,
            minStockAlert = 5.0,
            desiredRemainingStock = 30.0,
        ),
        DemoProductSpec(
            sku = "SMP-003",
            barcode = "8990000000003",
            name = "Teh Botol",
            category = "Minuman",
            baseUnit = "botol",
            baseHpp = 3_500L,
            basePrice = 6_000L,
            minStockAlert = 6.0,
            desiredRemainingStock = 24.0,
            units = listOf(
                DemoUnitSpec(
                    variantName = "Dus",
                    conversionFactor = 24.0,
                    unitPrice = 138_000L,
                    wholesaleMinQty = 3,
                    wholesalePrice = 130_000L,
                ),
            ),
        ),
        DemoProductSpec(
            sku = "SMP-004",
            barcode = "8990000000004",
            name = "Air Mineral",
            category = "Minuman",
            baseUnit = "botol",
            baseHpp = 2_500L,
            basePrice = 4_000L,
            minStockAlert = 12.0,
            desiredRemainingStock = 6.0,
        ),
        DemoProductSpec(
            sku = "SMP-005",
            barcode = "8990000000005",
            name = "Roti Cokelat",
            category = "Makanan",
            baseUnit = "pcs",
            baseHpp = 4_000L,
            basePrice = 8_000L,
            minStockAlert = 5.0,
            desiredRemainingStock = 18.0,
        ),
        DemoProductSpec(
            sku = "SMP-006",
            barcode = "8990000000006",
            name = "Indomie Goreng",
            category = "Makanan",
            baseUnit = "bungkus",
            baseHpp = 3_000L,
            basePrice = 4_000L,
            minStockAlert = 10.0,
            desiredRemainingStock = 48.0,
            units = listOf(
                DemoUnitSpec(
                    variantName = "Dus",
                    conversionFactor = 40.0,
                    unitPrice = 150_000L,
                    wholesaleMinQty = 2,
                    wholesalePrice = 145_000L,
                ),
            ),
        ),
        DemoProductSpec(
            sku = "SMP-007",
            barcode = null,
            name = "Mie Ayam",
            category = "Makanan",
            baseUnit = "porsi",
            baseHpp = 8_000L,
            basePrice = 15_000L,
            minStockAlert = 3.0,
            desiredRemainingStock = 12.0,
        ),
        DemoProductSpec(
            sku = "SMP-008",
            barcode = null,
            name = "Nasi Goreng",
            category = "Makanan",
            baseUnit = "porsi",
            baseHpp = 9_000L,
            basePrice = 18_000L,
            minStockAlert = 3.0,
            desiredRemainingStock = 15.0,
        ),
        DemoProductSpec(
            sku = "SMP-009",
            barcode = "8990000000009",
            name = "Keripik Singkong",
            category = "Snack",
            baseUnit = "bungkus",
            baseHpp = 5_000L,
            basePrice = 9_000L,
            minStockAlert = 8.0,
            desiredRemainingStock = 5.0,
        ),
        DemoProductSpec(
            sku = "SMP-010",
            barcode = "8990000000010",
            name = "Gula Pasir",
            category = "Sembako",
            baseUnit = "kg",
            baseHpp = 12_000L,
            basePrice = 15_000L,
            minStockAlert = 5.0,
            desiredRemainingStock = 25.0,
            units = listOf(
                DemoUnitSpec(
                    variantName = "Karung",
                    conversionFactor = 50.0,
                    unitPrice = 700_000L,
                    wholesaleMinQty = 1,
                    wholesalePrice = 690_000L,
                ),
            ),
        ),
        DemoProductSpec(
            sku = "SMP-011",
            barcode = "8990000000011",
            name = "Minyak Goreng",
            category = "Sembako",
            baseUnit = "liter",
            baseHpp = 14_000L,
            basePrice = 18_000L,
            minStockAlert = 6.0,
            desiredRemainingStock = 0.0,
        ),
        DemoProductSpec(
            sku = "SMP-012",
            barcode = "8990000000012",
            name = "Telur 1kg",
            category = "Sembako",
            baseUnit = "kg",
            baseHpp = 25_000L,
            basePrice = 30_000L,
            minStockAlert = 4.0,
            desiredRemainingStock = 16.0,
        ),
        DemoProductSpec(
            sku = "SMP-013",
            barcode = "8990000000013",
            name = "Beras 5kg",
            category = "Sembako",
            baseUnit = "karung",
            baseHpp = 60_000L,
            basePrice = 72_000L,
            minStockAlert = 3.0,
            desiredRemainingStock = -1.0,
        ),
        DemoProductSpec(
            sku = "SMP-014",
            barcode = "8990000000014",
            name = "Sabun Mandi",
            category = "Kebersihan",
            baseUnit = "pcs",
            baseHpp = 3_500L,
            basePrice = 5_500L,
            minStockAlert = 6.0,
            desiredRemainingStock = 20.0,
        ),
    )

    val customers: List<DemoCustomerSpec> = listOf(
        DemoCustomerSpec(name = "Budi Santoso", whatsappNumber = "6281200000001"),
        DemoCustomerSpec(name = "Siti Aminah", whatsappNumber = "6281200000002"),
        DemoCustomerSpec(name = "Rizky Pratama", whatsappNumber = "6281200000003"),
        DemoCustomerSpec(name = "Dewi Lestari", whatsappNumber = "6281200000004"),
    )

    val kopiSusuIndex: Int by lazy { indexOfName("Kopi Susu") }
    val indomieIndex: Int by lazy { indexOfName("Indomie Goreng") }
    val minyakGorengIndex: Int by lazy { indexOfName("Minyak Goreng") }
    val berasIndex: Int by lazy { indexOfName("Beras 5kg") }

    fun indexOfName(name: String): Int {
        val index = products.indexOfFirst { it.name == name }
        require(index >= 0) { "demo catalog is missing product $name" }
        return index
    }
}
