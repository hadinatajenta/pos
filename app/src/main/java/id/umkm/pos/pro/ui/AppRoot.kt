package id.umkm.pos.pro.ui

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.data.file.DownloadsFileSink
import id.umkm.pos.pro.feature.reports.ReportsPresenter
import id.umkm.pos.pro.feature.reports.ReportsRoute
import id.umkm.pos.pro.feature.reports.SalesCsvExporter
import id.umkm.pos.pro.feature.reports.XlsxReportExporter
import id.umkm.pos.pro.feature.transactions.TransactionsRoute
import id.umkm.pos.pro.ui.cashier.CashierRoute
import id.umkm.pos.pro.ui.common.LocalConnectivityMonitor
import id.umkm.pos.pro.ui.common.rememberAndroidConnectivityMonitor
import id.umkm.pos.pro.ui.common.rememberReceiptSharer
import id.umkm.pos.pro.ui.more.MoreRoute
import id.umkm.pos.pro.ui.navigation.PosDestination
import id.umkm.pos.pro.ui.products.ProductPalette
import id.umkm.pos.pro.ui.products.ProductRadii
import id.umkm.pos.pro.ui.products.ProductsRoute
import id.umkm.pos.pro.ui.security.LocalSecurityGate
import id.umkm.pos.pro.ui.security.SecurityGateHost
import id.umkm.pos.pro.ui.security.rememberSecurityGateController
import id.umkm.pos.pro.ui.shift.ShiftHost
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.welcome.WelcomeScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import id.umkm.pos.pro.core.model.AppLanguage
import id.umkm.pos.pro.ui.common.LocalCurrentLanguage
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.stringsFor
import kotlinx.coroutines.flow.MutableStateFlow


const val COMPACT_NAVIGATION_TEST_TAG = "compact-navigation-bar"
const val EXPANDED_NAVIGATION_TEST_TAG = "expanded-navigation-rail"

private const val BOTTOM_NAVIGATION_LABEL_SP = 11f
private const val BOTTOM_NAVIGATION_LABEL_MAX_SP = 12f
private const val COMPACT_NAVIGATION_SELECTED_WEIGHT = 1.7f
private const val COMPACT_NAVIGATION_INACTIVE_ICON_ALPHA = 0.7f

