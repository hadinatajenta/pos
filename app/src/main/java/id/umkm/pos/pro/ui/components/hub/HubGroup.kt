package id.umkm.pos.pro.ui.components.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSpacing

private val HUB_GROUP_BORDER_WIDTH = 0.75.dp

@Composable
fun HubSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = LocalAppTextStyles.current.sectionHeading,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(start = PosSpacing.xs),
    )
}

@Composable
fun HubGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            HUB_GROUP_BORDER_WIDTH,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}
