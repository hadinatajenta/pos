package id.umkm.pos.pro.data.backup

object BackupTables {

    const val FORMAT_VERSION: Int = 1

    const val SECURITY_TABLE: String = "security_store"

    const val SECURITY_AUDIT_TABLE: String = "security_audit_log"

    const val STAFF_TABLE: String = "staffs"

    val insertOrder: List<String> = listOf(
        "products",
        "product_variants_units",
        "customers",
        STAFF_TABLE,
        "transactions",
        "transaction_items",
        "debt_payments",
        "returns",
        "return_lines",
        "cashier_shifts",
        "app_settings",
        "invoice_counters",
    )

    val excludedTables: List<String> = listOf(SECURITY_TABLE, SECURITY_AUDIT_TABLE)

    val deleteOrder: List<String> = insertOrder.reversed()

    fun isSupported(tableName: String): Boolean = tableName in insertOrder
}
