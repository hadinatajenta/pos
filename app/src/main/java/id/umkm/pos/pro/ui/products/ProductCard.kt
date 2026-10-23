package id.umkm.pos.pro.ui.products

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.productCategoryIconRes
import id.umkm.pos.pro.ui.components.rememberFileImageBitmap
import id.umkm.pos.pro.ui.theme.PosFontFamily
import kotlinx.coroutines.delay

private val CARD_TILE_SIZE = 64.dp
private val CARD_MENU_VISUAL_SIZE = 30.dp
private val CARD_MENU_TOUCH_SIZE = 44.dp
private const val ENTRANCE_STAGGER_MILLIS = 35L
private const val ENTRANCE_MAX_STAGGER_STEPS = 12

@Composable
internal fun ProductCard(
    card: ProductCardView,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    metaLabel: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onToggleArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    entranceIndex: Int = 0,
    animateEntrance: Boolean = false,
) {
    val strings = LocalPosStrings.current
    val reducedMotion = rememberReducedMotion()
    var isVisible by remember(card.id, animateEntrance) {
        mutableStateOf(!animateEntrance || reducedMotion)
    }
    LaunchedEffect(card.id, animateEntrance, reducedMotion) {
        if (isVisible) return@LaunchedEffect
        delay(entranceIndex.coerceAtMost(ENTRANCE_MAX_STAGGER_STEPS) * ENTRANCE_STAGGER_MILLIS)
        isVisible = true
    }
    val entranceProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "productCardEntrance",
    )
    val entranceOffset = with(androidx.compose.ui.platform.LocalDensity.current) { 14.dp.toPx() }
    ProductCardSurface(
        card = card,
        modifier = modifier.graphicsLayer {
            alpha = entranceProgress
            translationY = (1f - entranceProgress) * entranceOffset
        },
        isSelected = isSelected,
        onClick = onClick,
        onClickLabel = onClickLabel ?: strings.productsEditNamed(card.name),
        metaLabel = metaLabel,
        trailingContent = trailingContent,
        onDuplicate = onDuplicate,
        onToggleArchive = onToggleArchive,
        onDelete = onDelete,
        reducedMotion = reducedMotion,
    )
}

@Composable
private fun ProductCardSurface(
    card: ProductCardView,
    modifier: Modifier,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    onClickLabel: String,
    metaLabel: String?,
    trailingContent: (@Composable () -> Unit)?,
    onDuplicate: (() -> Unit)?,
    onToggleArchive: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    reducedMotion: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(150),
        label = "productCardPress",
    )
    val clickableModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
    }
    Surface(
        color = ProductPalette.Surface,
        contentColor = ProductPalette.Ink,
        shape = ProductRadii.card,
        border = if (isSelected) BorderStroke(2.dp, ProductPalette.Focus) else null,
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 4.dp,
                shape = ProductRadii.card,
                clip = false,
                ambientColor = ProductPalette.Ink.copy(alpha = 0.08f),
                spotColor = ProductPalette.Ink.copy(alpha = 0.08f),
            )
            .then(clickableModifier),
    ) {
        ProductCardRowContent(
            card = card,
            metaLabel = metaLabel,
            trailingContent = trailingContent,
            onDuplicate = onDuplicate,
            onToggleArchive = onToggleArchive,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun ProductCardRowContent(
    card: ProductCardView,
    metaLabel: String?,
    trailingContent: (@Composable () -> Unit)?,
    onDuplicate: (() -> Unit)?,
    onToggleArchive: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductCardTile(card = card)
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = card.name,
                color = ProductPalette.Ink,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.priceFormatted,
                color = ProductPalette.Price,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontFamily = PosFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
            )
            Text(
                text = metaLabel ?: card.rowMetaLabel(
                    noCategory = strings.commonNoCategory,
                    stockLabel = strings.productsStockSummary(formatQuantity(card.stock)),
                    archivedLabel = strings.productsArchivedLabel,
                ),
                color = ProductPalette.MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingContent?.invoke()
        if (onDuplicate != null || onToggleArchive != null || onDelete != null) {
            ProductCardMenu(
                productName = card.name,
                isArchived = !card.isActive,
                onDuplicate = onDuplicate,
                onToggleArchive = onToggleArchive,
                onDelete = onDelete,
                onLightSurface = true,
            )
        }
    }
}

@Composable
private fun ProductCardTile(card: ProductCardView) {
    val image = rememberFileImageBitmap(card.imagePath)
    val gradient = categoryGradient(card.categoryColorKey)
    Box(
        modifier = Modifier
            .size(CARD_TILE_SIZE)
            .clip(ProductRadii.tile)
            .background(
                Brush.linearGradient(
                    colors = listOf(gradient.first, gradient.second),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.52f,
                    center = Offset(size.width * 0.98f, size.height * 0.04f),
                )
            },
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            ProductTileArtwork(card = card, iconSize = 30.dp)
        }
    }
}

@Composable
private fun ProductTileArtwork(card: ProductCardView, iconSize: Dp) {
    val icon = card.icon
    if (!icon.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = icon,
                fontSize = iconSize.value.sp,
                lineHeight = (iconSize.value * 1.1f).sp,
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(productCategoryIconRes(card.categoryName)),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = ProductPalette.Ink.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun ProductCardMenu(
    productName: String,
    isArchived: Boolean,
    onDuplicate: (() -> Unit)?,
    onToggleArchive: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val strings = LocalPosStrings.current
    Box(modifier = modifier.size(CARD_MENU_TOUCH_SIZE), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(CARD_MENU_VISUAL_SIZE)
                .clip(CircleShape)
                .background(
                    if (onLightSurface) ProductPalette.Background else Color.White.copy(alpha = 0.30f),
                )
                .clickable(
                    onClickLabel = strings.productsMenuNamed(productName),
                    onClick = { isExpanded = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ellipsis_vertical),
                contentDescription = strings.productsMenuNamed(productName),
                tint = if (onLightSurface) ProductPalette.Muted else ProductPalette.Ink,
                modifier = Modifier.size(17.dp),
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = ProductPalette.Surface,
        ) {
            if (onDuplicate != null) {
                DropdownMenuItem(
                    text = { Text(strings.productsDuplicate, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        isExpanded = false
                        onDuplicate()
                    },
                )
            }
            if (onToggleArchive != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isArchived) strings.productsUnarchive else strings.productsArchive,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onToggleArchive()
                    },
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = strings.actionDelete,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

private fun ProductCardView.rowMetaLabel(
    noCategory: String,
    stockLabel: String,
    archivedLabel: String,
): String = buildString {
    append(categoryName.takeIf { value -> value.isNotBlank() } ?: noCategory)
    append(" · ")
    append(stockLabel)
    if (!isActive) {
        append(" · ")
        append(archivedLabel)
    }
}
