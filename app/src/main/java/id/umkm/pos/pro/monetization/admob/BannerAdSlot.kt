package id.umkm.pos.pro.monetization.admob

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import id.umkm.pos.pro.BuildConfig
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.ui.common.LocalConnectivityMonitor

@Composable
fun BannerAdSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    onHeightChanged: (Dp) -> Unit = {},
) {
    val isOnline by LocalConnectivityMonitor.current.isOnline.collectAsState(initial = false)
    val adUnitId = bannerUnitId(placement)
    if (LocalInspectionMode.current || adUnitId == null || !AdPolicy.supportsBanner(placement) || !isOnline) {
        LaunchedEffect(placement, adUnitId, isOnline) { onHeightChanged(0.dp) }
        return
    }
    AdMobBanner(adUnitId = adUnitId, modifier = modifier, onHeightChanged = onHeightChanged)
}

private fun bannerUnitId(placement: AdPlacement): String? = when (placement) {
    AdPlacement.REPORTS -> BuildConfig.ADMOB_BANNER_UNIT_ID
    else -> null
}

@Composable
private fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    onHeightChanged: (Dp) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (maxWidth < 320.dp) {
            LaunchedEffect(Unit) { onHeightChanged(0.dp) }
            return@BoxWithConstraints
        }
        val adWidthDp = maxWidth.value.toInt().coerceAtMost(970)
        val adSize = remember(adUnitId, adWidthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
        }
        var loadedHeightPx by remember(adUnitId, adSize) { mutableStateOf<Int?>(null) }
        val adView = remember(adUnitId, adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
            }
        }

        DisposableEffect(adView) {
            val listener = object : AdListener() {
                override fun onAdLoaded() {
                    loadedHeightPx = (adView.adSize ?: adSize).getHeightInPixels(context)
                }
            }
            adView.adListener = listener
            adView.loadAd(AdRequest.Builder().build())
            onDispose {
                adView.destroy()
            }
        }

        val adHeightDp = loadedHeightPx?.let { heightPx ->
            with(density) { heightPx.toDp() }
        } ?: 0.dp
        LaunchedEffect(adHeightDp) { onHeightChanged(adHeightDp) }

        AndroidView(
            factory = { adView },
            modifier = Modifier
                .width(adWidthDp.dp)
                .height(adHeightDp),
        )
    }
}
