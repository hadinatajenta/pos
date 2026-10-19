package id.umkm.pos.pro.ui.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import id.umkm.pos.pro.R
import id.umkm.pos.pro.hardware.scanner.BarcodeFrameAnalyzer
import id.umkm.pos.pro.hardware.scanner.ZxingBarcodeFrameReader
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

private enum class ScanSurfaceMode { CAMERA, MANUAL }

@Composable
fun BarcodeScanDialog(
    submitLabel: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val hasCamera = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(if (hasCamera) ScanSurfaceMode.CAMERA else ScanSurfaceMode.MANUAL) }
    var manualCode by rememberSaveable { mutableStateOf("") }
    var isTorchOn by rememberSaveable { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var bindAttempt by remember { mutableIntStateOf(0) }
    val manualFocusRequester = remember { FocusRequester() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasPermission = isGranted
        permissionRequested = true
        if (isGranted) {
            cameraFailed = false
        }
    }

    val isPermissionBlocked = hasCamera &&
        !hasPermission &&
        permissionRequested &&
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false

    val submitManual: () -> Unit = {
        val trimmed = manualCode.trim()
        if (trimmed.isNotEmpty()) onSubmit(trimmed)
    }

    LaunchedEffect(mode) {
        if (mode == ScanSurfaceMode.MANUAL) {
            manualFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(PosSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PosSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                ) {
                    DialogHeader(onDismiss = onDismiss)

                    when {
                        !hasCamera -> NoCameraContent()
                        mode == ScanSurfaceMode.MANUAL -> Unit
                        !hasPermission && !isPermissionBlocked -> PermissionContent(
                            onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        )
                        !hasPermission -> PermissionBlockedContent(
                            onOpenSettings = { context.openAppSettings() },
                        )
                        cameraFailed -> CameraFailedContent(
                            onRetry = {
                                cameraFailed = false
                                bindAttempt += 1
                            },
                        )
                        else -> CameraContent(
                            isTorchOn = isTorchOn,
                            bindAttempt = bindAttempt,
                            onCode = onSubmit,
                            onFailure = { cameraFailed = true },
                        )
                    }

                    if (mode == ScanSurfaceMode.MANUAL || !hasCamera) {
                        ManualEntry(
                            value = manualCode,
                            submitLabel = submitLabel,
                            focusRequester = manualFocusRequester,
                            onValueChange = { manualCode = it },
                            onSubmit = submitManual,
                        )
                    }

                    ScanActions(
                        showTorch = hasCamera && hasPermission && mode == ScanSurfaceMode.CAMERA && !cameraFailed,
                        isTorchOn = isTorchOn,
                        isManualMode = mode == ScanSurfaceMode.MANUAL,
                        canSwitchToCamera = hasCamera,
                        onToggleTorch = { isTorchOn = !isTorchOn },
                        onToggleMode = {
                            mode = if (mode == ScanSurfaceMode.MANUAL) {
                                ScanSurfaceMode.CAMERA
                            } else {
                                ScanSurfaceMode.MANUAL
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(onDismiss: () -> Unit) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_scan_barcode),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = strings.scannerTitle,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = strings.actionClose,
            )
        }
    }
}

@Composable
private fun CameraContent(
    isTorchOn: Boolean,
    bindAttempt: Int,
    onCode: (String) -> Unit,
    onFailure: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large),
    ) {
        key(bindAttempt) {
            CameraPreview(
                isTorchOn = isTorchOn,
                onCode = onCode,
                onFailure = onFailure,
            )
        }
        FinderOverlay()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(PosSpacing.sm),
        ) {
            StatusChip(
                iconRes = R.drawable.ic_scan_barcode,
                label = strings.scannerCameraScanning,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

@Composable
private fun FinderOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scrim = Color.Black.copy(alpha = 0.55f)
        val strokeWidth = 3.dp.toPx()
        val side = minOf(size.width, size.height) * 0.72f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val right = left + side
        val bottom = top + side

        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(scrim, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, side))
        drawRect(scrim, topLeft = Offset(right, top), size = Size(size.width - right, side))
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left - strokeWidth, top - strokeWidth),
            size = Size(side + strokeWidth * 2, side + strokeWidth * 2),
            cornerRadius = CornerRadius(24f),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
private fun CameraPreview(
    isTorchOn: Boolean,
    onCode: (String) -> Unit,
    onFailure: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context.findActivity() as? LifecycleOwner }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val frameReader = remember { ZxingBarcodeFrameReader() }
    val cameraHolder = remember { AtomicReference<Camera?>(null) }
    val providerHolder = remember { AtomicReference<ProcessCameraProvider?>(null) }

    LaunchedEffect(lifecycleOwner) {
        val owner = lifecycleOwner ?: run {
            onFailure()
            return@LaunchedEffect
        }
        try {
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                AndroidSize(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build(),
                )
                .build()
            analysis.setAnalyzer(executor, BarcodeFrameAnalyzer(frameReader, onCode))
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            providerHolder.set(provider)
            cameraHolder.set(camera)
        } catch (error: Exception) {
            onFailure()
        }
    }

    LaunchedEffect(isTorchOn) {
        cameraHolder.get()?.cameraControl?.enableTorch(isTorchOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraHolder.get()?.cameraControl?.enableTorch(false)
            providerHolder.get()?.unbindAll()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun PermissionContent(onGrant: () -> Unit) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    ScanNoticeCard(
        iconRes = R.drawable.ic_lock,
        title = strings.scannerPermissionTitle,
        detail = strings.scannerPermissionDetail,
        containerColor = palette.neutralContainer.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
        Text(strings.scannerPermissionGrant)
    }
}

@Composable
private fun PermissionBlockedContent(onOpenSettings: () -> Unit) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    ScanNoticeCard(
        iconRes = R.drawable.ic_triangle_alert,
        title = strings.scannerPermissionTitle,
        detail = strings.scannerPermissionBlocked,
        containerColor = palette.warningContainer.copy(alpha = 0.6f),
        contentColor = palette.warningText,
    )
    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
        Text(strings.scannerPermissionOpenSettings)
    }
}

