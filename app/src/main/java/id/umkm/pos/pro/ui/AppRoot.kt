package id.umkm.pos.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.welcome.WelcomeScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

const val COMPACT_NAVIGATION_TEST_TAG = "compact-navigation-bar"
const val EXPANDED_NAVIGATION_TEST_TAG = "expanded-navigation-rail"

private const val FLOATING_NAVIGATION_ELEVATION = 4

private val FloatingNavigationShape = RoundedCornerShape(28.dp)
private val FloatingNavigationMinHeight = 56.dp
private val FloatingNavigationIndicatorWidth = 26.dp
private val FloatingNavigationIndicatorHeight = 3.dp

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val onboardingStore = LocalAppContainer.current.onboardingStore
    var hasCompletedOnboarding by rememberSaveable {
        mutableStateOf(onboardingStore.hasCompletedOnboarding())
    }
    if (!hasCompletedOnboarding) {
        WelcomeScreen(
            onStart = {
                onboardingStore.markOnboardingCompleted()
                hasCompletedOnboarding = true
            },
            modifier = modifier,
        )
        return
    }
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
                        onOpenProducts = { selectedDestination = PosDestination.PRODUK },
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm)
            .testTag(COMPACT_NAVIGATION_TEST_TAG),
    ) {
        Surface(
            color = Color.White,
            contentColor = PosColors.TextPrimary,
            shape = FloatingNavigationShape,
            shadowElevation = FLOATING_NAVIGATION_ELEVATION.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PosDestination.entries.forEach { destination ->
                    FloatingNavigationItem(
                        destination = destination,
                        isSelected = destination == selectedDestination,
                        onClick = { onDestinationSelected(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavigationItem(
    destination: PosDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSelected) PosColors.Accent else PosColors.TextPrimary
    Box(
        modifier = modifier
            .heightIn(min = FloatingNavigationMinHeight)
            .clip(MaterialTheme.shapes.large)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(FloatingNavigationIndicatorWidth)
                    .height(FloatingNavigationIndicatorHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PosColors.Accent),
            )
        }
        Column(
            modifier = Modifier.padding(vertical = PosSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
        ) {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(PosSizes.iconMedium),
            )
            Text(
                text = destination.label,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
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
                icon = { Icon(painter = painterResource(destination.iconRes), contentDescription = null) },
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
    onOpenProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        PosDestination.BERANDA -> DashboardRoute(
            modifier = modifier,
            onOpenCashier = onOpenCashier,
            onOpenProducts = onOpenProducts,
        )
        PosDestination.KASIR -> CashierRoute(onShareReceipt = onShareReceipt, modifier = modifier)
        PosDestination.PRODUK -> ProductsRoute(modifier = modifier)
        PosDestination.LAPORAN -> ReportsRoute(modifier = modifier)
        PosDestination.LAINNYA -> MoreRoute(modifier = modifier)
    }
}
