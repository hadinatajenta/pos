package id.umkm.pos.pro.feature.transactions

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.security.SecurityGate
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

internal data class TransactionActions(
    val onOpenDetail: (Long) -> Unit,
    val onReprintReceipt: (Long) -> Unit,
    val onShareReceipt: (Long) -> Unit,
    val onRefund: (Long) -> Unit,
)

private val MENU_MIN_WIDTH = 232.dp

@Composable
internal fun TransactionActionsButton(
    transaction: Transaction,
    actions: TransactionActions,
    modifier: Modifier = Modifier,
    isSheetStyle: Boolean = false,
    onOpenSheet: () -> Unit = {},
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val strings = LocalPosStrings.current
    Box(modifier = modifier) {
        IconButton(onClick = { if (isSheetStyle) onOpenSheet() else isMenuExpanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_ellipsis_vertical),
                contentDescription = strings.transactionsActionMenu(transaction.invoiceNo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!isSheetStyle) {
            TransactionActionsDropdown(
                expanded = isMenuExpanded,
                transaction = transaction,
                onDismiss = { isMenuExpanded = false },
                actions = actions,
            )
        }
    }
}

private data class TransactionMenuItem(
    val label: String,
    @DrawableRes val iconRes: Int,
    val isDestructive: Boolean,
    val onClick: () -> Unit,
)

private fun transactionMenuItems(
    transaction: Transaction,
    onDismiss: () -> Unit,
    actions: TransactionActions,
    strings: PosStrings,
    gate: SecurityGate,
): List<TransactionMenuItem> = listOfNotNull(
    TransactionMenuItem(strings.transactionsActionViewDetail, R.drawable.ic_eye, false) {
        onDismiss()
        actions.onOpenDetail(transaction.id)
    },
    TransactionMenuItem(strings.transactionsActionReprint, R.drawable.ic_printer, false) {
        onDismiss()
        actions.onReprintReceipt(transaction.id)
    },
    TransactionMenuItem(strings.transactionsActionShare, R.drawable.ic_share, false) {
        onDismiss()
        actions.onShareReceipt(transaction.id)
    },
    if (transaction.status == TransactionStatus.RETURNED) {
        null
    } else {
        TransactionMenuItem(strings.transactionsActionRefund, R.drawable.ic_undo, true) {
            onDismiss()
            gate.request(CriticalAction.PROCESS_RETURN, strings.transactionsActionRefund) {
                actions.onRefund(transaction.id)
            }
        }
    },
)

@Composable
private fun TransactionActionsDropdown(
    expanded: Boolean,
    transaction: Transaction,
    onDismiss: () -> Unit,
    actions: TransactionActions,
) {
    val palette = LocalPosSemanticPalette.current
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val menuItems = transactionMenuItems(transaction, onDismiss, actions, strings, gate)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = MENU_MIN_WIDTH),
    ) {
        menuItems.forEachIndexed { index, menuItem ->
            if (menuItem.isDestructive && index > 0) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = menuItem.label,
                        color = if (menuItem.isDestructive) palette.danger else Color.Unspecified,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(menuItem.iconRes),
                        contentDescription = null,
                        tint = if (menuItem.isDestructive) palette.danger else LocalContentColor.current,
                    )
                },
                onClick = menuItem.onClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionActionsSheet(
    transaction: Transaction,
    onDismiss: () -> Unit,
    actions: TransactionActions,
) {
    val palette = LocalPosSemanticPalette.current
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val menuItems = transactionMenuItems(transaction, onDismiss, actions, strings, gate)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PosSpacing.md),
        ) {
            Text(
                text = strings.transactionsMenuTitle(transaction.invoiceNo),
                style = LocalAppTextStyles.current.sectionHeading,
                modifier = Modifier.padding(horizontal = PosSpacing.lg, vertical = PosSpacing.sm),
            )
            menuItems.forEachIndexed { index, menuItem ->
                if (menuItem.isDestructive && index > 0) {
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PosSizes.touchTarget)
                        .clickable(onClick = menuItem.onClick)
                        .padding(horizontal = PosSpacing.lg, vertical = PosSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
                ) {
                    Icon(
                        painter = painterResource(menuItem.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(PosSizes.iconMedium),
                        tint = if (menuItem.isDestructive) {
                            palette.danger
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = menuItem.label,
                        style = LocalAppTextStyles.current.body,
                        color = if (menuItem.isDestructive) {
                            palette.danger
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
