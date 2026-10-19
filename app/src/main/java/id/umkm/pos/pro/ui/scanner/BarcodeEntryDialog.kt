package id.umkm.pos.pro.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
fun BarcodeEntryDialog(
    submitLabel: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalPosStrings.current
    var code by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val submit: () -> Unit = {
        val trimmed = code.trim()
        if (trimmed.isNotEmpty()) onSubmit(trimmed)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_scan_barcode),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(strings.scannerTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.md)) {
                CameraStatusCard()
                OutlinedTextField(
                    value = code,
                    onValueChange = { value -> code = value },
                    label = { Text(strings.scannerManualLabel) },
                    placeholder = { Text(strings.scannerManualPlaceholder) },
                    supportingText = { Text(strings.scannerManualHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            Button(onClick = submit, enabled = code.isNotBlank()) {
                Text(submitLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun CameraStatusCard() {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    Surface(
        color = palette.neutralContainer.copy(alpha = 0.6f),
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
                    painter = painterResource(R.drawable.ic_scan_barcode),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = strings.scannerCameraTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
            }
            StatusChip(
                iconRes = R.drawable.ic_clock,
                label = strings.scannerCameraUnavailable,
                containerColor = palette.neutralContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = strings.scannerCameraUnavailableDetail,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
