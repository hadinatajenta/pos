package id.umkm.pos.pro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun SettingsScannerScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.settingsScannerTitle,
            onBack = onBack,
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
            PosCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                ) {
                    IconBadge(iconRes = R.drawable.ic_scan_barcode)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                    ) {
                        Text(
                            text = strings.settingsScannerCameraSection,
                            style = LocalAppTextStyles.current.sectionHeading,
                        )
                        StatusChip(
                            iconRes = R.drawable.ic_circle_check,
                            label = strings.scannerCameraAvailable,
                            containerColor = palette.successContainer,
                            contentColor = palette.successText,
                        )
                    }
                }
                Text(
                    text = strings.scannerPermissionDetail,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PosCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                ) {
                    IconBadge(iconRes = R.drawable.ic_package)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsScannerHidSection,
                            style = LocalAppTextStyles.current.sectionHeading,
                        )
                        Text(
                            text = strings.settingsScannerHidToggle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                    Switch(
                        checked = state.isHidScannerEnabled,
                        onCheckedChange = actions.onSetHidScannerEnabled,
                    )
                }
                Text(
                    text = strings.settingsScannerHidDetail,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = strings.settingsScannerHidToggleDetail,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = PosSpacing.xs))

                Text(
                    text = strings.settingsScannerTestSection,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    text = strings.settingsScannerTestHint,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ScanTestResult(lastScannedCode = state.lastScannedCode)
            }
        }
    }
}

@Composable
private fun ScanTestResult(lastScannedCode: String?) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    if (lastScannedCode == null) {
        Text(
            text = strings.settingsScannerTestEmpty,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_circle_check),
            contentDescription = null,
            tint = palette.success,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Text(
            text = strings.settingsScannerTestResult(lastScannedCode),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = palette.successText,
            modifier = Modifier.weight(1f),
        )
    }
}
