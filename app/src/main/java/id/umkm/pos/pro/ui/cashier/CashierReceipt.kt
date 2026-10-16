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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

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
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Sale complete",
                style = LocalAppTextStyles.current.sectionHeading,
                textAlign = TextAlign.Center,
            )
            Text(
                text = receipt.invoiceNo,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(PosSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                ) {
                    ReceiptSummaryLine(label = "Total", value = Rupiah.format(receipt.grandTotal), emphasized = true)
                    ReceiptSummaryLine(label = "Paid", value = Rupiah.format(receipt.paidAmount))
                    if (receipt.changeAmount > 0L) {
                        ReceiptSummaryLine(label = "Change", value = Rupiah.format(receipt.changeAmount))
                    }
                    if (receipt.remainingDebt > 0L) {
                        ReceiptSummaryLine(label = "Remaining balance", value = Rupiah.format(receipt.remainingDebt))
                    }
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
                            imageVector = Icons.Filled.Print,
                            contentDescription = null,
                            modifier = Modifier.size(PosSizes.iconSmall),
                        )
                        Spacer(modifier = Modifier.width(PosSpacing.xs))
                        Text(text = "Print")
                    }
                }
                OutlinedButton(
                    onClick = { onShareReceipt(receipt.shareText) },
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(PosSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(PosSpacing.xs))
                    Text(text = "Share")
                }
            }
            Button(
                onClick = actions.onNewSale,
                modifier = Modifier.fillMaxWidth().heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = "New sale", style = LocalAppTextStyles.current.buttonLabel)
            }
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
        Text(
            text = value,
            style = if (emphasized) {
                LocalAppTextStyles.current.financialValue
            } else {
                LocalAppTextStyles.current.body
            },
        )
    }
}
