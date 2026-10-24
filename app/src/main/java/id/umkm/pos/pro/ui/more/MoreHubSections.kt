package id.umkm.pos.pro.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CashierShift
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.components.hub.StoreIdentityMark
import id.umkm.pos.pro.ui.shift.LocalShiftLauncher
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val HERO_SHADOW_ELEVATION = 1.dp
private val HERO_ACTION_BORDER_WIDTH = 1.dp
private val CASH_VALUE_MAX_WIDTH = 200.dp
private val CASH_LOADING_INDICATOR_SIZE = 16.dp
private val CASH_LOADING_STROKE_WIDTH = 2.dp
private val EMPTY_DETAIL_ICON_CONTAINER_SIZE = 56.dp
private val EMPTY_DETAIL_ICON_SIZE = 24.dp

@Composable
fun MoreHeroSection(
    storeName: String?,
    cashierLabel: String?,
    taxRatePercent: Int,
    isStoreLoading: Boolean,
    activeShift: CashierShift?,
    operatorName: String?,
    expectedCash: ExpectedCashState,
    onShiftAction: () -> Unit,
    onOpenHistory: () -> Unit,
    onRetryExpectedCash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = HERO_SHADOW_ELEVATION,
        modifier = modifier
            .fillMaxWidth()
            .testTag(MoreHubTestTags.HERO),
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            StoreRow(
                storeName = storeName,
                cashierLabel = cashierLabel,
                taxRatePercent = taxRatePercent,
                isStoreLoading = isStoreLoading,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ShiftRow(activeShift = activeShift, operatorName = operatorName)
            if (activeShift != null) {
                ExpectedCashRow(expectedCash = expectedCash, onRetryExpectedCash = onRetryExpectedCash)
            }
            HeroActions(
                activeShift = activeShift,
                onShiftAction = onShiftAction,
                onOpenHistory = onOpenHistory,
            )
        }
    }
}

@Composable
fun MoreEmptyDetail(modifier: Modifier = Modifier) {
    val strings = LocalPosStrings.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(MoreHubTestTags.EMPTY_DETAIL),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(EMPTY_DETAIL_ICON_CONTAINER_SIZE)
                    .background(
                        color = LocalPosSemanticPalette.current.neutralContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_store),
                    contentDescription = null,
                    modifier = Modifier.size(EMPTY_DETAIL_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = strings.moreSelectPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StoreRow(
    storeName: String?,
    cashierLabel: String?,
    taxRatePercent: Int,
    isStoreLoading: Boolean,
) {
    val strings = LocalPosStrings.current
    val storeLabel = if (isStoreLoading) {
        strings.moreStoreLoading
    } else {
        storeName?.takeIf { it.isNotBlank() } ?: strings.moreStoreUnnamed
    }
    val storeSubtitle = if (isStoreLoading) {
        null
    } else if (cashierLabel.isNullOrBlank()) {
        strings.moreTaxSummary(taxRatePercent)
    } else {
        "$cashierLabel • ${strings.moreTaxSummary(taxRatePercent)}"
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoreIdentityMark(storeName = storeName)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
        ) {
            Text(
                text = storeLabel,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (storeSubtitle != null) {
                Text(
                    text = storeSubtitle,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ShiftRow(activeShift: CashierShift?, operatorName: String?) {
    val strings = LocalPosStrings.current
    val shiftSubtitle = if (activeShift == null) {
        strings.moreShiftEmptySubtitle
    } else {
        strings.moreShiftActiveSubtitle(
            operatorName?.takeIf { it.isNotBlank() } ?: activeShift.cashierName,
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(iconRes = R.drawable.ic_clock)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
        ) {
            Text(
                text = strings.moreShiftTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = shiftSubtitle,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpectedCashRow(expectedCash: ExpectedCashState, onRetryExpectedCash: () -> Unit) {
    when (expectedCash) {
        ExpectedCashState.Loading -> ExpectedCashLoadingRow()
        is ExpectedCashState.Ready -> ExpectedCashReadyRow(amount = expectedCash.amount)
        ExpectedCashState.Error -> ExpectedCashErrorRow(onRetryExpectedCash = onRetryExpectedCash)
    }
}

@Composable
private fun ExpectedCashLoadingRow() {
    val strings = LocalPosStrings.current
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
        CircularProgressIndicator(
            modifier = Modifier.size(CASH_LOADING_INDICATOR_SIZE),
            strokeWidth = CASH_LOADING_STROKE_WIDTH,
        )
        Text(
            text = strings.shiftSummaryLoading,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpectedCashReadyRow(amount: Long) {
    val strings = LocalPosStrings.current
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
            text = Rupiah.format(amount),
            style = LocalAppTextStyles.current.financialValue,
            modifier = Modifier
                .widthIn(max = CASH_VALUE_MAX_WIDTH)
                .testTag(MoreHubTestTags.CASH_VALUE),
        )
    }
}

@Composable
private fun ExpectedCashErrorRow(onRetryExpectedCash: () -> Unit) {
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
        Text(
            text = strings.moreShiftExpectedCash,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.moreCashLoadFailed,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .weight(1f)
                    .testTag(MoreHubTestTags.CASH_ERROR),
                maxLines = 2,
            )
            TextButton(
                onClick = onRetryExpectedCash,
                modifier = Modifier.testTag(MoreHubTestTags.CASH_RETRY),
            ) {
                Text(
                    text = strings.actionRetry,
                    style = LocalAppTextStyles.current.buttonLabel,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroActions(
    activeShift: CashierShift?,
    onShiftAction: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val shiftLauncher = LocalShiftLauncher.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        OutlinedButton(
            onClick = {
                onShiftAction()
                if (activeShift == null) {
                    shiftLauncher.openQuickShift()
                }
            },
            shape = PosShapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(
                HERO_ACTION_BORDER_WIDTH,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            ),
            contentPadding = PaddingValues(horizontal = PosSpacing.md),
            modifier = Modifier
                .heightIn(min = PosSizes.touchTarget)
                .testTag(MoreHubTestTags.SHIFT_ACTION),
        ) {
            Text(
                text = if (activeShift == null) {
                    strings.cashierShiftOpenAction
                } else {
                    strings.cashierShiftSwitchAction
                },
                style = LocalAppTextStyles.current.buttonLabel,
            )
        }
        SecondaryTextAction(
            text = strings.shiftHistoryAction,
            onClick = onOpenHistory,
            modifier = Modifier.testTag(MoreHubTestTags.HISTORY_ACTION),
        )
    }
}
