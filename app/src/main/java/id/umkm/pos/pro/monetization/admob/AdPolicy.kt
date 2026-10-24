package id.umkm.pos.pro.monetization.admob

import id.umkm.pos.pro.core.contract.AdPlacement

enum class AdFormat {
    BANNER,
    REWARDED_VIDEO,
}

enum class AdSurface(
    val placement: AdPlacement?,
    val format: AdFormat?,
) {
    CASHIER(null, null),
    CART(null, null),
    PAYMENT(null, null),
    TRANSACTION_SUCCESS(null, null),
    RECEIPT_PRINTING(null, null),
    DEBT_LIST(AdPlacement.DEBT_LIST, AdFormat.BANNER),
    STOCK_MANAGEMENT(AdPlacement.STOCK_MANAGEMENT, AdFormat.BANNER),
    REPORTS(AdPlacement.REPORTS, AdFormat.BANNER),
    EXCEL_EXPORT(AdPlacement.EXCEL_EXPORT, AdFormat.REWARDED_VIDEO),
    CLOUD_BACKUP(AdPlacement.CLOUD_BACKUP, AdFormat.REWARDED_VIDEO),
    SHELF_LABEL_PRINT(AdPlacement.SHELF_LABEL_PRINT, AdFormat.REWARDED_VIDEO),
    ;

    val isCheckoutCritical: Boolean
        get() = placement == null
}

object AdPolicy {

    fun isAllowed(surface: AdSurface): Boolean = !surface.isCheckoutCritical

    fun isAllowed(placement: AdPlacement): Boolean {
        val surface = surfaceFor(placement) ?: return false
        return isAllowed(surface)
    }

    fun surfaceFor(placement: AdPlacement): AdSurface? =
        AdSurface.entries.firstOrNull { surface -> surface.placement == placement }

    fun formatFor(placement: AdPlacement): AdFormat? = surfaceFor(placement)?.format

    fun supportsBanner(placement: AdPlacement): Boolean = formatFor(placement) == AdFormat.BANNER

    fun supportsRewardedVideo(placement: AdPlacement): Boolean =
        formatFor(placement) == AdFormat.REWARDED_VIDEO

    val checkoutSurfaces: List<AdSurface>
        get() = AdSurface.entries.filter { surface -> surface.isCheckoutCritical }

    val permittedPlacements: List<AdPlacement>
        get() = AdPlacement.entries.filter { placement -> isAllowed(placement) }
}
