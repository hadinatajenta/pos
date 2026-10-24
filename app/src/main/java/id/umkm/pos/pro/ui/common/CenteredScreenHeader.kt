package id.umkm.pos.pro.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles

private val DEFAULT_HORIZONTAL_PADDING = 24.dp
private val DEFAULT_TOP_PADDING = 8.dp
private val DEFAULT_BOTTOM_PADDING = 28.dp

@Composable
fun CenteredScreenHeader(
    title: String,
    supportingText: String?,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = DEFAULT_HORIZONTAL_PADDING,
    topPadding: Dp = DEFAULT_TOP_PADDING,
    bottomPadding: Dp = DEFAULT_BOTTOM_PADDING,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topPadding,
                bottom = bottomPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = LocalAppTextStyles.current.screenTitle.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
