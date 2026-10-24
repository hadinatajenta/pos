package id.umkm.pos.pro.feature.reports

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.RewardedAdOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReportAdRewardTest {

    @Test
    fun returnsEarnedWhenAdControllerReturnsEarned() = runTest {
        val controller = RecordingAdController(outcome = RewardedAdOutcome.EARNED)
        val outcome = requestExportRewardedAd(controller)
        assertThat(outcome).isEqualTo(RewardedAdOutcome.EARNED)
    }

    @Test
    fun returnsDismissedWhenAdControllerReturnsDismissed() = runTest {
        val controller = RecordingAdController(outcome = RewardedAdOutcome.DISMISSED)
        val outcome = requestExportRewardedAd(controller)
        assertThat(outcome).isEqualTo(RewardedAdOutcome.DISMISSED)
    }

    @Test
    fun returnsUnavailableWhenAdControllerReturnsUnavailable() = runTest {
        val controller = RecordingAdController(outcome = RewardedAdOutcome.UNAVAILABLE)
        val outcome = requestExportRewardedAd(controller)
        assertThat(outcome).isEqualTo(RewardedAdOutcome.UNAVAILABLE)
    }

    @Test
    fun returnsUnavailableWhenExceptionThrown() = runTest {
        val controller = RecordingAdController(shouldThrow = true)
        val outcome = requestExportRewardedAd(controller)
        assertThat(outcome).isEqualTo(RewardedAdOutcome.UNAVAILABLE)
    }

    @Test(expected = CancellationException::class)
    fun rethrowsCancellationException() = runTest {
        val controller = object : id.umkm.pos.pro.core.contract.AdController {
            override fun observeAvailability(placement: id.umkm.pos.pro.core.contract.AdPlacement) =
                kotlinx.coroutines.flow.flowOf(true)

            override suspend fun showRewarded(placement: id.umkm.pos.pro.core.contract.AdPlacement): RewardedAdOutcome {
                throw CancellationException("cancelled")
            }
        }
        requestExportRewardedAd(controller)
    }
}
