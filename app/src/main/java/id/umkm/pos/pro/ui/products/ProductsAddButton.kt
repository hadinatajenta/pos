package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun AddProductButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(PosColors.ForestGreen)
            .clickable(onClickLabel = "Tambah Produk", onClick = onClick)
            .heightIn(min = PosSizes.touchTarget)
            .padding(horizontal = PosSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
            tint = Color.White,
        )
        Text(
            text = "Tambah Produk",
            style = LocalAppTextStyles.current.buttonLabel,
            color = Color.White,
            maxLines = 1,
        )
    }
}
