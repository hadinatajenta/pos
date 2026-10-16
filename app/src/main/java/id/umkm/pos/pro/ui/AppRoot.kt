package id.umkm.pos.pro.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import id.umkm.pos.pro.feature.reports.ReportsRoute
import id.umkm.pos.pro.ui.cashier.CashierRoute
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LocalConnectivityMonitor
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.rememberAndroidConnectivityMonitor
import id.umkm.pos.pro.ui.common.rememberReceiptSharer
import id.umkm.pos.pro.ui.customers.CustomersRoute
import id.umkm.pos.pro.ui.navigation.PosDestination
import id.umkm.pos.pro.ui.products.ProductsRoute
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSpacing

const val COMPACT_NAVIGATION_TEST_TAG = "compact-navigation-bar"
const val EXPANDED_NAVIGATION_TEST_TAG = "expanded-navigation-rail"

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var selectedDestination by rememberSaveable { mutableStateOf(PosDestination.CASHIER) }
    val connectivityMonitor = rememberAndroidConnectivityMonitor()
    val shareReceipt = rememberReceiptSharer()
    CompositionLocalProvider(LocalConnectivityMonitor provides connectivityMonitor) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isExpandedWindow = maxWidth >= PosBreakpoints.expandedNavigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
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
    NavigationBar(modifier = Modifier.testTag(COMPACT_NAVIGATION_TEST_TAG)) {
        PosDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { Text(text = destination.label, maxLines = 1) },
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
        modifier = Modifier.testTag(EXPANDED_NAVIGATION_TEST_TAG),
        header = { Spacer(modifier = Modifier.height(PosSpacing.sm)) },
    ) {
        PosDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { Text(text = destination.label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun DestinationHost(
    destination: PosDestination,
    onShareReceipt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        PosDestination.CASHIER -> CashierRoute(onShareReceipt = onShareReceipt, modifier = modifier)
        PosDestination.PRODUCTS -> ProductsRoute(modifier = modifier)
        PosDestination.CUSTOMERS -> CustomersRoute(modifier = modifier)
        PosDestination.REPORTS -> ReportsRoute(modifier = modifier)
        PosDestination.MORE -> MoreDestination(modifier = modifier)
    }
}

@Composable
private fun MoreDestination(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "More",
            supportingText = "Setup and data tools",
        )
        EmptyState(
            icon = Icons.Filled.MoreHoriz,
            title = "Setup tools are not connected yet",
            supportingText = "Printer, scanner, backup, export, and staff access settings appear here once their modules are wired in.",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
