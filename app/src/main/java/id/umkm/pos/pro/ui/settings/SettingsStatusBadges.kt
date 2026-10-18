package id.umkm.pos.pro.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.core.model.AppLanguage
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette

@Composable
internal fun LanguageBadge(language: AppLanguage) {
    val palette = LocalPosSemanticPalette.current
    StatusChip(
        iconRes = R.drawable.ic_shapes,
        label = language.nativeName,
        containerColor = palette.neutralContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun PrinterConnectionStatusBadge(connection: PrinterConnection) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    when (connection) {
        PrinterConnection.CONNECTED -> StatusChip(
            iconRes = R.drawable.ic_circle_check,
            label = strings.settingsPrinterStatusConnected,
            containerColor = palette.successContainer,
            contentColor = palette.success,
        )
        PrinterConnection.CONNECTING -> StatusChip(
            iconRes = R.drawable.ic_clock,
            label = strings.settingsPrinterStatusConnecting,
            containerColor = palette.warningContainer,
            contentColor = palette.warning,
        )
        PrinterConnection.DISCONNECTED -> StatusChip(
            iconRes = R.drawable.ic_triangle_alert,
            label = strings.settingsPrinterStatusDisconnected,
            containerColor = palette.neutralContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun QrisStatusBadge(isConfigured: Boolean) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    if (isConfigured) {
        StatusChip(
            iconRes = R.drawable.ic_circle_check,
            label = strings.settingsQrisInstalled,
            containerColor = palette.successContainer,
            contentColor = palette.success,
        )
    } else {
        StatusChip(
            iconRes = R.drawable.ic_triangle_alert,
            label = strings.settingsQrisNotConfigured,
            containerColor = palette.warningContainer,
            contentColor = palette.warning,
        )
    }
}

@Composable
internal fun StoreTaxBadge(taxRatePercent: Int) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    StatusChip(
        iconRes = R.drawable.ic_banknote,
        label = strings.settingsMenuStoreTax(taxRatePercent),
        containerColor = palette.neutralContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
