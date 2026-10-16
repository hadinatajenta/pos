package id.umkm.pos.pro.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.feature.reports.ReportsRoute
import id.umkm.pos.pro.ui.cashier.CashierRoute
import id.umkm.pos.pro.ui.common.LocalConnectivityMonitor
import id.umkm.pos.pro.ui.common.rememberAndroidConnectivityMonitor
import id.umkm.pos.pro.ui.common.rememberReceiptSharer
import id.umkm.pos.pro.ui.dashboard.DashboardRoute
import id.umkm.pos.pro.ui.more.MoreRoute
import id.umkm.pos.pro.ui.navigation.PosDestination
import id.umkm.pos.pro.ui.products.ProductsRoute
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosColors

const val COMPACT_NAVIGATION_TEST_TAG = "compact-navigation-bar"
const val EXPANDED_NAVIGATION_TEST_TAG = "expanded-navigation-rail"

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var selectedDestination by rememberSaveable { mutableStateOf(PosDestination.BERANDA) }
    val connectivityMonitor = rememberAndroidConnectivityMonitor()
    val shareReceipt = rememberReceiptSharer()
    CompositionLocalProvider(LocalConnectivityMonitor provides connectivityMonitor) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isExpandedWindow = maxWidth >= PosBreakpoints.expandedNavigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isExpandedWindow) {
                        PrimaryNavigationBar(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { destination -> selectedDestination = destination },
                        )
                    }
                },
            ) { contentPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                    if (isExpandedWindow) {
                        PrimaryNavigationRail(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { destination -> selectedDestination = destination },
                        )
                    }
                    DestinationHost(
                        destination = selectedDestination,
                        onShareReceipt = shareReceipt,
                        onOpenCashier = { selectedDestination = PosDestination.KASIR },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryNavigationBar(
    selectedDestination: PosDestination,
    onDestinationSelected: (PosDestination) -> Unit,
) {
    NavigationBar(
        containerColor = PosColors.ForestGreen,
        contentColor = Color.White,
        modifier = Modifier.testTag(COMPACT_NAVIGATION_TEST_TAG),
    ) {
        PosDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = {
                    Text(
                        text = destination.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = PosColors.GreenElevated,
                    unselectedIconColor = Color.White.copy(alpha = 0.62f),
                    unselectedTextColor = Color.White.copy(alpha = 0.62f),
                ),
            )
        }
    }
}

@Composable
private fun PrimaryNavigationRail(
    selectedDestination: PosDestination,
    onDestinationSelected: (PosDestination) -> Unit,
) {
    NavigationRail(
        containerColor = PosColors.ForestGreen,
        contentColor = Color.White,
        modifier = Modifier.testTag(EXPANDED_NAVIGATION_TEST_TAG),
    ) {
        PosDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { Text(text = destination.label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = PosColors.GreenElevated,
                    unselectedIconColor = Color.White.copy(alpha = 0.62f),
                    unselectedTextColor = Color.White.copy(alpha = 0.62f),
                ),
            )
        }
    }
}

@Composable
private fun DestinationHost(
    destination: PosDestination,
    onShareReceipt: (String) -> Unit,
    onOpenCashier: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        PosDestination.BERANDA -> DashboardRoute(
            modifier = modifier,
            onOpenCashier = onOpenCashier,
        )
        PosDestination.KASIR -> CashierRoute(onShareReceipt = onShareReceipt, modifier = modifier)
        PosDestination.PRODUK -> ProductsRoute(modifier = modifier)
        PosDestination.LAPORAN -> ReportsRoute(modifier = modifier)
        PosDestination.LAINNYA -> MoreRoute(modifier = modifier)
    }
}
