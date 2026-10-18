package id.umkm.pos.pro.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun ReceiptPreviewDialog(
    bitmap: Bitmap?,
    paperWidthMm: Int,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    isPrinting: Boolean,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PosSpacing.md),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PosSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsPrinterPreviewTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "${strings.settingsPrinterPaperWidth}: ${paperWidthMm}mm (${if (paperWidthMm >= 80) strings.settingsPrinterWidth80Detail else strings.settingsPrinterWidth58Detail})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = strings.actionClose,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                HorizontalDivider()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(palette.neutralContainer.copy(alpha = 0.5f))
                        .padding(PosSpacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth(if (paperWidthMm >= 80) 0.95f else 0.85f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = strings.settingsPrinterPreviewDescription,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            )
                        }
                    } else {
                        Text(
                            text = strings.settingsPrinterPreviewLoading,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(strings.actionClose)
                    }
                    PrimaryActionButton(
                        text = strings.cashierPrintReceipt,
                        iconRes = R.drawable.ic_printer,
                        loading = isPrinting,
                        enabled = !isPrinting,
                        onClick = onPrint,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
