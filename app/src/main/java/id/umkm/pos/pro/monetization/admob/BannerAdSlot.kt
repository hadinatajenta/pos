package id.umkm.pos.pro.monetization.admob

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.ui.LocalAppContainer
import androidx.compose.runtime.getValue

@Composable
fun BannerAdSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    if (!AdPolicy.supportsBanner(placement)) return
    val adController = LocalAppContainer.current.adController
    val isAdAvailable by adController.observeAvailability(placement).collectAsState(initial = false)
    if (!isAdAvailable) return
    Box(modifier = modifier.fillMaxWidth()) {
        content()
    }
}
