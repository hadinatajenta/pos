package id.umkm.pos.pro.feature.reports

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.returns.ReturnRoute

@Composable
fun ReportsRoute(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadsSink = remember(context) { DownloadsReportSink(context.applicationContext) }
    val presenter = remember(container, downloadsSink) {
        ReportsPresenter(
            reportRepository = container.reportRepository,
            transactionRepository = container.transactionRepository,
            productRepository = container.productRepository,
            csvExporter = SalesCsvExporter(container.reportRepository, downloadsSink),
            xlsxExporter = XlsxReportExporter(downloadsSink),
            adController = container.adController,
            coroutineScope = coroutineScope,
            clock = container.clock,
        )
    }

    val uiState by presenter.uiState.collectAsState()
    var pendingExportFormat by remember { mutableStateOf<ReportExportFormat?>(null) }
    val exportPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val format = pendingExportFormat
        pendingExportFormat = null
        if (format != null) {
            if (isGranted) {
                presenter.exportReport(format)
            } else {
                presenter.onExportPermissionDenied()
            }
        }
    }

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
        onExport = { format ->
            if (hasDownloadsWritePermission(context)) {
                presenter.exportReport(format)
            } else {
                pendingExportFormat = format
                exportPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        },
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

private fun hasDownloadsWritePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
