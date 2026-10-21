package id.umkm.pos.pro.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.HeaderActionButton
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.MenuListRow
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.customers.CustomersRoute
import id.umkm.pos.pro.ui.settings.SettingsRoute
import id.umkm.pos.pro.ui.shift.LocalShiftLauncher
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import kotlinx.coroutines.CancellationException


private enum class MoreDestination { SETTINGS, CUSTOMERS }

@Composable
fun MoreRoute(
    onOpenCashier: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.observeSettings().collectAsState(initial = null)
    val customers by container.customerRepository.observeCustomers(null).collectAsState(initial = emptyList())
    val activeShift by remember(container) {
        container.shiftRepository.observeActiveShift()
    }.collectAsState(initial = null)
    var expectedCash by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(activeShift?.id) {
        expectedCash = null
        expectedCash = try {
            activeShift?.let { shift ->
                when (val result = container.shiftRepository.getShiftSummary(shift.id)) {
                    is AppResult.Success -> result.value.expectedEndingCash
                    is AppResult.Failure -> null
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            null
        }
    }

    var activeDetail by rememberSaveable { mutableStateOf<MoreDestination?>(null) }
    val onBackToMenu = { activeDetail = null }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= PosBreakpoints.twoPane
        if (isTwoPane) {
            val selectedPane = activeDetail ?: MoreDestination.SETTINGS
            Row(modifier = Modifier.fillMaxSize()) {
                MoreHubColumn(
                    settings = settings,
                    customers = customers,
                    activeShift = activeShift,
                    expectedCash = expectedCash,
                    selectedDestination = selectedPane,
                    onSelectDestination = { destination -> activeDetail = destination },
                    onOpenCashier = onOpenCashier,
                    modifier = Modifier
                        .width(360.dp)
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
                    when (selectedPane) {
                        MoreDestination.CUSTOMERS -> CustomersRoute(
                            onBack = null,
                            modifier = Modifier.fillMaxSize(),
                        )

                        MoreDestination.SETTINGS -> SettingsRoute(
                            onBack = null,
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                null -> MoreHubColumn(
                    settings = settings,
                    customers = customers,
                    activeShift = activeShift,
                    expectedCash = expectedCash,
                    selectedDestination = null,
                    onSelectDestination = { destination -> activeDetail = destination },
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
    expectedCash: Long?,
    selectedDestination: MoreDestination?,
    onSelectDestination: (MoreDestination) -> Unit,
    onOpenCashier: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val customerCount = customers.size
    val totalReceivable = customers.sumOf { it.totalDebt }

    Column(modifier = modifier) {
        ScreenHeader(title = strings.navMore)
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
                ),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            StoreIdentityHeroCard(
                settings = settings,
                activeShift = activeShift,
                onOpenSettings = { onSelectDestination(MoreDestination.SETTINGS) },
            )

            ShiftCard(
                activeShift = activeShift,
                expectedCash = expectedCash,
                onOpenCashier = onOpenCashier,
            )

            MenuListRow(
                title = strings.moreHubSettings,
                iconRes = R.drawable.ic_settings,
                isSelected = selectedDestination == MoreDestination.SETTINGS,
                onClick = { onSelectDestination(MoreDestination.SETTINGS) },
            )

            MenuListRow(
                title = strings.moreHubCustomers,
                iconRes = R.drawable.ic_users,
                isSelected = selectedDestination == MoreDestination.CUSTOMERS,

                badge = {
                    if (totalReceivable > 0L) {
                        val unpaidCount = customers.count { it.totalDebt > 0L }
                        StatusChip(
                            iconRes = R.drawable.ic_clock,
                            label = strings.moreKasbonCount(unpaidCount),
                            containerColor = palette.warningContainer,
                            contentColor = palette.warning,
                        )
                    } else {
                        StatusChip(
                            iconRes = R.drawable.ic_users,
                            label = strings.moreCustomerCount(customerCount),
                            containerColor = palette.neutralContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = { onSelectDestination(MoreDestination.CUSTOMERS) },
            )

            DemoDataSection()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShiftCard(
    activeShift: CashierShift?,
    expectedCash: Long?,
    onOpenCashier: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val shiftLauncher = LocalShiftLauncher.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            IconBadge(iconRes = R.drawable.ic_users)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = strings.moreShiftTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (activeShift == null) {
                        strings.moreShiftEmptySubtitle
                    } else {
                        strings.moreShiftActiveSubtitle(activeShift.cashierName)
                    },
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (activeShift != null) {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Text(
                    text = strings.moreShiftExpectedCash,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                AutoSizeNumberText(
                    text = expectedCash?.let(Rupiah::format) ?: strings.shiftSummaryLoading,
                    style = LocalAppTextStyles.current.financialValue,
                    modifier = Modifier.widthIn(max = 176.dp),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            HeaderActionButton(
                text = strings.navCashier,
                iconRes = R.drawable.ic_users,
                onClick = onOpenCashier,
            )
            SecondaryTextAction(
                text = strings.shiftHistoryAction,
                onClick = shiftLauncher::openHistory,
            )
        }
    }
}

@Composable
private fun StoreIdentityHeroCard(
    settings: AppSettings?,
    activeShift: CashierShift?,
    onOpenSettings: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val storeName = settings?.storeName?.takeIf { it.isNotBlank() } ?: strings.moreDefaultStoreName
    val cashierName = activeShift?.cashierName?.takeIf { it.isNotBlank() }
        ?: settings?.cashierName?.takeIf { it.isNotBlank() }
        ?: strings.moreDefaultCashierName
    val taxRate = settings?.taxRatePercent ?: 0

    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenSettings),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            ProductThumbnail(
                category = "toko",
                productName = storeName,
                size = 48.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = storeName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$cashierName • ${strings.moreTaxSummary(taxRate)}",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

