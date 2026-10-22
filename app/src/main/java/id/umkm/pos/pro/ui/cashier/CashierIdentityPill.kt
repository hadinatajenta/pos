package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.PersonAvatar
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun CashierIdentityPill(
    cashierName: String?,
    isShiftOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val label = if (isShiftOpen) {
        cashierName?.takeIf { it.isNotBlank() } ?: strings.moreDefaultCashierName
    } else {
        strings.shiftOpenTitle
    }
    val actionLabel = if (isShiftOpen) {
        strings.cashierShiftSwitchAction
    } else {
        strings.cashierShiftOpenAction
    }
    val shape = PosShapes.medium
    Box(
        modifier = modifier.heightIn(min = PosSizes.touchTarget),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = shape,
            color = if (isShiftOpen) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (isShiftOpen) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier
                .clip(shape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = onClick,
                ),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = if (isShiftOpen) 5.dp else 10.dp,
                    end = 9.dp,
                    top = 4.dp,
                    bottom = 4.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                if (isShiftOpen) {
                    PersonAvatar(name = label, size = 28.dp)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(PosSizes.iconSmall),
                    )
                }
                Text(
                    text = label,
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 132.dp),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
