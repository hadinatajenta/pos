package id.umkm.pos.pro.feature.reports

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.theme.PosFontFamily

internal object ReportsPalette {
    val ScreenBackground = Color(0xFFF4F7F6)
    val CardSurface = Color(0xFFFFFFFF)
    val Primary = Color(0xFF0A4D36)
    val PrimaryLight = Color(0xFFE6F0EC)
    val TextMain = Color(0xFF111827)
    val TextMuted = Color(0xFF6B7280)
    val GradientEnd = Color(0xFFF0F5F3)
    val ToastSurface = Color(0xFFEBF4F1)
    val ToastBorder = Color(0xFFD1E7DD)
    val Divider = Color(0xFFF1F3F5)
}

internal object ReportsShapes {
    val card = RoundedCornerShape(24.dp)
    val smallCard = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(12.dp)
    val pill = CircleShape
}

@Composable
internal fun ReportsCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = ReportsShapes.card,
    contentPadding: Dp = 24.dp,
    elevation: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = ReportsPalette.CardSurface,
        contentColor = ReportsPalette.TextMain,
        shape = shape,
        modifier = modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.05f),
            spotColor = Color.Black.copy(alpha = 0.05f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
internal fun ReportsCardTitle(
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = text,
            color = ReportsPalette.TextMain,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                color = ReportsPalette.TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun ReportsMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    @DrawableRes iconRes: Int? = null,
    valueColor: Color = ReportsPalette.TextMain,
    onValueClick: (() -> Unit)? = null,
) {
    ReportsCard(
        modifier = modifier,
        shape = ReportsShapes.smallCard,
        contentPadding = 20.dp,
        elevation = 4.dp,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onValueClick == null) Modifier else Modifier.clickable(onClick = onValueClick),
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = label,
                    color = ReportsPalette.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                AutoSizeNumberText(
                    text = value,
                    style = TextStyle(
                        fontFamily = PosFontFamily,
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = valueColor,
                    minFontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        color = ReportsPalette.TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                    )
                }
            }
            if (iconRes != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ReportIconBadge(iconRes = iconRes)
            }
        }
    }
}

@Composable
internal fun ReportIconBadge(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ReportsPalette.PrimaryLight),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ReportsPalette.Primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun ReportsFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIconRes: Int? = null,
) {
    Surface(
        onClick = onClick,
        color = if (selected) ReportsPalette.Primary else ReportsPalette.CardSurface,
        contentColor = if (selected) Color.White else ReportsPalette.TextMuted,
        shape = ReportsShapes.pill,
        shadowElevation = if (selected) 0.dp else 2.dp,
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIconRes != null) {
                Icon(
                    painter = painterResource(leadingIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun ReportsToast(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isError: Boolean = false,
    onOpen: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    Surface(
        color = ReportsPalette.ToastSurface,
        contentColor = ReportsPalette.TextMain,
        shape = ReportsShapes.smallCard,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ReportsPalette.ToastBorder),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = ReportsPalette.Primary,
                )

                isError -> Icon(
                    painter = painterResource(R.drawable.ic_circle_alert),
                    contentDescription = null,
                    tint = Color(0xFFB91C1C),
                    modifier = Modifier.size(18.dp),
                )

                else -> Icon(
                    painter = painterResource(R.drawable.ic_circle_check),
                    contentDescription = null,
                    tint = ReportsPalette.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ReportsPalette.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onOpen != null) {
                ToastAction(label = "Open", onClick = onOpen)
            }
            if (onShare != null) {
                ToastAction(label = "Share", onClick = onShare)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    tint = ReportsPalette.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ToastAction(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        contentColor = ReportsPalette.Primary,
        shape = ReportsShapes.button,
        modifier = Modifier.heightIn(min = 36.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
