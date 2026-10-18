package id.umkm.pos.pro.ui.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.AppLanguage
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.components.MenuListRow
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

private enum class SettingsDestination {
    LANGUAGE,
    PRINTER,
    QRIS,
    STORE,
    BACKUP,
    ABOUT,
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    var activeDestination by rememberSaveable { mutableStateOf<SettingsDestination?>(null) }

    val openDestination: (SettingsDestination) -> Unit = { destination ->
        actions.onMessageConsumed()
        activeDestination = destination
    }
    val closeDestination: () -> Unit = {
        actions.onMessageConsumed()
        activeDestination = null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= PosBreakpoints.twoPane
        BackHandler(enabled = !isTwoPane && activeDestination != null) {
            closeDestination()
        }

        if (isTwoPane) {
            val selectedDestination = activeDestination ?: SettingsDestination.LANGUAGE
            Row(modifier = Modifier.fillMaxSize()) {
                SettingsHubColumn(
                    state = state,
                    selectedDestination = selectedDestination,
                    onSelectDestination = openDestination,
                    onBack = null,
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
                    SettingsDetail(
                        destination = selectedDestination,
                        state = state,
                        actions = actions,
                        onBack = null,
                    )
                }
            }
        } else {
            val destination = activeDestination
            if (destination == null) {
                SettingsHubColumn(
                    state = state,
                    selectedDestination = null,
                    onSelectDestination = openDestination,
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                SettingsDetail(
                    destination = destination,
                    state = state,
                    actions = actions,
                    onBack = closeDestination,
                )
            }
        }
    }
}

@Composable
private fun SettingsDetail(
    destination: SettingsDestination,
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)?,
) {
    when (destination) {
        SettingsDestination.LANGUAGE -> SettingsLanguageScreen(
            state = state,
            actions = actions,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
        SettingsDestination.PRINTER -> SettingsPrinterScreen(
            state = state,
            actions = actions,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
        SettingsDestination.QRIS -> SettingsQrisScreen(
            state = state,
            actions = actions,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
        SettingsDestination.STORE -> SettingsStoreScreen(
            state = state,
            actions = actions,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
        SettingsDestination.BACKUP -> SettingsBackupScreen(
            state = state,
            actions = actions,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
        SettingsDestination.ABOUT -> SettingsAboutScreen(
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SettingsHubColumn(
    state: SettingsUiState,
    selectedDestination: SettingsDestination?,
    onSelectDestination: (SettingsDestination) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier,
) {
    val strings = LocalPosStrings.current
    val settings = state.settings
    val activeLanguage = AppLanguage.fromCode(settings?.languageCode)

    Column(modifier = modifier) {
        ScreenHeader(
            title = strings.settingsTitle,
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
                    top = PosSpacing.xs,
                    bottom = PosSizes.listBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            MenuListRow(
                title = strings.settingsLanguageTitle,
                description = strings.settingsMenuLanguageDescription,
                iconRes = R.drawable.ic_shapes,
                isSelected = selectedDestination == SettingsDestination.LANGUAGE,
                badge = { LanguageBadge(language = activeLanguage) },
                onClick = { onSelectDestination(SettingsDestination.LANGUAGE) },
            )

            MenuListRow(
                title = strings.settingsPrinterTitle,
                description = strings.settingsMenuPrinterDescription,
                iconRes = R.drawable.ic_printer,
                isSelected = selectedDestination == SettingsDestination.PRINTER,
                badge = { PrinterConnectionStatusBadge(connection = state.printerConnection) },
                onClick = { onSelectDestination(SettingsDestination.PRINTER) },
            )

            MenuListRow(
                title = strings.settingsQrisTitle,
                description = strings.settingsMenuQrisDescription,
                iconRes = R.drawable.ic_scan_barcode,
                isSelected = selectedDestination == SettingsDestination.QRIS,
                badge = { QrisStatusBadge(isConfigured = state.isQrisConfigured) },
                onClick = { onSelectDestination(SettingsDestination.QRIS) },
            )

            MenuListRow(
                title = strings.settingsStoreProfileTitle,
                description = strings.settingsMenuStoreDescription,
                iconRes = R.drawable.ic_store,
                isSelected = selectedDestination == SettingsDestination.STORE,
                badge = { StoreTaxBadge(taxRatePercent = settings?.taxRatePercent ?: 0) },
                onClick = { onSelectDestination(SettingsDestination.STORE) },
            )

            MenuListRow(
                title = strings.settingsBackupTitle,
                description = strings.settingsBackupMenuDescription,
                iconRes = R.drawable.ic_download,
                isSelected = selectedDestination == SettingsDestination.BACKUP,
                onClick = { onSelectDestination(SettingsDestination.BACKUP) },
            )

            MenuListRow(
                title = strings.settingsAboutTitle,
                description = strings.settingsAboutMenuDescription,
                iconRes = R.drawable.ic_lock,
                isSelected = selectedDestination == SettingsDestination.ABOUT,
                onClick = { onSelectDestination(SettingsDestination.ABOUT) },
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SettingsScreenPreview() {
    UmkmPosTheme {
        SettingsScreen(
            state = SettingsUiState(
                settings = AppSettings(
                    storeName = "Toko Berkah UMKM",
                    cashierName = "Mbak Dini",
                    taxRatePercent = 0,
                    roundingIncrement = 0L,
                    isPinEnabled = false,
                    printerAddress = null,
                    printerName = null,
                    paperWidthMm = 58,
                    languageCode = "id",
                    qrisImagePath = null,
                    qrisPayload = null,
                ),
            ),
            actions = SettingsActions(
                onQrisImagePicked = {},
                onRemoveQris = {},
                onMessageConsumed = {},
            ),
        )
    }
}