@Composable
private fun NoCameraContent() {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    ScanNoticeCard(
        iconRes = R.drawable.ic_scan_barcode,
        title = strings.scannerCameraTitle,
        detail = strings.scannerNoCameraDetail,
        containerColor = palette.neutralContainer.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CameraFailedContent(onRetry: () -> Unit) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    ScanNoticeCard(
        iconRes = R.drawable.ic_triangle_alert,
        title = strings.scannerCameraTitle,
        detail = strings.scannerCameraError,
        containerColor = palette.dangerContainer.copy(alpha = 0.6f),
        contentColor = palette.dangerText,
    )
    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(strings.actionRetry)
    }
}

@Composable
private fun ScanNoticeCard(
    iconRes: Int,
    title: String,
    detail: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PosSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = detail,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ManualEntry(
    value: String,
    submitLabel: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val strings = LocalPosStrings.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(strings.scannerManualLabel) },
        placeholder = { Text(strings.scannerManualPlaceholder) },
        supportingText = { Text(strings.scannerManualHint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
    Button(
        onClick = onSubmit,
        enabled = value.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(submitLabel)
    }
}

@Composable
private fun ScanActions(
    showTorch: Boolean,
    isTorchOn: Boolean,
    isManualMode: Boolean,
    canSwitchToCamera: Boolean,
    onToggleTorch: () -> Unit,
    onToggleMode: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        if (showTorch) {
            OutlinedButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PosSizes.touchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye),
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Text(
                    text = if (isTorchOn) strings.scannerTorchDisable else strings.scannerTorchEnable,
                    maxLines = 1,
                    modifier = Modifier.padding(start = PosSpacing.xs),
                )
            }
        }
        if (canSwitchToCamera) {
            OutlinedButton(
                onClick = onToggleMode,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PosSizes.touchTarget),
            ) {
                Text(
                    text = if (isManualMode) strings.scannerManualModeOff else strings.scannerManualModeOn,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
