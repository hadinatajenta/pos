package id.umkm.pos.pro.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.rememberImagePicker
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.components.rememberFileImageBitmap
import id.umkm.pos.pro.ui.security.SecurityGate
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val QRIS_PREVIEW_HEIGHT = 220.dp

@Composable
internal fun SettingsQrisScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val pickQrisImage = rememberImagePicker(onImagePicked = actions.onQrisImagePicked)
    val isConfigured = state.isQrisConfigured
    val gate = rememberSecurityGate()

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.settingsQrisTitle,
            onBack = onBack,
            actions = {
                QrisStatusBadge(isConfigured = isConfigured)
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PosSpacing.md,
                    end = PosSpacing.md,
                    top = PosSpacing.sm,
                    bottom = PosSizes.listBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            SettingsFeedbackBanner(state = state)

            PosCard(modifier = Modifier.fillMaxWidth()) {
                QrisConditionStatusBox(isConfigured = isConfigured)

                if (state.isImportingQris) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = strings.settingsQrisScanning,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (isConfigured) {
                    QrisConfiguredContent(
                        state = state,
                        actions = actions,
                        onPickImage = pickQrisImage,
                        gate = gate,
                    )
                } else {
                    QrisNotConfiguredUploadZone(
                        isImporting = state.isImportingQris,
                        onPickImage = pickQrisImage,
                    )
                }
            }
        }
    }
}

@Composable
private fun QrisConditionStatusBox(isConfigured: Boolean) {
    val palette = LocalPosSemanticPalette.current
    val strings = LocalPosStrings.current
    val containerColor = if (isConfigured) palette.successContainer.copy(alpha = 0.65f) else palette.warningContainer.copy(alpha = 0.65f)
    val accentColor = if (isConfigured) palette.success else palette.warning
    val iconRes = if (isConfigured) R.drawable.ic_circle_check else R.drawable.ic_triangle_alert
    val statusText = if (isConfigured) {
        strings.settingsQrisActiveDetail
    } else {
        strings.settingsQrisEmptyDetail
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QrisNotConfiguredUploadZone(
    isImporting: Boolean,
    onPickImage: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = !isImporting, onClick = onPickImage),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PosSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = strings.settingsQrisUploadTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            Text(
                text = strings.settingsQrisUploadHint,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(PosSpacing.xs))
            PrimaryActionButton(
                text = strings.settingsQrisPickImage,
                iconRes = R.drawable.ic_cloud_upload,
                loading = isImporting,
                enabled = !isImporting,
                onClick = onPickImage,
            )
            Text(
                text = strings.settingsQrisUploadTips,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QrisConfiguredContent(
    state: SettingsUiState,
    actions: SettingsActions,
    onPickImage: () -> Unit,
    gate: SecurityGate,
) {
    val palette = LocalPosSemanticPalette.current
    val strings = LocalPosStrings.current
    val preview = rememberFileImageBitmap(state.settings?.qrisImagePath)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Surface(
            color = palette.neutralContainer.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PosSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Text(
                    text = strings.settingsQrisSavedPreview,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(QRIS_PREVIEW_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            contentDescription = strings.settingsQrisSavedDescription,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(PosSpacing.sm),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                            modifier = Modifier.padding(PosSpacing.md),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_circle_check),
                                contentDescription = null,
                                tint = palette.success,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = strings.settingsQrisPayloadTitle,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = strings.settingsQrisPayloadHint,
                                style = LocalAppTextStyles.current.supportingLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            OutlinedButton(
                onClick = onPickImage,
                enabled = !state.isImportingQris,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = PosSpacing.sm,
                    vertical = PosSpacing.xs,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PosSizes.touchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(
                    text = strings.settingsQrisChangeImage,
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
            OutlinedButton(
                onClick = {
                    gate.request(CriticalAction.REMOVE_QRIS, strings.settingsQrisRemove) {
                        actions.onRemoveQris()
                    }
                },
                enabled = !state.isImportingQris,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = palette.danger,
                ),
                border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.4f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = PosSpacing.sm,
                    vertical = PosSpacing.xs,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PosSizes.touchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(
                    text = strings.settingsQrisRemove,
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
        }
    }
}
