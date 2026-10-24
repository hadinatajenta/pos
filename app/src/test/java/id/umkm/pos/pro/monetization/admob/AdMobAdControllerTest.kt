package id.umkm.pos.pro.monetization.admob

import android.content.ContextWrapper
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.app.CurrentActivityProvider
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.RewardedAdOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AdMobAdControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeActivityProvider = CurrentActivityProvider()

    @Test
    fun observeAvailabilityReturnsFalse() = runTest(testDispatcher) {
        val controller = AdMobAdController(
            context = ContextWrapper(null),
            activityProvider = fakeActivityProvider,
            rewardedExportUnitId = "test-unit",
            mainDispatcher = testDispatcher,
        )
        assertThat(controller.observeAvailability(AdPlacement.EXCEL_EXPORT).first()).isFalse()
    }

    @Test
    fun returnsUnavailableForNonExcelExportPlacement() = runTest(testDispatcher) {
        val controller = AdMobAdController(
            context = ContextWrapper(null),
            activityProvider = fakeActivityProvider,
            rewardedExportUnitId = "test-unit",
            mainDispatcher = testDispatcher,
        )
        val outcome = controller.showRewarded(AdPlacement.REPORTS)
        assertThat(outcome).isEqualTo(RewardedAdOutcome.UNAVAILABLE)
    }

    @Test
    fun returnsUnavailableWhenUnitIdIsBlank() = runTest(testDispatcher) {
        val controller = AdMobAdController(
            context = ContextWrapper(null),
            activityProvider = fakeActivityProvider,
            rewardedExportUnitId = "   ",
            mainDispatcher = testDispatcher,
        )
        val outcome = controller.showRewarded(AdPlacement.EXCEL_EXPORT)
        assertThat(outcome).isEqualTo(RewardedAdOutcome.UNAVAILABLE)
    }

    @Test
    fun returnsUnavailableWhenNoCurrentActivity() = runTest(testDispatcher) {
        val controller = AdMobAdController(
            context = ContextWrapper(null),
            activityProvider = fakeActivityProvider,
            rewardedExportUnitId = "test-unit",
            mainDispatcher = testDispatcher,
        )
        val outcome = controller.showRewarded(AdPlacement.EXCEL_EXPORT)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(outcome).isEqualTo(RewardedAdOutcome.UNAVAILABLE)
    }
}
