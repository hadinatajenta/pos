package id.umkm.pos.pro.feature.reports

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun ReportsRoute(
    presenter: ReportsPresenter,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
}

private fun hasDownloadsWritePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
