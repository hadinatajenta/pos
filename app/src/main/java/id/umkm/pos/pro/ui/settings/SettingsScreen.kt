package id.umkm.pos.pro.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.AppSettings
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.common.rememberImagePicker
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.components.rememberFileImageBitmap
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

private val QRIS_PREVIEW_HEIGHT = 240.dp

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val pickQrisImage = rememberImagePicker(onImagePicked = actions.onQrisImagePicked)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = PosSpacing.sm, bottom = PosSpacing.lg),
        ) {
            QrisSettingsCard(state = state, actions = actions, onPickImage = pickQrisImage)
        }
    }
}

@Composable
private fun QrisSettingsCard(
    state: SettingsUiState,
    actions: SettingsActions,
    onPickImage: () -> Unit,
) {
    val palette = LocalPosSemanticPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Text(
                text = "QRIS",
                style = LocalAppTextStyles.current.sectionHeading,
            )
            StatusChip(
                iconRes = R.drawable.ic_triangle_alert,
                label = "Wajib",
                containerColor = palette.warningContainer,
                contentColor = palette.warning,
            )
        }
        Text(
            text = "Unggah foto QRIS statis milik Anda. Saat pembayaran QRIS, aplikasi membuat QR dengan nominal otomatis sesuai total belanja.",
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.isQrisConfigured) {
            QrisConfiguredCard(state = state, actions = actions, onPickImage = onPickImage)
        } else {
            QrisUploadCard(isImporting = state.isImportingQris, onPickImage = onPickImage)
        }
        if (state.isImportingQris) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Membaca QRIS dari gambar...",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = LocalAppTextStyles.current.supportingLabel,
                color = palette.danger,
            )
        }
        state.message?.let { message ->
            Text(
                text = message,
                style = LocalAppTextStyles.current.supportingLabel,
                color = palette.success,
            )
        }
        if (!state.isQrisConfigured) {
            Text(
                text = "Pembayaran QRIS tidak bisa dipakai sampai foto QRIS statis diunggah.",
                style = LocalAppTextStyles.current.supportingLabel,
                color = palette.warning,
            )
        }
    }
}

@Composable
private fun QrisUploadCard(
    isImporting: Boolean,
    onPickImage: () -> Unit,
) {
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isImporting, onClick = onPickImage),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_scan_barcode),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Unggah QRIS statis",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Pilih foto QRIS dari galeri. Pastikan seluruh QR terlihat jelas.",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QrisConfiguredCard(
    state: SettingsUiState,
    actions: SettingsActions,
    onPickImage: () -> Unit,
) {
    val palette = LocalPosSemanticPalette.current
    val preview = rememberFileImageBitmap(state.settings?.qrisImagePath)
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(QRIS_PREVIEW_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = "QRIS statis",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "Pratinjau QRIS tidak tersedia",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            StatusChip(
                iconRes = R.drawable.ic_circle_check,
                label = "QRIS aktif",
                containerColor = palette.successContainer,
                contentColor = palette.success,
            )
            Spacer(modifier = Modifier.weight(1f))
            SecondaryTextAction(
                text = "Ganti foto",
                onClick = onPickImage,
                enabled = !state.isImportingQris,
            )
        }
        SecondaryTextAction(
            text = "Hapus QRIS",
            onClick = actions.onRemoveQris,
            enabled = !state.isImportingQris,
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    UmkmPosTheme {
        SettingsScreen(
            state = SettingsUiState(
                settings = AppSettings(
                    storeName = "Toko Uji",
                    cashierName = null,
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
