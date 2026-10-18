package id.umkm.pos.pro.ui.more

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.customers.CustomersRoute
import id.umkm.pos.pro.ui.settings.SettingsRoute
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private enum class MoreDestination { SETTINGS, CUSTOMERS }

@Composable
fun MoreRoute(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.observeSettings().collectAsState(initial = null)
    val customers by container.customerRepository.observeCustomers(null).collectAsState(initial = emptyList())

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
                    selectedDestination = selectedPane,
                    onSelectDestination = { destination -> activeDetail = destination },
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
                    selectedDestination = null,
                    onSelectDestination = { destination -> activeDetail = destination },
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
    selectedDestination: MoreDestination?,
    onSelectDestination: (MoreDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPosSemanticPalette.current
    val isQrisConfigured = settings?.qrisPayload != null
    val customerCount = customers.size
    val totalReceivable = customers.sumOf { it.totalDebt }

    Column(modifier = modifier) {
        ScreenHeader(title = "Lainnya")
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
                onOpenSettings = { onSelectDestination(MoreDestination.SETTINGS) },
            )

            MoreHubCard(
                title = "Pengaturan",
                iconRes = R.drawable.ic_settings,
                isSelected = selectedDestination == MoreDestination.SETTINGS,
                badge = {
                    if (isQrisConfigured) {
                        StatusChip(
                            iconRes = R.drawable.ic_circle_check,
                            label = "QRIS Aktif",
                            containerColor = palette.successContainer,
                            contentColor = palette.success,
                        )
                    } else {
                        StatusChip(
                            iconRes = R.drawable.ic_triangle_alert,
                            label = "Belum Diatur",
                            containerColor = palette.warningContainer,
                            contentColor = palette.warning,
                        )
                    }
                },
                onClick = { onSelectDestination(MoreDestination.SETTINGS) },
            )

            MoreHubCard(
                title = "Pelanggan",
                iconRes = R.drawable.ic_users,
                isSelected = selectedDestination == MoreDestination.CUSTOMERS,
                badge = {
                    if (totalReceivable > 0L) {
                        val unpaidCount = customers.count { it.totalDebt > 0L }
                        StatusChip(
                            iconRes = R.drawable.ic_clock,
                            label = "$unpaidCount Kasbon",
                            containerColor = palette.warningContainer,
                            contentColor = palette.warning,
                        )
                    } else {
                        StatusChip(
                            iconRes = R.drawable.ic_users,
                            label = "$customerCount Data",
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

@Composable
private fun StoreIdentityHeroCard(
    settings: AppSettings?,
    onOpenSettings: () -> Unit,
) {
    val storeName = settings?.storeName?.takeIf { it.isNotBlank() } ?: "Toko UMKM Pro"
    val cashierName = settings?.cashierName?.takeIf { it.isNotBlank() } ?: "Kasir Utama"
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
                    text = "$cashierName • PPN $taxRate%",
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

@Composable
private fun MoreHubCard(
    title: String,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    badge: @Composable () -> Unit,
    onClick: () -> Unit,
    metaText: String? = null,
) {
    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = borderStroke,
        shadowElevation = if (isSelected) 0.dp else 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PosSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            IconBadge(iconRes = iconRes)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metaText != null) {
                    Text(
                        text = metaText,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            badge()
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
