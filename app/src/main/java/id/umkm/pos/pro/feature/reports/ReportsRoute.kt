package id.umkm.pos.pro.feature.reports

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.returns.ReturnRoute

@Composable
fun ReportsRoute(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportDirectory = remember(context) {
        context.getExternalFilesDir(null) ?: context.filesDir
    }
    val presenter = remember(container, exportDirectory) {
        ReportsPresenter(
            reportRepository = container.reportRepository,
            transactionRepository = container.transactionRepository,
            productRepository = container.productRepository,
            csvExporter = SalesCsvExporter(container.reportRepository, FileCsvSink()),
            xlsxExporter = XlsxReportExporter(FileBinarySink()),
            adController = container.adController,
            exportDirectory = exportDirectory,
            coroutineScope = coroutineScope,
            clock = container.clock,
        )
    }

    val uiState by presenter.uiState.collectAsState()

    LaunchedEffect(presenter) {
        presenter.start()
    }

    ReportsScreen(
        uiState = uiState,
        onPresetSelected = presenter::selectPreset,
        onCustomRangeSelected = presenter::selectCustomDays,
        onRefresh = presenter::refresh,
        onSummaryDrillDown = presenter::openSummaryDrillDown,
        onDayDrillDown = presenter::openDayDrillDown,
        onCloseDrillDown = presenter::closeDrillDown,
        onTransactionReturn = presenter::requestReturn,
        onExport = presenter::exportReport,
        onDismissExport = presenter::dismissExportState,
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
