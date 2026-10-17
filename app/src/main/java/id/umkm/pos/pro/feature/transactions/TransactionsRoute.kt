package id.umkm.pos.pro.feature.transactions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.umkm.pos.pro.feature.reports.ReportsPresenter
import id.umkm.pos.pro.ui.returns.ReturnRoute

@Composable
fun TransactionsRoute(
    presenter: ReportsPresenter,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()

    LaunchedEffect(presenter) {
        presenter.start()
    }

    TransactionsScreen(
        uiState = uiState,
        onPresetSelected = presenter::selectPreset,
        onCustomRangeSelected = presenter::selectCustomDays,
        onRefresh = presenter::refresh,
        onSummaryDrillDown = presenter::openSummaryDrillDown,
        onDayDrillDown = presenter::openDayDrillDown,
        onCloseDrillDown = presenter::closeDrillDown,
        onTransactionReturn = presenter::requestReturn,
        modifier = modifier,
    )

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
