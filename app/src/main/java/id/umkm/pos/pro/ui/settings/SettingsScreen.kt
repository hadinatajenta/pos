package id.umkm.pos.pro.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.common.rememberImagePicker
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.components.rememberFileImageBitmap
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

private val QRIS_PREVIEW_HEIGHT = 220.dp

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val pickQrisImage = rememberImagePicker(onImagePicked = actions.onQrisImagePicked)
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Pengaturan Toko & QRIS",
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
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
                QrisSettingsSection(
                    state = state,
                    actions = actions,
                    onPickImage = pickQrisImage,
                )
                state.settings?.let { settings ->
                    StoreProfileSettingsSection(
                        settings = settings,
                        onSave = actions.onSaveStoreSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsFeedbackBanner(state: SettingsUiState) {
    val palette = LocalPosSemanticPalette.current
    state.errorMessage?.let { errorText ->
        Surface(
            color = palette.dangerContainer,
            contentColor = palette.danger,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(PosSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_alert),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = palette.danger,
                )
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.danger,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    state.message?.let { successText ->
        Surface(
            color = palette.successContainer,
            contentColor = palette.success,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(PosSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_check),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = palette.success,
                )
                Text(
                    text = successText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.success,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QrisSettingsSection(
    state: SettingsUiState,
    actions: SettingsActions,
    onPickImage: () -> Unit,
) {
    val palette = LocalPosSemanticPalette.current
    val isConfigured = state.isQrisConfigured

    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            IconBadge(iconRes = R.drawable.ic_scan_barcode)
            Text(
                text = "Pembayaran QRIS",
                style = LocalAppTextStyles.current.sectionHeading,
                modifier = Modifier.weight(1f),
            )
            if (isConfigured) {
                StatusChip(
                    iconRes = R.drawable.ic_circle_check,
                    label = "Sudah Terpasang",
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
        }

        QrisConditionStatusBox(isConfigured = isConfigured)

        if (state.isImportingQris) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Memindai kode QRIS dari gambar...",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (isConfigured) {
            QrisConfiguredContent(
                state = state,
                actions = actions,
                onPickImage = onPickImage,
            )
        } else {
            QrisNotConfiguredUploadZone(
                isImporting = state.isImportingQris,
                onPickImage = onPickImage,
            )
        }
    }
}

@Composable
private fun QrisConditionStatusBox(isConfigured: Boolean) {
    val palette = LocalPosSemanticPalette.current
    val containerColor = if (isConfigured) palette.successContainer.copy(alpha = 0.65f) else palette.warningContainer.copy(alpha = 0.65f)
    val accentColor = if (isConfigured) palette.success else palette.warning
    val iconRes = if (isConfigured) R.drawable.ic_circle_check else R.drawable.ic_triangle_alert
    val statusText = if (isConfigured) {
        "QRIS aktif — nominal otomatis dibuat saat pembayaran di Kasir."
    } else {
        "Belum ada gambar QRIS. Unggah foto QRIS toko Anda di bawah ini."
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
                text = "Unggah Foto / Screenshot QRIS Toko",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Tekan tombol hijau di bawah ini untuk membuka galeri HP/Tablet Anda, lalu pilih gambar QRIS statis toko.",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(PosSpacing.xs))
            PrimaryActionButton(
                text = "Pilih Gambar QRIS dari Galeri",
                iconRes = R.drawable.ic_cloud_upload,
                loading = isImporting,
                enabled = !isImporting,
                onClick = onPickImage,
            )
            Text(
                text = "Tips: Pastikan seluruh kotak kode QR terlihat utuh, terang, dan tidak terpotong.",
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
) {
    val palette = LocalPosSemanticPalette.current
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
                    text = "Pratinjau Gambar QRIS Tersimpan",
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
                            contentDescription = "QRIS statis tersimpan",
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
                                text = "Data Payload QRIS Aktif & Tersimpan",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Siap digunakan untuk menghasilkan kode QR dinamis di kasir.",
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
                    text = "Ganti Foto QRIS",
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
            OutlinedButton(
                onClick = actions.onRemoveQris,
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
                    text = "Hapus QRIS",
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoreProfileSettingsSection(
    settings: AppSettings,
    onSave: (storeName: String, cashierName: String, taxRatePercent: Int, roundingIncrement: Long) -> Unit,
) {
    val initialStoreName = settings.storeName.ifBlank { "Toko UMKM Pro" }
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

    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            IconBadge(iconRes = R.drawable.ic_store)
            Text(
                text = "Profil Toko & Aturan Kasir",
                style = LocalAppTextStyles.current.sectionHeading,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = { Text("Nama Toko / Usaha") },
            placeholder = { Text("Contoh: Toko Berkah Jaya") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = cashierName,
            onValueChange = { cashierName = it },
            label = { Text("Nama Kasir Default (Opsional)") },
            placeholder = { Text("Contoh: Kasir Utama") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
            Text(
                text = "Pajak / PPN Transaksi (%)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
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
                text = "Pembulatan Nominal Struk",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            val roundingOptions = listOf(
                0L to "Tanpa Pembulatan",
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
            text = if (hasChanges) "Simpan Pengaturan Toko" else "Pengaturan Toko Tersimpan",
            iconRes = R.drawable.ic_check,
            enabled = storeName.isNotBlank() && hasChanges,
            onClick = {
                onSave(
                    storeName,
                    cashierName,
                    parsedTax,
                    roundingIncrement,
                )
            },
        )
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
