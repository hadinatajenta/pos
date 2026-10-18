package id.umkm.pos.pro.data.backup

object BackupTables {

    const val FORMAT_VERSION: Int = 1

    val insertOrder: List<String> = listOf(
        "products",
        "product_variants_units",
        "customers",
        "transactions",
        "transaction_items",
        "debt_payments",
        "returns",
        "return_lines",
        "cashier_shifts",
        "security_store",
        "app_settings",
        "invoice_counters",
    )

    val deleteOrder: List<String> = insertOrder.reversed()

    fun isSupported(tableName: String): Boolean = tableName in insertOrder
}
