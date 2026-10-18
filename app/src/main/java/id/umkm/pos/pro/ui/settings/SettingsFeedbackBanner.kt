package id.umkm.pos.pro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.asText
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun SettingsFeedbackBanner(state: SettingsUiState) {
    val palette = LocalPosSemanticPalette.current
    state.errorMessage?.let { errorText ->
        Surface(
            color = palette.dangerContainer,
            contentColor = palette.danger,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(PosSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_alert),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = palette.danger,
                )
                Text(
                    text = errorText.asText(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.danger,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    state.message?.let { successText ->
        Surface(
            color = palette.successContainer,
            contentColor = palette.success,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(PosSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_check),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = palette.success,
                )
                Text(
                    text = successText.asText(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.success,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
