package id.umkm.pos.pro.ui.components.hub

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.components.IconBadge
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSpacing

private val HUB_ROW_MIN_HEIGHT = 56.dp
private val HUB_ROW_CHEVRON_SIZE = 20.dp
private val HUB_ROW_COMPACT_THRESHOLD = 380.dp
private val HUB_ROW_SELECTED_ALPHA = 0.35f

@Composable
fun HubRow(
    title: String,
    description: String?,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    badge: @Composable () -> Unit = {},
) {
    BoxWithConstraints {
        val isCompactRow = maxWidth < HUB_ROW_COMPACT_THRESHOLD
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = HUB_ROW_SELECTED_ALPHA)
                    } else {
                        Color.Transparent
                    },
                )
                .heightIn(min = HUB_ROW_MIN_HEIGHT)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { selected = isSelected }
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            IconBadge(iconRes = iconRes)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isCompactRow) {
                    Spacer(modifier = Modifier.height(PosSpacing.xs))
                    badge()
                }
            }
            if (!isCompactRow) {
                badge()
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(HUB_ROW_CHEVRON_SIZE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
