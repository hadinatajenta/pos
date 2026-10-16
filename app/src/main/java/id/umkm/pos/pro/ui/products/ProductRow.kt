package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.stockStatusLabel
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.PosTextStyles

private val PRODUCT_ROW_THUMBNAIL_SIZE = 64.dp
private val STOCK_PILL_ICON_SIZE = 12.dp
private val STOCK_PILL_TEXT_MAX_WIDTH = 84.dp
private val PRODUCT_PRICE_TEXT_STYLE = PosTextStyles.financialValue.copy(fontSize = 15.sp, lineHeight = 20.sp)

@Composable
internal fun ProductRow(
    productItem: ProductListItem,
    actions: ProductsActions,
) {
    val product = productItem.product
    var isMenuExpanded by remember { mutableStateOf(false) }
    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Ubah ${product.name}", onClick = { actions.onOpenEditForm(product.id) }),
        contentPadding = PosSpacing.sm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            ProductThumbnail(category = product.category, size = PRODUCT_ROW_THUMBNAIL_SIZE)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.category?.takeIf { category -> category.isNotBlank() } ?: "Tanpa kategori",
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AutoSizeNumberText(
                    text = Rupiah.format(product.basePrice),
                    style = PRODUCT_PRICE_TEXT_STYLE,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                ProductStockPill(
                    stockStatus = productItem.stockStatus,
                    stockBaseQty = product.stockBaseQty,
                )
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Menu ${product.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ubah") },
                            onClick = {
                                isMenuExpanded = false
                                actions.onOpenEditForm(product.id)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus") },
                            onClick = {
                                isMenuExpanded = false
                                actions.onRequestDelete(product.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductStockPill(
    stockStatus: StockStatus,
    stockBaseQty: Double,
) {
    val palette = LocalPosSemanticPalette.current
    val containerColor: Color
    val contentColor: Color
    val icon: ImageVector
    when (stockStatus) {
        StockStatus.IN_STOCK -> {
            containerColor = palette.successContainer
            contentColor = palette.success
            icon = Icons.Filled.CheckCircle
        }

        StockStatus.LOW_STOCK -> {
            containerColor = palette.warningContainer
            contentColor = palette.warning
            icon = Icons.Filled.WarningAmber
        }

        StockStatus.OUT_OF_STOCK -> {
            containerColor = palette.dangerContainer
            contentColor = palette.danger
            icon = Icons.Filled.Cancel
        }

        StockStatus.OVERSOLD -> {
            containerColor = palette.dangerContainer
            contentColor = palette.danger
            icon = Icons.Filled.TrendingDown
        }
    }
    val quantityLabel = "Stok: ${formatQuantity(stockBaseQty)}"
    Box(
        modifier = Modifier
            .background(color = containerColor, shape = MaterialTheme.shapes.small)
            .clearAndSetSemantics { contentDescription = "$quantityLabel, ${stockStatusLabel(stockStatus)}" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.xs, vertical = PosSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(STOCK_PILL_ICON_SIZE),
                tint = contentColor,
            )
            AutoSizeNumberText(
                text = quantityLabel,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                modifier = Modifier.widthIn(max = STOCK_PILL_TEXT_MAX_WIDTH),
            )
        }
    }
}
