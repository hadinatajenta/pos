package id.umkm.pos.pro.monetization.admob

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AdPlacement
import org.junit.Test

class AdPolicyTest {

    @Test
    fun checkoutSurfacesNeverAllowAds() {
        assertThat(AdPolicy.checkoutSurfaces).isNotEmpty()
        AdPolicy.checkoutSurfaces.forEach { surface ->
            assertThat(AdPolicy.isAllowed(surface)).isFalse()
        }
    }

    @Test
    fun checkoutSurfacesExistForEveryCriticalFlow() {
        assertThat(AdPolicy.checkoutSurfaces).containsExactly(
            AdSurface.CASHIER,
            AdSurface.CART,
            AdSurface.PAYMENT,
            AdSurface.TRANSACTION_SUCCESS,
            AdSurface.RECEIPT_PRINTING,
        )
    }

    @Test
    fun checkoutSurfacesHaveNoPlacementSoTheyCannotRequestAnAd() {
        AdPolicy.checkoutSurfaces.forEach { surface ->
            assertThat(surface.placement).isNull()
            assertThat(surface.format).isNull()
        }
    }

    @Test
    fun noAdPlacementResolvesToACheckoutSurface() {
        AdPlacement.entries.forEach { placement ->
            val surface = AdPolicy.surfaceFor(placement)
            assertThat(surface).isNotNull()
            assertThat(surface!!.isCheckoutCritical).isFalse()
        }
    }

    @Test
    fun everyAdPlacementIsPermittedByProductPolicy() {
        AdPlacement.entries.forEach { placement ->
            assertThat(AdPolicy.isAllowed(placement)).isTrue()
        }
    }

    @Test
    fun bannerPlacementsMatchThePrd() {
        assertThat(AdPolicy.supportsBanner(AdPlacement.DEBT_LIST)).isTrue()
        assertThat(AdPolicy.supportsBanner(AdPlacement.STOCK_MANAGEMENT)).isTrue()
        assertThat(AdPolicy.supportsBanner(AdPlacement.EXCEL_EXPORT)).isFalse()
        assertThat(AdPolicy.formatFor(AdPlacement.DEBT_LIST)).isEqualTo(AdFormat.BANNER)
    }

    @Test
    fun rewardedPlacementsMatchThePrd() {
        assertThat(AdPolicy.supportsRewardedVideo(AdPlacement.EXCEL_EXPORT)).isTrue()
        assertThat(AdPolicy.supportsRewardedVideo(AdPlacement.CLOUD_BACKUP)).isTrue()
        assertThat(AdPolicy.supportsRewardedVideo(AdPlacement.SHELF_LABEL_PRINT)).isTrue()
        assertThat(AdPolicy.supportsRewardedVideo(AdPlacement.DEBT_LIST)).isFalse()
    }

    @Test
    fun surfaceForUnknownPlacementsIsResolvedForAllCurrentPlacements() {
        assertThat(AdPolicy.permittedPlacements).containsExactlyElementsIn(AdPlacement.entries)
    }
}
