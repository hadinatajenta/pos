package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
fun ExportRunningRow(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(PosSpacing.sm))
        Text(text = message, style = LocalAppTextStyles.current.supportingLabel)
    }
}

@Composable
fun ExportFailedRow(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_circle_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(PosSpacing.sm))
        Text(
            text = message,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        SecondaryTextAction(text = strings.actionClose, onClick = onDismiss)
    }
}

@Composable
fun ExportDoneRow(
    title: String,
    detail: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    Surface(
        color = palette.successContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, palette.success.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_check),
                    contentDescription = null,
                    tint = palette.success,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = strings.actionClose,
                        modifier = Modifier.size(18.dp),
                    )
                }
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
fun ExportedFileCard(
    fileName: String,
    destinationLabel: String,
    onOpen: () -> Boolean,
    onShare: () -> Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    var failedAction by remember(fileName) { mutableStateOf<ExportedFileAction?>(null) }

    Surface(
        color = palette.successContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, palette.success.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_check),
                    contentDescription = null,
                    tint = palette.success,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = strings.exportDone,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = strings.actionClose,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = destinationLabel,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            failedAction?.let { action ->
                Text(
                    text = when (action) {
                        ExportedFileAction.OPEN -> strings.exportOpenFailed
                        ExportedFileAction.SHARE -> strings.exportShareFailed
                    },
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                OutlinedButton(
                    onClick = {
                        failedAction = if (onOpen()) null else ExportedFileAction.OPEN
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = PosSizes.touchTarget),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eye),
                        contentDescription = null,
                        modifier = Modifier.size(PosSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(PosSpacing.xs))
                    Text(text = strings.actionOpen, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        failedAction = if (onShare()) null else ExportedFileAction.SHARE
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = PosSizes.touchTarget),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = null,
                        modifier = Modifier.size(PosSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(PosSpacing.xs))
                    Text(text = strings.actionShare, maxLines = 1)
                }
            }
        }
    }
}

private enum class ExportedFileAction {
    OPEN,
    SHARE,
}
