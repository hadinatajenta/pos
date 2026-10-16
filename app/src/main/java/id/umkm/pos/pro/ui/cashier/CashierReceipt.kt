package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R

private val TRAILING_VALUE_MAX_WIDTH = 176.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierReceiptSheet(
    receipt: CashierReceipt,
    isPrintingReceipt: Boolean,
    receiptMessage: String?,
    actions: CashierActions,
    onShareReceipt: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onNewSale,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PosSpacing.md)
                .padding(bottom = PosSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_circle_check),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = LocalPosSemanticPalette.current.success,
            )
            Text(
                text = "Transaksi selesai",
                style = LocalAppTextStyles.current.sectionHeading,
                textAlign = TextAlign.Center,
            )
            Text(
                text = receipt.invoiceNo,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PosCard(modifier = Modifier.fillMaxWidth()) {
                ReceiptSummaryLine(label = "Total", value = Rupiah.format(receipt.grandTotal), emphasized = true)
                ReceiptSummaryLine(label = "Dibayar", value = Rupiah.format(receipt.paidAmount))
                if (receipt.changeAmount > 0L) {
                    ReceiptSummaryLine(label = "Kembalian", value = Rupiah.format(receipt.changeAmount))
                }
                if (receipt.remainingDebt > 0L) {
                    ReceiptSummaryLine(label = "Sisa tagihan", value = Rupiah.format(receipt.remainingDebt))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                OutlinedButton(
                    onClick = actions.onPrintReceipt,
                    enabled = !isPrintingReceipt,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    if (isPrintingReceipt) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(PosSizes.iconSmall),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_printer),
                            contentDescription = null,
                            modifier = Modifier.size(PosSizes.iconSmall),
                        )
                        Spacer(modifier = Modifier.width(PosSpacing.xs))
                        Text(text = "Cetak struk")
                    }
                }
                OutlinedButton(
                    onClick = { onShareReceipt(receipt.shareText) },
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = null,
                        modifier = Modifier.size(PosSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(PosSpacing.xs))
                    Text(text = "Bagikan")
                }
            }
            PrimaryActionButton(
                text = "Transaksi baru",
                onClick = actions.onNewSale,
            )
            if (receiptMessage != null) {
                Text(
                    text = receiptMessage,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ReceiptSummaryLine(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AutoSizeNumberText(
            text = value,
            style = if (emphasized) {
                LocalAppTextStyles.current.financialValue
            } else {
                LocalAppTextStyles.current.body
            },
            modifier = Modifier.widthIn(max = TRAILING_VALUE_MAX_WIDTH),
        )
    }
}
