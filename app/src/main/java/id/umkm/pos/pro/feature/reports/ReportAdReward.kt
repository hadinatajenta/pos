package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import kotlinx.coroutines.CancellationException

internal suspend fun requestExportRewardedAd(adController: AdController): AppResult<Unit> =
    try {
        adController.showRewarded(AdPlacement.EXCEL_EXPORT)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppResult.Failure(AppError.Network(error.message ?: "rewarded ad unavailable"))
    }