private val CompactNavigationHorizontalMargin = 14.dp
private val CompactNavigationBottomMargin = 8.dp
private val CompactNavigationItemMinHeight = 44.dp
private val CompactNavigationPillShape = RoundedCornerShape(26.dp)
private val CompactNavigationShadowElevation = 12.dp
private val NavigationRailWidth = 88.dp
private val NavigationRailEdgeMargin = 14.dp

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val onboardingStore = container.onboardingStore
    var hasCompletedOnboarding by rememberSaveable {
        mutableStateOf(onboardingStore.hasCompletedOnboarding())
    }
    androidx.compose.animation.AnimatedContent(
        targetState = hasCompletedOnboarding,
        transitionSpec = {
            androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(280),
            ) togetherWith androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(200),
            )
        },
        modifier = modifier.fillMaxSize(),
        label = "onboardingToMainAppTransition",
    ) { completed ->
        if (!completed) {
            WelcomeScreen(
                onStart = {
                    onboardingStore.markOnboardingCompleted()
                    hasCompletedOnboarding = true
                },
                modifier = Modifier.fillMaxSize(),
            )
            return@AnimatedContent
        }
        var selectedDestination by rememberSaveable { mutableStateOf(PosDestination.KASIR) }
        var openStaffDirectory by rememberSaveable { mutableStateOf(false) }
        val connectivityMonitor = rememberAndroidConnectivityMonitor()
        val shareReceipt = rememberReceiptSharer()
        var isFullScreenContent by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val salesScope = rememberCoroutineScope()
        val downloadsSink = remember(context) { DownloadsFileSink(context.applicationContext) }
        val settings by container.settingsRepository.observeSettings().collectAsState(initial = null)
        val currentLanguage = remember(settings?.languageCode) {
            AppLanguage.fromCode(settings?.languageCode)
        }
        val posStrings = remember(currentLanguage) {
            stringsFor(currentLanguage)
        }
        val posStringsFlow = remember(container) { MutableStateFlow(posStrings) }
        SideEffect {
            posStringsFlow.value = posStrings
        }
        val salesPresenter = remember(container, downloadsSink, salesScope, posStringsFlow) {
            ReportsPresenter(
                reportRepository = container.reportRepository,
                transactionRepository = container.transactionRepository,
                productRepository = container.productRepository,
                settingsRepository = container.settingsRepository,
                csvExporter = SalesCsvExporter(container.reportRepository, downloadsSink),
                xlsxExporter = XlsxReportExporter(downloadsSink),
                adController = container.adController,
                coroutineScope = salesScope,
                clock = container.clock,
                stringsFlow = posStringsFlow,
            )
        }

        val securityGate = rememberSecurityGateController(
            securityService = container.securityService,
            clock = container.clock,
        )
        CompositionLocalProvider(
            LocalConnectivityMonitor provides connectivityMonitor,
            LocalCurrentLanguage provides currentLanguage,
            LocalPosStrings provides posStrings,
            LocalSecurityGate provides securityGate,
            LocalFullScreenContent provides { isFullScreen -> isFullScreenContent = isFullScreen },
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                val isExpandedWindow = maxWidth >= PosBreakpoints.expandedNavigation
                ShiftHost(
                    onManageStaff = {
                        openStaffDirectory = true
                        selectedDestination = PosDestination.LAINNYA
                    },
                    snackbarBottomPadding = if (isExpandedWindow) PosSpacing.md else 96.dp,
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
                        bottomBar = {
                            if (!isExpandedWindow && !isFullScreenContent) {
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
                                salesPresenter = salesPresenter,
                                onShareReceipt = shareReceipt,
                                onOpenCashier = { selectedDestination = PosDestination.KASIR },
                                openStaffDirectory = openStaffDirectory,
                                onOpenStaffDirectoryConsumed = { openStaffDirectory = false },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .imePadding(),
                            )
                        }
                    }
                }

                SecurityGateHost(securityGate)
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
            .padding(
                start = CompactNavigationHorizontalMargin,
                end = CompactNavigationHorizontalMargin,
                bottom = CompactNavigationBottomMargin,
            ),
    ) {
        Surface(
            color = ProductPalette.NavSurface,
            contentColor = Color.White,
            shape = ProductRadii.navigation,
            shadowElevation = CompactNavigationShadowElevation,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(COMPACT_NAVIGATION_TEST_TAG),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(horizontal = PosSpacing.xs, vertical = PosSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PosDestination.entries.forEach { destination ->
                    val isSelected = destination == selectedDestination
                    BottomNavigationItem(
                        destination = destination,
                        isSelected = isSelected,
                        onClick = { onDestinationSelected(destination) },
                        modifier = Modifier.weight(
                            if (isSelected) COMPACT_NAVIGATION_SELECTED_WEIGHT else 1f,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationItem(
    destination: PosDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = destination.localizedLabel(LocalPosStrings.current)
    val fontScale = LocalDensity.current.fontScale
    val labelFontSize = minOf(BOTTOM_NAVIGATION_LABEL_SP, BOTTOM_NAVIGATION_LABEL_MAX_SP / fontScale)
    Box(
        modifier = modifier
            .heightIn(min = CompactNavigationItemMinHeight)
            .then(
                if (isSelected) {
                    Modifier
                        .clip(CompactNavigationPillShape)
                        .background(Color.White)
                } else {
                    Modifier
                },
            )
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Row(
                modifier = Modifier.padding(horizontal = PosSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = null,
                    tint = ProductPalette.Ink,
                    modifier = Modifier.size(PosSizes.iconMedium),
                )
                Text(
                    text = label,
                    color = ProductPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = labelFontSize.sp,
                        letterSpacing = 0.sp,
                    ),
                )
            }
        } else {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = label,
                tint = Color.White.copy(alpha = COMPACT_NAVIGATION_INACTIVE_ICON_ALPHA),
                modifier = Modifier.size(PosSizes.iconMedium),
            )
        }
    }
}

@Composable
private fun PrimaryNavigationRail(
    selectedDestination: PosDestination,
    onDestinationSelected: (PosDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                start = NavigationRailEdgeMargin,
                top = NavigationRailEdgeMargin,
                bottom = NavigationRailEdgeMargin,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Surface(
            color = ProductPalette.NavSurface,
            contentColor = Color.White,
            shape = ProductRadii.navigation,
            shadowElevation = CompactNavigationShadowElevation,
            modifier = Modifier
                .width(NavigationRailWidth)
                .testTag(EXPANDED_NAVIGATION_TEST_TAG),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(horizontal = PosSpacing.xs, vertical = PosSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PosDestination.entries.forEach { destination ->
                    RailNavigationItem(
                        destination = destination,
                        isSelected = destination == selectedDestination,
                        onClick = { onDestinationSelected(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RailNavigationItem(
    destination: PosDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = destination.localizedLabel(LocalPosStrings.current)
    val fontScale = LocalDensity.current.fontScale
    val labelFontSize = minOf(BOTTOM_NAVIGATION_LABEL_SP, BOTTOM_NAVIGATION_LABEL_MAX_SP / fontScale)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CompactNavigationItemMinHeight)
            .then(
                if (isSelected) {
                    Modifier
                        .clip(CompactNavigationPillShape)
                        .background(Color.White)
                } else {
                    Modifier
                },
            )
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Column(
                modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = null,
                    tint = ProductPalette.Ink,
                    modifier = Modifier.size(PosSizes.iconMedium),
                )
                Text(
                    text = label,
                    color = ProductPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = labelFontSize.sp,
                        letterSpacing = 0.sp,
                    ),
                )
            }
        } else {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = label,
                tint = Color.White.copy(alpha = COMPACT_NAVIGATION_INACTIVE_ICON_ALPHA),
                modifier = Modifier.size(PosSizes.iconMedium),
            )
        }
    }
}

@Composable
private fun DestinationHost(
    destination: PosDestination,
    salesPresenter: ReportsPresenter,
    onShareReceipt: (String) -> Unit,
    onOpenCashier: () -> Unit,
    openStaffDirectory: Boolean,
    onOpenStaffDirectoryConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        PosDestination.KASIR -> CashierRoute(onShareReceipt = onShareReceipt, modifier = modifier)
        PosDestination.PRODUK -> ProductsRoute(modifier = modifier)
        PosDestination.TRANSAKSI -> TransactionsRoute(
            presenter = salesPresenter,
            onStartSale = onOpenCashier,
            onShareReceiptText = onShareReceipt,
            modifier = modifier,
        )
        PosDestination.LAPORAN -> ReportsRoute(presenter = salesPresenter, modifier = modifier)
        PosDestination.LAINNYA -> MoreRoute(
            onOpenCashier = onOpenCashier,
            openStaffDirectory = openStaffDirectory,
            onOpenStaffDirectoryConsumed = onOpenStaffDirectoryConsumed,
            modifier = modifier,
        )
    }
}

fun PosDestination.localizedLabel(strings: PosStrings): String = when (this) {
    PosDestination.KASIR -> strings.navCashier
    PosDestination.PRODUK -> strings.navProducts
    PosDestination.TRANSAKSI -> strings.navTransactions
    PosDestination.LAPORAN -> strings.navReports
    PosDestination.LAINNYA -> strings.navMore
}

