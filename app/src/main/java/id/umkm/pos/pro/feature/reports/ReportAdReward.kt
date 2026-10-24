package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.RewardedAdOutcome
import kotlinx.coroutines.CancellationException

internal suspend fun requestExportRewardedAd(adController: AdController): RewardedAdOutcome =
    try {
        adController.showRewarded(AdPlacement.EXCEL_EXPORT)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RewardedAdOutcome.UNAVAILABLE
    }
