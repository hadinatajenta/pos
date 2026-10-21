package id.umkm.pos.pro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsStoreScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings ?: return
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val initialStoreName = settings.storeName.ifBlank { strings.moreDefaultStoreName }
    var storeName by rememberSaveable(settings.storeName) { mutableStateOf(initialStoreName) }
    var cashierName by rememberSaveable(settings.cashierName) { mutableStateOf(settings.cashierName.orEmpty()) }
    var taxRateInput by rememberSaveable(settings.taxRatePercent) {
        mutableStateOf(settings.taxRatePercent.toString())
    }
    var roundingIncrement by rememberSaveable(settings.roundingIncrement) {
        mutableLongStateOf(settings.roundingIncrement)
    }

    val parsedTax = taxRateInput.toIntOrNull()?.coerceIn(0, 100) ?: 0
    val hasChanges = remember(storeName, cashierName, parsedTax, roundingIncrement, settings) {
        storeName.trim() != settings.storeName ||
            cashierName.trim() != settings.cashierName.orEmpty() ||
            parsedTax != settings.taxRatePercent ||
            roundingIncrement != settings.roundingIncrement
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.settingsStoreProfileTitle,
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
            SettingsFeedbackBanner(state = state)

            PosCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text(strings.settingsStoreNameLabel) },
                    placeholder = { Text(strings.settingsStoreNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = { Text(strings.settingsCashierNameLabel) },
                    placeholder = { Text(strings.settingsCashierNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
                    Text(
                        text = strings.settingsTaxLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                    ) {
                        OutlinedTextField(
                            value = taxRateInput,
                            onValueChange = { value ->
                                val digits = value.filter { it.isDigit() }.take(3)
                                taxRateInput = digits
                            },
                            placeholder = { Text("0") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(104.dp),
                        )
                        listOf(0 to "0%", 10 to "10%", 11 to "11%").forEach { (preset, label) ->
                            val selected = parsedTax == preset && taxRateInput.isNotEmpty()
                            FilterChip(
                                selected = selected,
                                onClick = { taxRateInput = preset.toString() },
                                label = { Text(label, maxLines = 1) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = LocalPosSemanticPalette.current.neutralContainer,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.settingsRoundingLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val roundingOptions = listOf(
                        0L to strings.settingsNoRounding,
                        100L to "Rp 100",
                        500L to "Rp 500",
                        1000L to "Rp 1.000",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                    ) {
                        roundingOptions.forEach { (value, label) ->
                            val selected = roundingIncrement == value
                            FilterChip(
                                selected = selected,
                                onClick = { roundingIncrement = value },
                                label = { Text(label) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = LocalPosSemanticPalette.current.neutralContainer,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }

                PrimaryActionButton(
                    text = if (hasChanges) strings.settingsSaveStore else strings.settingsStoreSaved,
                    iconRes = R.drawable.ic_check,
                    enabled = storeName.isNotBlank() && hasChanges,
                    onClick = {
                        val saveStoreSettings = {
                            actions.onSaveStoreSettings(
                                storeName,
                                cashierName,
                                parsedTax,
                                roundingIncrement,
                            )
                        }
                        val changesTaxOrRounding = requiresTaxOrRoundingGate(
                            storedTaxRatePercent = settings.taxRatePercent,
                            storedRoundingIncrement = settings.roundingIncrement,
                            taxRatePercent = parsedTax,
                            roundingIncrement = roundingIncrement,
                        )
                        if (changesTaxOrRounding) {
                            gate.request(
                                CriticalAction.CHANGE_TAX_AND_ROUNDING,
                                strings.settingsSaveStore,
                                saveStoreSettings,
                            )
                        } else {
                            saveStoreSettings()
                        }
                    },
                )
            }
        }
    }
}
