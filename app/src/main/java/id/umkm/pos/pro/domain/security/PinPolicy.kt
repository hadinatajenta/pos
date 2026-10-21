package id.umkm.pos.pro.domain.security

import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityRole

data class PinPolicy(
    val discountPercentThreshold: Int = 10,
    val discountAmountThreshold: Long = 20_000L,
    val sessionUnlockMinutes: Int = 5,
    val lockoutAfterAttempts: Int = 5,
    val lockoutMinutes: Int = 5,
    val globalLockoutAfterAttempts: Int = 10,
    val globalLockoutMinutes: Int = 15,
    val minPinLength: Int = 4,
    val maxPinLength: Int = 8,
    val auditRetentionEntries: Int = 1_000,
) {

    fun requiredRole(action: CriticalAction): SecurityRole = when (action) {
        CriticalAction.VIEW_PROFIT_AND_COST,
        CriticalAction.EDIT_PRODUCT_PRICE,
        CriticalAction.ITEM_DISCOUNT_ABOVE_THRESHOLD,
        CriticalAction.PROCESS_RETURN,
        CriticalAction.EXPORT_SALES_REPORT,
        CriticalAction.DELETE_PRODUCT,
        CriticalAction.RECORD_DEBT_PAYMENT,
        -> SecurityRole.MANAGER

        CriticalAction.EDIT_PRODUCT_COST,
        CriticalAction.NEGOTIATED_PRICE_BELOW_COST,
        CriticalAction.EXPORT_BACKUP,
        CriticalAction.RESTORE_BACKUP,
        CriticalAction.CHANGE_TAX_AND_ROUNDING,
        CriticalAction.REMOVE_QRIS,
        CriticalAction.SWITCH_CASHIER,
        CriticalAction.CHANGE_PIN,
        CriticalAction.VIEW_SECURITY_AUDIT,
        -> SecurityRole.OWNER
    }

    fun alwaysPrompts(action: CriticalAction): Boolean = requiredRole(action) == SecurityRole.OWNER

    fun isValidPin(pin: String): Boolean =
        pin.length in minPinLength..maxPinLength && pin.all { it.isDigit() }

    fun exceedsDiscountThreshold(lineSubtotal: Long, discount: Long): Boolean {
        if (discount <= 0L || lineSubtotal <= 0L) return false
        if (discount > discountAmountThreshold) return true
        return discount > lineSubtotal * discountPercentThreshold / 100
    }

    fun isBelowCost(negotiatedUnitPrice: Long, baseHpp: Long): Boolean =
        baseHpp > 0L && negotiatedUnitPrice < baseHpp

    fun isLocked(nowMillis: Long, lockoutUntilMillis: Long?): Boolean =
        lockoutUntilMillis != null && lockoutUntilMillis > nowMillis

    fun remainingAttempts(failedAttempts: Int): Int =
        (lockoutAfterAttempts - failedAttempts).coerceAtLeast(0)

    fun lockoutUntilAfterFailure(nowMillis: Long, failedAttempts: Int): Long? = when {
        failedAttempts >= globalLockoutAfterAttempts -> nowMillis + globalLockoutMinutes * MILLIS_PER_MINUTE
        failedAttempts >= lockoutAfterAttempts -> nowMillis + lockoutMinutes * MILLIS_PER_MINUTE
        else -> null
    }

    fun retryDelayMillis(failedAttempts: Int): Long = when (failedAttempts) {
        lockoutAfterAttempts - 2 -> 5_000L
        lockoutAfterAttempts - 1 -> 10_000L
        else -> 0L
    }

    fun sessionExpiryMillis(nowMillis: Long): Long = nowMillis + sessionUnlockMinutes * MILLIS_PER_MINUTE

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
