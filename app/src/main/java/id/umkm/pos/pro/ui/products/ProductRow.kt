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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.StockStatus
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.stockStatusLabel
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.PosTextStyles
import id.umkm.pos.pro.ui.components.stockStatusVisuals
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private val PRODUCT_ROW_THUMBNAIL_SIZE = 52.dp
private val STOCK_PILL_ICON_SIZE = 12.dp
private val STOCK_PILL_TEXT_MAX_WIDTH = 84.dp
private val PRODUCT_PRICE_TEXT_STYLE = PosTextStyles.financialValue.copy(fontSize = 15.sp, lineHeight = 20.sp)

@Composable
internal fun ProductRow(
    productItem: ProductListItem,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val product = productItem.product
    var isMenuExpanded by remember { mutableStateOf(false) }
    val strings = LocalPosStrings.current
    PosCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = strings.productsEditNamed(product.name),
                onClick = { actions.onOpenEditForm(product.id) },
            ),
        contentPadding = PosSpacing.sm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            ProductThumbnail(
                category = product.category,
                size = PRODUCT_ROW_THUMBNAIL_SIZE,
                imagePath = product.imagePath,
                productName = product.name,
            )
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
                    text = product.category?.takeIf { category -> category.isNotBlank() } ?: strings.commonNoCategory,
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
                            painter = painterResource(R.drawable.ic_ellipsis_vertical),
                            contentDescription = strings.productsMenuNamed(product.name),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.actionEdit) },
                            onClick = {
                                isMenuExpanded = false
                                actions.onOpenEditForm(product.id)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(strings.actionDelete) },
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
    val strings = LocalPosStrings.current
    val visuals = stockStatusVisuals(stockStatus)
    val quantityLabel = strings.productsStockSummary(formatQuantity(stockBaseQty))
    Box(
        modifier = Modifier
            .background(color = visuals.containerColor, shape = MaterialTheme.shapes.small)
            .clearAndSetSemantics { contentDescription = "$quantityLabel, ${stockStatusLabel(stockStatus, strings)}" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.xs, vertical = PosSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Icon(
                painter = painterResource(visuals.iconRes),
                contentDescription = null,
                modifier = Modifier.size(STOCK_PILL_ICON_SIZE),
                tint = visuals.contentColor,
            )
            AutoSizeNumberText(
                text = quantityLabel,
                style = MaterialTheme.typography.labelMedium,
                color = visuals.contentColor,
                modifier = Modifier.widthIn(max = STOCK_PILL_TEXT_MAX_WIDTH),
            )
        }
    }
}
