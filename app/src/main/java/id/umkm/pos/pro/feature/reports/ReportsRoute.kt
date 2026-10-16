package id.umkm.pos.pro.feature.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import id.umkm.pos.pro.ui.LocalAppContainer

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
            csvExporter = SalesCsvExporter(container.reportRepository, FileCsvSink()),
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
        onExportCsv = presenter::exportCsv,
        onDismissExport = presenter::dismissExportState,
        modifier = modifier,
    )
}
