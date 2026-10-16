package id.umkm.pos.pro.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.theme.PosSpacing

private data class MoreEntry(
    val label: String,
    val supportingText: String,
    val icon: ImageVector,
    val isAvailable: Boolean,
)

private val moreEntries = listOf(
    MoreEntry("Pelanggan & Kasbon", "Catat utang dan pembayaran pelanggan", Icons.Filled.Groups, isAvailable = true),
    MoreEntry("Printer Struk", "Sambungkan printer termal", Icons.Filled.Print, isAvailable = false),
    MoreEntry("Scanner Barcode", "Kamera atau pemindai USB/Bluetooth", Icons.Filled.QrCodeScanner, isAvailable = false),
    MoreEntry("Cadangkan & Pulihkan", "Salinan lokal dan Google Drive", Icons.Filled.CloudUpload, isAvailable = false),
    MoreEntry("Keamanan & PIN", "Lindungi tindakan sensitif", Icons.Filled.Lock, isAvailable = false),
    MoreEntry("Pengaturan", "Identitas toko dan preferensi", Icons.Filled.Settings, isAvailable = false),
)

@Composable
fun MoreRoute(modifier: Modifier = Modifier) {
    var showCustomers by rememberSaveable { mutableStateOf(false) }
    if (showCustomers) {
        BackHandler { showCustomers = false }
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PosSpacing.sm, vertical = PosSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showCustomers = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Text(text = "Pelanggan & Kasbon", style = MaterialTheme.typography.titleLarge)
            }
            id.umkm.pos.pro.ui.customers.CustomersRoute(modifier = Modifier.fillMaxSize())
        }
    } else {
        MoreMenu(modifier = modifier, onOpenCustomers = { showCustomers = true })
    }
}

@Composable
private fun MoreMenu(
    onOpenCustomers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Lainnya",
            supportingText = "Perangkat, data, dan pengaturan",
        )
        Column(
            modifier = Modifier.padding(horizontal = PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            moreEntries.forEach { entry ->
                MoreEntryRow(
                    entry = entry,
                    onClick = { if (entry.label == "Pelanggan & Kasbon") onOpenCustomers() },
                )
            }
            DemoDataSection()
        }
    }
}

@Composable
private fun MoreEntryRow(
    entry: MoreEntry,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (entry.isAvailable) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(PosSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (entry.isAvailable) entry.supportingText else "Segera hadir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.isAvailable) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
