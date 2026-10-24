package id.umkm.pos.pro.monetization.admob

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import id.umkm.pos.pro.app.CurrentActivityProvider
import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.RewardedAdOutcome
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AdMobAdController(
    private val context: Context,
    private val activityProvider: CurrentActivityProvider,
    private val rewardedExportUnitId: String,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : AdController {

    override fun observeAvailability(placement: AdPlacement): Flow<Boolean> = flowOf(false)

    override suspend fun showRewarded(placement: AdPlacement): RewardedAdOutcome {
        if (placement != AdPlacement.EXCEL_EXPORT) {
            return RewardedAdOutcome.UNAVAILABLE
        }
        if (rewardedExportUnitId.isBlank()) {
            return RewardedAdOutcome.UNAVAILABLE
        }
        return try {
            withContext(mainDispatcher) {
                showRewardedInternal(rewardedExportUnitId)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            RewardedAdOutcome.UNAVAILABLE
        }
    }

    private suspend fun showRewardedInternal(unitId: String): RewardedAdOutcome {
        val initialActivity = activityProvider.currentActivity
        if (initialActivity == null || initialActivity.isFinishing || initialActivity.isDestroyed) {
            return RewardedAdOutcome.UNAVAILABLE
        }

        return suspendCancellableCoroutine { continuation ->
            val isResolved = AtomicBoolean(false)

            fun resolve(outcome: RewardedAdOutcome) {
                if (isResolved.compareAndSet(false, true)) {
                    if (continuation.isActive) {
                        continuation.resume(outcome)
                    }
                }
            }

            continuation.invokeOnCancellation {
                isResolved.set(true)
            }

            try {
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context,
                    unitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            resolve(RewardedAdOutcome.UNAVAILABLE)
                        }

                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            if (isResolved.get()) {
                                return
                            }
                            val activity = activityProvider.currentActivity
                            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                                resolve(RewardedAdOutcome.UNAVAILABLE)
                                return
                            }

                            var earned = false

                            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    resolve(if (earned) RewardedAdOutcome.EARNED else RewardedAdOutcome.DISMISSED)
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    resolve(RewardedAdOutcome.UNAVAILABLE)
                                }
                            }

                            try {
                                rewardedAd.show(activity) { _ ->
                                    earned = true
                                }
                            } catch (_: Exception) {
                                resolve(RewardedAdOutcome.UNAVAILABLE)
                            }
                        }
                    },
                )
            } catch (_: Exception) {
                resolve(RewardedAdOutcome.UNAVAILABLE)
            }
        }
    }
}
