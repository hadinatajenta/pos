package id.umkm.pos.pro.monetization.admob

import id.umkm.pos.pro.core.contract.AdController
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NoAdsController : AdController {
    override fun observeAvailability(placement: AdPlacement): Flow<Boolean> = flowOf(false)

    override suspend fun showRewarded(placement: AdPlacement): AppResult<Unit> =
        AppResult.Failure(AppError.Network("ads are not configured on this build"))
}
