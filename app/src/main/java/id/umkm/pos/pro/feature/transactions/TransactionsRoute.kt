package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.umkm.pos.pro.feature.reports.ReportsPresenter
import id.umkm.pos.pro.ui.common.rememberReceiptPrintLauncher
import id.umkm.pos.pro.ui.returns.ReturnRoute
import java.util.TimeZone

@Composable
fun TransactionsRoute(
    presenter: ReportsPresenter,
    onStartSale: () -> Unit,
    onShareReceiptText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()
    val timeZone = remember { TimeZone.getDefault() }
    val printReceipt = rememberReceiptPrintLauncher()
    val actions = remember(presenter) {
        TransactionActions(
            onOpenDetail = presenter::openDetail,
            onReprintReceipt = presenter::printReceipt,
            onShareReceipt = presenter::shareReceipt,
            onRefund = presenter::requestReturn,
        )
    }

    LaunchedEffect(presenter) {
        presenter.start()
    }

    TransactionsScreen(
        uiState = uiState,
        actions = actions,
        onPresetSelected = presenter::selectPreset,
        onCustomRangeSelected = presenter::selectCustomDays,
        onToggleStatusFilter = presenter::toggleStatusFilter,
        onAddTransaction = onStartSale,
        onRefresh = presenter::refresh,
        modifier = modifier,
    )

    val pendingReceiptShare = uiState.pendingReceiptShare
    LaunchedEffect(pendingReceiptShare) {
        if (pendingReceiptShare != null) {
            onShareReceiptText(pendingReceiptShare)
            presenter.onReceiptShared()
        }
    }

    val pendingReceiptPrint = uiState.pendingReceiptPrint
    LaunchedEffect(pendingReceiptPrint) {
        if (pendingReceiptPrint != null) {
            printReceipt(pendingReceiptPrint)
            presenter.onReceiptPrinted()
        }
    }

    if (uiState.detail != null || uiState.isDetailLoading) {
        TransactionDetailDialog(
            detail = uiState.detail,
            isDetailLoading = uiState.isDetailLoading,
            timeZone = timeZone,
            onDismiss = presenter::closeDetail,
        )
    }

    val message = uiState.message
    if (message != null) {
        AlertDialog(
            onDismissRequest = presenter::onMessageConsumed,
            title = { Text(text = "Transaksi") },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = presenter::onMessageConsumed) {
                    Text(text = "Tutup")
                }
            },
        )
    }

    val pendingReturnTransactionId = uiState.pendingReturnTransactionId
    if (pendingReturnTransactionId != null) {
        Dialog(
            onDismissRequest = presenter::dismissReturn,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ReturnRoute(
                    transactionId = pendingReturnTransactionId,
                    onReturnProcessed = presenter::onReturnProcessed,
                )
            }
        }
    }
}
