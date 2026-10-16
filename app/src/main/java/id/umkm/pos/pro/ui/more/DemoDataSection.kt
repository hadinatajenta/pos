package id.umkm.pos.pro.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.BuildConfig
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.data.seed.DemoClearSummary
import id.umkm.pos.pro.data.seed.DemoDataSeeder
import id.umkm.pos.pro.data.seed.DemoSeedSummary
import id.umkm.pos.pro.ui.common.SectionHeading
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import kotlinx.coroutines.launch
import id.umkm.pos.pro.R
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

val LocalDemoDataSeeder = staticCompositionLocalOf<DemoDataSeeder?> { null }

@Composable
internal fun DemoDataSection(modifier: Modifier = Modifier) {
    if (BuildConfig.DEBUG) {
        DemoDataSectionContent(modifier = modifier)
    }
}

@Composable
private fun DemoDataSectionContent(modifier: Modifier = Modifier) {
    val seeder = LocalDemoDataSeeder.current ?: return
    val scope = rememberCoroutineScope()
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        SectionHeading(text = "Data Demo")
        DemoActionRow(
            label = "Muat data demo",
            supportingText = "Isi katalog, pelanggan, dan transaksi contoh",
            iconRes = R.drawable.ic_flask,
            isBusy = isBusy,
            onClick = {
                if (!isBusy) {
                    scope.launch {
                        isBusy = true
                        statusMessage = null
                        statusMessage = seeder.load().toStatusMessage(::seedLoadedMessage)
                        isBusy = false
                    }
                }
            },
        )
        DemoActionRow(
            label = "Hapus data demo",
            supportingText = "Hapus hanya data bertanda DEMO",
            iconRes = R.drawable.ic_trash,
            isBusy = isBusy,
            onClick = { if (!isBusy) showClearConfirm = true },
        )
        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = "Hapus data demo?") },
            text = {
                Text(text = "Transaksi, pelanggan, dan produk bertanda DEMO akan dihapus. Data asli tidak diubah.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        scope.launch {
                            isBusy = true
                            statusMessage = null
                            statusMessage = seeder.clear().toClearStatusMessage(::clearSummaryMessage)
                            isBusy = false
                        }
                    },
                ) {
                    Text(text = "Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = "Batal")
                }
            },
        )
    }
}

@Composable
private fun DemoActionRow(
    label: String,
    supportingText: String,
    @DrawableRes iconRes: Int,
    isBusy: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isBusy) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        Row(
            modifier = Modifier.padding(PosSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PosSizes.iconMedium),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconMedium),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun AppResult<DemoSeedSummary>.toStatusMessage(message: (DemoSeedSummary) -> String): String =
    when (this) {
        is AppResult.Success -> message(value)
        is AppResult.Failure -> failureMessage(error.message)
    }

private fun AppResult<DemoClearSummary>.toClearStatusMessage(message: (DemoClearSummary) -> String): String =
    when (this) {
        is AppResult.Success -> message(value)
        is AppResult.Failure -> failureMessage(error.message)
    }

private fun seedLoadedMessage(summary: DemoSeedSummary): String =
    "Dimuat: ${summary.products} produk, ${summary.customers} pelanggan, " +
        "${summary.transactions} transaksi, ${summary.returns} retur"

private fun clearSummaryMessage(summary: DemoClearSummary): String =
    "Dihapus: ${summary.products} produk, ${summary.customers} pelanggan, ${summary.transactions} transaksi"

private fun failureMessage(detail: String): String = "Gagal: $detail"
