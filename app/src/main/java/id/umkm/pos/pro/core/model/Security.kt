package id.umkm.pos.pro.core.model

enum class SecurityRole {
    CASHIER,
    MANAGER,
    OWNER,
    ;

    fun covers(required: SecurityRole): Boolean = ordinal >= required.ordinal

    companion object {
        fun fromName(name: String?): SecurityRole? = entries.firstOrNull { it.name == name }
    }
}

enum class CriticalAction(val id: String) {
    VIEW_PROFIT_AND_COST("view_profit_and_cost"),
    EDIT_PRODUCT_COST("edit_product_cost"),
    EDIT_PRODUCT_PRICE("edit_product_price"),
    ITEM_DISCOUNT_ABOVE_THRESHOLD("item_discount_above_threshold"),
    NEGOTIATED_PRICE_BELOW_COST("negotiated_price_below_cost"),
    PROCESS_RETURN("process_return"),
    EXPORT_SALES_REPORT("export_sales_report"),
    EXPORT_BACKUP("export_backup"),
    RESTORE_BACKUP("restore_backup"),
    DELETE_PRODUCT("delete_product"),
    CHANGE_TAX_AND_ROUNDING("change_tax_and_rounding"),
    REMOVE_QRIS("remove_qris"),
    RECORD_DEBT_PAYMENT("record_debt_payment"),
    CHANGE_PIN("change_pin"),
    VIEW_SECURITY_AUDIT("view_security_audit"),
    ;

    companion object {
        fun fromId(id: String): CriticalAction? = entries.firstOrNull { it.id == id }
    }
}

data class SecurityState(
    val isPinEnabled: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long? = null,
    val sessionRole: SecurityRole? = null,
    val sessionUnlockedUntilMillis: Long? = null,
    val pinUpdatedAtMillis: Long? = null,
    val recoveryCodeConfigured: Boolean = false,
) {
    fun activeSessionRole(nowMillis: Long): SecurityRole? =
        if (sessionUnlockedUntilMillis != null && sessionUnlockedUntilMillis > nowMillis) {
            sessionRole
        } else {
            null
        }
}

data class SecurityCredentials(
    val pinHash: String?,
    val recoveryCodeHash: String?,
) {
    val isConfigured: Boolean get() = !pinHash.isNullOrBlank()
}

enum class SecurityAuditResult(val storageKey: String) {
    GRANTED("granted"),
    DENIED("denied"),
    LOCKED("locked"),
    RECOVERY_USED("recovery_used"),
    PIN_ENABLED("pin_enabled"),
    PIN_CHANGED("pin_changed"),
    PIN_DISABLED("pin_disabled"),
    RECOVERY_CODE_REGENERATED("recovery_code_regenerated"),
    RESTORE_RESET_PIN("restore_reset_pin"),
    ;

    companion object {
        fun fromStorageKey(key: String): SecurityAuditResult? =
            entries.firstOrNull { it.storageKey == key }
    }
}

data class SecurityAuditEntry(
    val occurredAtMillis: Long,
    val actionId: String,
    val result: SecurityAuditResult,
    val actorLabel: String? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val id: Long = 0L,
)
