package id.umkm.pos.pro.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.BuildConfig
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.data.seed.DemoClearSummary
import id.umkm.pos.pro.data.seed.DemoDataSeeder
import id.umkm.pos.pro.data.seed.DemoSeedSummary
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import kotlinx.coroutines.launch

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
    val palette = LocalPosSemanticPalette.current
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    PosCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            IconBadge(iconRes = R.drawable.ic_flask)
            Text(
                text = "Data Demo",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            OutlinedButton(
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
                enabled = !isBusy,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = PosSpacing.sm,
                    vertical = PosSpacing.xs,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PosSizes.touchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flask),
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(
                    text = "Muat Demo",
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
            OutlinedButton(
                onClick = { if (!isBusy) showClearConfirm = true },
                enabled = !isBusy,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = palette.danger,
                ),
                border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.35f)),
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
                    text = "Hapus Demo",
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
        }

        statusMessage?.let { message ->
            Surface(
                color = palette.neutralContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = "Hapus data demo?") },
            text = {
                Text(text = "Transaksi, pelanggan, dan produk bertanda DEMO akan dihapus. Data asli toko tidak diubah.")
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
