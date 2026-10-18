package id.umkm.pos.pro.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.PrinterConnection
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun SettingsPrinterScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val currentAddress = state.settings?.printerAddress
    val currentName = state.settings?.printerName ?: strings.settingsPrinterNoneSelected
    val paperWidth = state.settings?.paperWidthMm ?: 58
    val isConnected = state.printerConnection == PrinterConnection.CONNECTED
    val isConnecting = state.printerConnection == PrinterConnection.CONNECTING

    var isDropdownOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.settingsPrinterTitle,
            onBack = onBack,
            actions = {
                PrinterConnectionStatusBadge(connection = state.printerConnection)
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
                Surface(
                    color = palette.neutralContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PosSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.settingsPrinterSelectDevice,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = if (!currentAddress.isNullOrBlank()) "$currentName (${currentAddress})" else strings.settingsPrinterSelectPrompt,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    actions.onRefreshPrinters()
                                    isDropdownOpen = !isDropdownOpen
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = PosSpacing.sm,
                                    vertical = PosSpacing.xs,
                                ),
                                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
                            ) {
                                Text(
                                    text = if (isDropdownOpen) strings.actionClose else strings.actionPick,
                                    style = LocalAppTextStyles.current.buttonLabel,
                                )
                            }
                        }

                        if (isDropdownOpen) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = PosSpacing.xs))
                            Text(
                                text = strings.settingsPrinterDeviceListTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            state.availablePrinters.forEach { printer ->
                                val isSelected = printer.address == currentAddress
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else Color.Transparent,
                                        )
                                        .clickable {
                                            actions.onSelectPrinter(printer)
                                            isDropdownOpen = false
                                        }
                                        .padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (printer.address.startsWith("SIMULATOR")) R.drawable.ic_receipt
                                            else R.drawable.ic_printer,
                                        ),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = printer.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = if (printer.address.startsWith("SIMULATOR")) strings.settingsPrinterSimulatorLive else printer.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
                    Text(
                        text = strings.settingsPrinterPaperWidth,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                    ) {
                        FilterChip(
                            selected = paperWidth == 58,
                            onClick = { actions.onSetPaperWidth(58) },
                            label = { Text(strings.settingsPrinterWidth58) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = palette.neutralContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        FilterChip(
                            selected = paperWidth == 80,
                            onClick = { actions.onSetPaperWidth(80) },
                            label = { Text(strings.settingsPrinterWidth80) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = palette.neutralContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                ) {
                    if (isConnected) {
                        OutlinedButton(
                            onClick = actions.onDisconnectPrinter,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.danger),
                            border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                        ) {
                            Text(strings.settingsPrinterDisconnect, maxLines = 1)
                        }
                    } else {
                        OutlinedButton(
                            onClick = actions.onConnectPrinter,
                            enabled = !currentAddress.isNullOrBlank() && !isConnecting,
                            modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                        ) {
                            Text(if (isConnecting) strings.settingsPrinterStatusConnecting else strings.settingsPrinterConnect, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = actions.onOpenReceiptPreview,
                        modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_eye),
                            contentDescription = null,
                            modifier = Modifier.size(PosSizes.iconSmall),
                        )
                        Spacer(modifier = Modifier.width(PosSpacing.xs))
                        Text(strings.settingsPrinterPreviewShort, maxLines = 1)
                    }
                }

                PrimaryActionButton(
                    text = strings.settingsPrinterTestPrint,
                    iconRes = R.drawable.ic_printer,
                    loading = state.isPrintingTest,
                    enabled = !state.isPrintingTest && !currentAddress.isNullOrBlank(),
                    onClick = actions.onTestPrint,
                )

                Text(
                    text = strings.settingsPrinterSimulatorNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (state.isPreviewDialogOpen) {
        ReceiptPreviewDialog(
            bitmap = state.receiptPreviewBitmap,
            paperWidthMm = state.settings?.paperWidthMm ?: 58,
            onDismiss = actions.onDismissReceiptPreview,
            onPrint = actions.onTestPrint,
            isPrinting = state.isPrintingTest,
        )
    }
}
