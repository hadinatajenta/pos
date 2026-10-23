package id.umkm.pos.pro.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.components.hub.HubGroup
import id.umkm.pos.pro.ui.components.hub.HubRow
import id.umkm.pos.pro.ui.components.hub.HubSectionLabel
import id.umkm.pos.pro.ui.customers.CustomersRoute
import id.umkm.pos.pro.ui.settings.SettingsRoute
import id.umkm.pos.pro.ui.shift.LocalShiftLauncher
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import kotlinx.coroutines.CancellationException

private enum class MoreDestination { SETTINGS, CUSTOMERS }

@Composable
fun MoreRoute(
    modifier: Modifier = Modifier,
    onOpenCashier: () -> Unit = {},
    openStaffDirectory: Boolean = false,
    onOpenStaffDirectoryConsumed: () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.observeSettings().collectAsState(initial = null)
    val customers by container.customerRepository.observeCustomers(null).collectAsState(initial = emptyList())
    val activeShift by remember(container) {
        container.shiftRepository.observeActiveShift()
    }.collectAsState(initial = null)
    val activeStaff by remember(container) {
        container.staffRepository.observeActiveStaff()
    }.collectAsState(initial = null)

    var expectedCashState by remember { mutableStateOf<ExpectedCashState>(ExpectedCashState.Loading) }
    var cashRetryKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(activeShift?.id, cashRetryKey) {
        val shift = activeShift
        expectedCashState = ExpectedCashState.Loading
        if (shift != null) {
            expectedCashState = try {
                when (val shiftSummaryResult = container.shiftRepository.getShiftSummary(shift.id)) {
                    is AppResult.Success -> ExpectedCashState.Ready(shiftSummaryResult.value.expectedEndingCash)
                    is AppResult.Failure -> ExpectedCashState.Error
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                ExpectedCashState.Error
            }
        }
    }

    var activeDetail by rememberSaveable { mutableStateOf<MoreDestination?>(null) }
    val onBackToMenu = { activeDetail = null }

    LaunchedEffect(openStaffDirectory) {
        if (openStaffDirectory) {
            activeDetail = MoreDestination.SETTINGS
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= PosBreakpoints.twoPane) {
            val hubWidth = (maxWidth * 0.38f).coerceIn(320.dp, 440.dp)
            Row(modifier = Modifier.fillMaxSize()) {
                MoreHubColumn(
                    settings = settings,
                    customers = customers,
                    activeShift = activeShift,
                    activeStaff = activeStaff,
                    expectedCash = expectedCashState,
                    selectedDestination = activeDetail,
                    onSelectDestination = { destination -> activeDetail = destination },
                    onRetryExpectedCash = { cashRetryKey++ },
                    onOpenCashier = onOpenCashier,
                    modifier = Modifier
                        .width(hubWidth)
                        .fillMaxHeight(),
                )
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxHeight(),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    when (activeDetail) {
                        null -> MoreEmptyDetail()
                        MoreDestination.CUSTOMERS -> CustomersRoute(
                            onBack = null,
                            modifier = Modifier.fillMaxSize(),
                        )

                        MoreDestination.SETTINGS -> SettingsRoute(
                            onBack = null,
                            openStaffDirectory = openStaffDirectory,
                            onOpenStaffDirectoryConsumed = onOpenStaffDirectoryConsumed,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else {
            when (activeDetail) {
                MoreDestination.CUSTOMERS -> MoreDetailScreen(onBack = onBackToMenu) {
                    CustomersRoute(
                        onBack = onBackToMenu,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                MoreDestination.SETTINGS -> MoreDetailScreen(onBack = onBackToMenu) {
                    SettingsRoute(
                        onBack = onBackToMenu,
                        openStaffDirectory = openStaffDirectory,
                        onOpenStaffDirectoryConsumed = onOpenStaffDirectoryConsumed,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                null -> MoreHubColumn(
                    settings = settings,
                    customers = customers,
                    activeShift = activeShift,
                    activeStaff = activeStaff,
                    expectedCash = expectedCashState,
                    selectedDestination = null,
                    onSelectDestination = { destination -> activeDetail = destination },
                    onRetryExpectedCash = { cashRetryKey++ },
                    onOpenCashier = onOpenCashier,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun MoreDetailScreen(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onBack)
    content()
}

@Composable
private fun MoreHubColumn(
    settings: AppSettings?,
    customers: List<Customer>,
    activeShift: CashierShift?,
    activeStaff: Staff?,
    expectedCash: ExpectedCashState,
    selectedDestination: MoreDestination?,
    onSelectDestination: (MoreDestination) -> Unit,
    onRetryExpectedCash: () -> Unit,
    onOpenCashier: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current

    Column(modifier = modifier) {
        ScreenHeader(title = strings.navMore, supportingText = strings.moreHeaderSupporting)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PosSpacing.md,
                    end = PosSpacing.md,
                    top = PosSpacing.xs,
                    bottom = PosSizes.listBottomPadding,
                )
                .testTag(MoreHubTestTags.HUB),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.lg),
        ) {
            MoreHero(
                settings = settings,
                activeShift = activeShift,
                activeStaff = activeStaff,
                expectedCash = expectedCash,
                onShiftAction = onOpenCashier,
                onRetryExpectedCash = onRetryExpectedCash,
            )
            MoreManageSection(
                customers = customers,
                selectedDestination = selectedDestination,
                onSelectDestination = onSelectDestination,
            )
            DemoDataSection()
        }
    }
}

@Composable
private fun MoreHero(
    settings: AppSettings?,
    activeShift: CashierShift?,
    activeStaff: Staff?,
    expectedCash: ExpectedCashState,
    onShiftAction: () -> Unit,
    onRetryExpectedCash: () -> Unit,
) {
    val shiftLauncher = LocalShiftLauncher.current
    val storeName = settings?.storeName?.takeIf { it.isNotBlank() }
    val cashierLabel = if (activeShift != null) {
        null
    } else {
        settings?.cashierName?.takeIf { it.isNotBlank() }
    }
    val operatorName = if (activeShift == null) {
        null
    } else {
        activeStaff?.name?.takeIf { it.isNotBlank() } ?: activeShift.cashierName
    }

    MoreHeroSection(
        storeName = storeName,
        cashierLabel = cashierLabel,
        taxRatePercent = settings?.taxRatePercent ?: 0,
        isStoreLoading = settings == null,
        activeShift = activeShift,
        operatorName = operatorName,
        expectedCash = expectedCash,
        onShiftAction = onShiftAction,
        onOpenHistory = shiftLauncher::openHistory,
        onRetryExpectedCash = onRetryExpectedCash,
    )
}

@Composable
private fun MoreManageSection(
    customers: List<Customer>,
    selectedDestination: MoreDestination?,
    onSelectDestination: (MoreDestination) -> Unit,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val unpaidCount = customers.count { it.totalDebt > 0L }
    val customersDescription = if (customers.isEmpty()) {
        strings.moreCustomersEmptyDescription
    } else {
        strings.moreCustomersDescription
    }

    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
        HubSectionLabel(text = strings.moreSectionManage)
        HubGroup {
            HubRow(
                title = strings.moreHubSettings,
                description = strings.moreSettingsDescription,
                iconRes = R.drawable.ic_settings,
                onClick = { onSelectDestination(MoreDestination.SETTINGS) },
                isSelected = selectedDestination == MoreDestination.SETTINGS,
                modifier = Modifier.testTag(MoreHubTestTags.SETTINGS_ROW),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = PosSpacing.md),
            )
            HubRow(
                title = strings.moreHubCustomers,
                description = customersDescription,
                iconRes = R.drawable.ic_users,
                onClick = { onSelectDestination(MoreDestination.CUSTOMERS) },
                isSelected = selectedDestination == MoreDestination.CUSTOMERS,
                modifier = Modifier.testTag(MoreHubTestTags.CUSTOMERS_ROW),
                badge = {
                    if (unpaidCount > 0) {
                        StatusChip(
                            iconRes = R.drawable.ic_clock,
                            label = strings.moreKasbonCount(unpaidCount),
                            containerColor = palette.warningContainer,
                            contentColor = palette.warning,
                        )
                    }
                },
            )
        }
    }
}
