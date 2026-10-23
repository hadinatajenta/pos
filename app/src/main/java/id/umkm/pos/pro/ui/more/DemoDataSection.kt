package id.umkm.pos.pro.ui.more

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.BuildConfig
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.data.seed.DemoClearSummary
import id.umkm.pos.pro.data.seed.DemoDataSeeder
import id.umkm.pos.pro.data.seed.DemoSeedSummary
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.asText
import id.umkm.pos.pro.ui.common.toUiText
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.hub.HubSectionLabel
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import kotlinx.coroutines.launch

val LocalDemoDataSeeder = staticCompositionLocalOf<DemoDataSeeder?> { null }

@Composable
internal fun DemoDataSection(modifier: Modifier = Modifier) {
    if (!BuildConfig.DEBUG) return
    val seeder = LocalDemoDataSeeder.current ?: return
    DemoDataContent(seeder = seeder, modifier = modifier)
}

@Composable
private fun DemoDataContent(seeder: DemoDataSeeder, modifier: Modifier = Modifier) {
    val strings = LocalPosStrings.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        HubSectionLabel(text = strings.moreSectionTools)
        Text(
            text = strings.moreDebugHint,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DemoDataCard(seeder = seeder)
    }
}

@Composable
private fun DemoDataCard(seeder: DemoDataSeeder, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val isLargeFont = LocalDensity.current.fontScale >= 1.3f
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<UiText?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val loadDemoData: () -> Unit = {
        scope.launch {
            isBusy = true
            statusMessage = null
            statusMessage = seeder.load().toStatusMessage(::seedLoadedMessage)
            isBusy = false
        }
    }
    val clearDemoData: () -> Unit = {
        scope.launch {
            isBusy = true
            statusMessage = null
            statusMessage = seeder.clear().toClearStatusMessage(::clearSummaryMessage)
            isBusy = false
        }
    }

    PosCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MoreHubTestTags.DEBUG_SECTION),
    ) {
        DemoCardHeader(isBusy = isBusy)
        DemoCardActions(
            isBusy = isBusy,
            isLargeFont = isLargeFont,
            onLoad = loadDemoData,
            onClearRequest = { showClearConfirm = true },
        )
        statusMessage?.let { message ->
            DemoStatusMessage(message = message)
        }
    }

    if (showClearConfirm) {
        DemoClearDialog(
            onConfirm = {
                showClearConfirm = false
                clearDemoData()
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

@Composable
private fun DemoCardHeader(isBusy: Boolean) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        IconBadge(
            iconRes = R.drawable.ic_flask,
            containerColor = palette.neutralContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = strings.demoDataTitle,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun DemoCardActions(
    isBusy: Boolean,
    isLargeFont: Boolean,
    onLoad: () -> Unit,
    onClearRequest: () -> Unit,
) {
    val strings = LocalPosStrings.current

    if (isLargeFont) {
        Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            DemoActionButton(
                text = strings.demoDataLoad,
                iconRes = R.drawable.ic_flask,
                onClick = onLoad,
                enabled = !isBusy,
                isDestructive = false,
                modifier = Modifier.fillMaxWidth(),
            )
            DemoActionButton(
                text = strings.demoDataClear,
                iconRes = R.drawable.ic_trash,
                onClick = onClearRequest,
                enabled = !isBusy,
                isDestructive = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            DemoActionButton(
                text = strings.demoDataLoad,
                iconRes = R.drawable.ic_flask,
                onClick = onLoad,
                enabled = !isBusy,
                isDestructive = false,
                modifier = Modifier.weight(1f),
            )
            DemoActionButton(
                text = strings.demoDataClear,
                iconRes = R.drawable.ic_trash,
                onClick = onClearRequest,
                enabled = !isBusy,
                isDestructive = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DemoActionButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    isDestructive: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPosSemanticPalette.current

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = if (isDestructive) {
            ButtonDefaults.outlinedButtonColors(contentColor = palette.danger)
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        border = if (isDestructive) {
            BorderStroke(1.dp, palette.danger.copy(alpha = 0.35f))
        } else {
            ButtonDefaults.outlinedButtonBorder(enabled = enabled)
        },
        contentPadding = PaddingValues(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
        modifier = modifier.heightIn(min = PosSizes.touchTarget),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(PosSpacing.xs))
        Text(
            text = text,
            style = LocalAppTextStyles.current.buttonLabel,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DemoStatusMessage(message: UiText, modifier: Modifier = Modifier) {
    val palette = LocalPosSemanticPalette.current

    Surface(
        color = palette.neutralContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = message.asText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
        )
    }
}

@Composable
private fun DemoClearDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalPosStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = strings.demoDataClearConfirmTitle) },
        text = { Text(text = strings.demoDataClearConfirmMsg) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = strings.actionDelete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = strings.actionCancel)
            }
        },
    )
}

private fun AppResult<DemoSeedSummary>.toStatusMessage(message: (DemoSeedSummary) -> UiText): UiText =
    when (this) {
        is AppResult.Success -> message(value)
        is AppResult.Failure -> error.demoDataFailureText()
    }

private fun AppResult<DemoClearSummary>.toClearStatusMessage(message: (DemoClearSummary) -> UiText): UiText =
    when (this) {
        is AppResult.Success -> message(value)
        is AppResult.Failure -> error.demoDataFailureText()
    }

private fun AppError.demoDataFailureText(): UiText =
    UiText.from { strings -> strings.demoDataFailed(toUiText().resolve(strings)) }

private fun seedLoadedMessage(summary: DemoSeedSummary): UiText =
    UiText.from { strings ->
        strings.demoDataLoaded(
            summary.products,
            summary.customers,
            summary.transactions,
            summary.returns,
        )
    }

private fun clearSummaryMessage(summary: DemoClearSummary): UiText =
    UiText.from { strings ->
        strings.demoDataCleared(summary.products, summary.customers, summary.transactions)
    }
