package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val BRAND_MARK_SIZE = 30.dp

@Composable
internal fun ProductsHeader(
    onCreateProduct: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PosSpacing.md, end = PosSpacing.md, top = PosSpacing.md, bottom = PosSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(BRAND_MARK_SIZE)
                    .clip(MaterialTheme.shapes.small)
                    .background(PosColors.Gold),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PointOfSale,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PosColors.ForestGreen,
                )
            }
            Text(
                text = "POS UMKM Pro",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = PosSpacing.sm),
            )
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifikasi",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PosSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Manajemen Produk",
                style = LocalAppTextStyles.current.screenTitle,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(PosSpacing.sm))
            AddProductButton(onClick = onCreateProduct)
        }
        Text(
            text = "Kelola produk jualan Anda dengan mudah. Tambah, edit, atau atur stok produk.",
            style = LocalAppTextStyles.current.supportingLabel,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = PosSpacing.xxs),
        )
    }
}

@Composable
private fun AddProductButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(PosColors.Gold)
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
            tint = PosColors.ForestGreen,
        )
        Text(
            text = "Tambah Produk",
            style = LocalAppTextStyles.current.buttonLabel,
            color = PosColors.ForestGreen,
            maxLines = 1,
        )
    }
}
