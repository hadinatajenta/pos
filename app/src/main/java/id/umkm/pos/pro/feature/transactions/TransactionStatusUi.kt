package id.umkm.pos.pro.feature.transactions

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette

internal fun transactionStatusLabel(status: TransactionStatus): String = when (status) {
    TransactionStatus.COMPLETED -> "Lunas"
    TransactionStatus.PARTIAL_DEBT -> "Kasbon"
    TransactionStatus.RETURNED -> "Dikembalikan"
}

@Composable
internal fun transactionStatusContentColor(status: TransactionStatus): Color {
    val palette = LocalPosSemanticPalette.current
    return when (status) {
        TransactionStatus.COMPLETED -> palette.success
        TransactionStatus.PARTIAL_DEBT -> palette.warning
        TransactionStatus.RETURNED -> palette.danger
    }
}

@Composable
internal fun TransactionStatusChip(
    status: TransactionStatus,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPosSemanticPalette.current
    val containerColor: Color
    val contentColor: Color
    @DrawableRes val iconRes: Int
    when (status) {
        TransactionStatus.COMPLETED -> {
            containerColor = palette.successContainer
            contentColor = palette.success
            iconRes = R.drawable.ic_circle_check
        }

        TransactionStatus.PARTIAL_DEBT -> {
            containerColor = palette.warningContainer
            contentColor = palette.warning
            iconRes = R.drawable.ic_circle_alert
        }

        TransactionStatus.RETURNED -> {
            containerColor = palette.dangerContainer
            contentColor = palette.danger
            iconRes = R.drawable.ic_undo
        }
    }
    StatusChip(
        iconRes = iconRes,
        label = transactionStatusLabel(status),
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    )
}
