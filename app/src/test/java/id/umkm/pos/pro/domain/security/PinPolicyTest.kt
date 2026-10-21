package id.umkm.pos.pro.domain.security

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityRole
import org.junit.Test

class PinPolicyTest {

    private val policy = PinPolicy()

    @Test
    fun ownerLevelActionsAlwaysPromptAndManagerLevelActionsAcceptASession() {
        assertThat(policy.alwaysPrompts(CriticalAction.CHANGE_PIN)).isTrue()
        assertThat(policy.alwaysPrompts(CriticalAction.RESTORE_BACKUP)).isTrue()
        assertThat(policy.alwaysPrompts(CriticalAction.EXPORT_BACKUP)).isTrue()
        assertThat(policy.alwaysPrompts(CriticalAction.EDIT_PRODUCT_COST)).isTrue()
        assertThat(policy.alwaysPrompts(CriticalAction.CHANGE_TAX_AND_ROUNDING)).isTrue()
        assertThat(policy.alwaysPrompts(CriticalAction.REMOVE_QRIS)).isTrue()

        assertThat(policy.alwaysPrompts(CriticalAction.VIEW_PROFIT_AND_COST)).isFalse()
        assertThat(policy.alwaysPrompts(CriticalAction.ITEM_DISCOUNT_ABOVE_THRESHOLD)).isFalse()
        assertThat(policy.alwaysPrompts(CriticalAction.PROCESS_RETURN)).isFalse()
        assertThat(policy.alwaysPrompts(CriticalAction.EXPORT_SALES_REPORT)).isFalse()
    }

    @Test
    fun everyCriticalActionDeclaresARole() {
        CriticalAction.entries.forEach { action ->
            assertThat(policy.requiredRole(action)).isNotNull()
        }
        assertThat(policy.requiredRole(CriticalAction.RECORD_DEBT_PAYMENT)).isEqualTo(SecurityRole.MANAGER)
        assertThat(policy.requiredRole(CriticalAction.VIEW_SECURITY_AUDIT)).isEqualTo(SecurityRole.OWNER)
    }

    @Test
    fun roleCoverageFollowsTheDeclaredOrder() {
        assertThat(SecurityRole.OWNER.covers(SecurityRole.MANAGER)).isTrue()
        assertThat(SecurityRole.OWNER.covers(SecurityRole.OWNER)).isTrue()
        assertThat(SecurityRole.MANAGER.covers(SecurityRole.OWNER)).isFalse()
        assertThat(SecurityRole.CASHIER.covers(SecurityRole.CASHIER)).isTrue()
        assertThat(SecurityRole.MANAGER.covers(SecurityRole.CASHIER)).isTrue()
    }

    @Test
    fun pinLengthAndDigitsAreValidated() {
        assertThat(policy.isValidPin("1234")).isTrue()
        assertThat(policy.isValidPin("12345678")).isTrue()

        assertThat(policy.isValidPin("123")).isFalse()
        assertThat(policy.isValidPin("123456789")).isFalse()
        assertThat(policy.isValidPin("12a4")).isFalse()
        assertThat(policy.isValidPin("")).isFalse()
    }

    @Test
    fun discountAmountThresholdTriggersAboveTheNominalLimit() {
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 500_000L, discount = 20_000L)).isFalse()
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 500_000L, discount = 20_001L)).isTrue()
    }

    @Test
    fun discountPercentThresholdTriggersAboveThePercentageLimit() {
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 100_000L, discount = 10_000L)).isFalse()
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 100_000L, discount = 10_001L)).isTrue()
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 50_000L, discount = 5_000L)).isFalse()
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 50_000L, discount = 5_001L)).isTrue()
    }

    @Test
    fun discountWithoutAValueNeverTriggers() {
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 100_000L, discount = 0L)).isFalse()
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 0L, discount = 5_000L)).isFalse()
        assertThat(policy.exceedsDiscountThreshold(lineSubtotal = 100_000L, discount = -1L)).isFalse()
    }

    @Test
    fun negotiatedPriceOnlyTriggersBelowACostThatIsKnown() {
        assertThat(policy.isBelowCost(negotiatedUnitPrice = 9_999L, baseHpp = 10_000L)).isTrue()
        assertThat(policy.isBelowCost(negotiatedUnitPrice = 10_000L, baseHpp = 10_000L)).isFalse()
        assertThat(policy.isBelowCost(negotiatedUnitPrice = 12_000L, baseHpp = 10_000L)).isFalse()
        assertThat(policy.isBelowCost(negotiatedUnitPrice = 1_000L, baseHpp = 0L)).isFalse()
    }

    @Test
    fun lockoutEscalatesAtFiveAndTenFailures() {
        assertThat(policy.lockoutUntilAfterFailure(nowMillis = 1_000L, failedAttempts = 4)).isNull()
        assertThat(policy.lockoutUntilAfterFailure(nowMillis = 1_000L, failedAttempts = 5))
            .isEqualTo(1_000L + 5 * 60_000L)
        assertThat(policy.lockoutUntilAfterFailure(nowMillis = 1_000L, failedAttempts = 9))
            .isEqualTo(1_000L + 5 * 60_000L)
        assertThat(policy.lockoutUntilAfterFailure(nowMillis = 1_000L, failedAttempts = 10))
            .isEqualTo(1_000L + 15 * 60_000L)
    }

    @Test
    fun remainingAttemptsAndRetryDelayStepUpBeforeTheLockout() {
        assertThat(policy.remainingAttempts(0)).isEqualTo(5)
        assertThat(policy.remainingAttempts(4)).isEqualTo(1)
        assertThat(policy.remainingAttempts(5)).isEqualTo(0)
        assertThat(policy.remainingAttempts(7)).isEqualTo(0)

        assertThat(policy.retryDelayMillis(1)).isEqualTo(0L)
        assertThat(policy.retryDelayMillis(2)).isEqualTo(0L)
        assertThat(policy.retryDelayMillis(3)).isEqualTo(5_000L)
        assertThat(policy.retryDelayMillis(4)).isEqualTo(10_000L)
        assertThat(policy.retryDelayMillis(5)).isEqualTo(0L)
    }

    @Test
    fun lockStaysActiveUntilTheDeadlinePasses() {
        assertThat(policy.isLocked(nowMillis = 999L, lockoutUntilMillis = 1_000L)).isTrue()
        assertThat(policy.isLocked(nowMillis = 1_000L, lockoutUntilMillis = 1_000L)).isFalse()
        assertThat(policy.isLocked(nowMillis = 1_000L, lockoutUntilMillis = null)).isFalse()
    }

    @Test
    fun sessionExpiryUsesTheConfiguredWindow() {
        assertThat(policy.sessionExpiryMillis(10_000L)).isEqualTo(10_000L + 5 * 60_000L)
        assertThat(PinPolicy(sessionUnlockMinutes = 1).sessionExpiryMillis(0L)).isEqualTo(60_000L)
    }
}
