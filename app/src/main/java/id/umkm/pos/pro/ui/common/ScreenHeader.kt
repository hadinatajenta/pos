package id.umkm.pos.pro.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = LocalAppTextStyles.current.screenTitle)
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                content = actions,
            )
        }
    }
}

@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = LocalAppTextStyles.current.sectionHeading,
        modifier = modifier.fillMaxWidth(),
    )
}
