package id.umkm.pos.pro.ui.cashier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.products.ProductCard
import id.umkm.pos.pro.ui.products.ProductCardView
import id.umkm.pos.pro.ui.products.ProductPalette
import id.umkm.pos.pro.ui.products.ProductRadii
import id.umkm.pos.pro.ui.products.categoryColorKey
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosFontFamily
import id.umkm.pos.pro.ui.theme.PosShapes
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import kotlin.math.roundToInt

private const val FLOATING_CART_BAR_ELEVATION = 6

private val CART_LINE_GAP = 12.dp
private val CART_LINE_SHAPE = RoundedCornerShape(18.dp)
private val CART_LINE_SHADOW = 4.dp
private val CART_LINE_PADDING = 16.dp
private val CART_PAGE_HORIZONTAL_PADDING = 20.dp
private val STEPPER_SIZE = 40.dp
private val STEPPER_ICON_SIZE = 18.dp
private val STEPPER_QUANTITY_MIN_WIDTH = 32.dp
private val SUMMARY_SHAPE = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
private val SUMMARY_PADDING = 20.dp
private val SUMMARY_SHADOW = 8.dp
private val SUMMARY_BUTTON_MIN_HEIGHT = 52.dp
private val TRAILING_VALUE_MAX_WIDTH = 176.dp
private val TOTAL_VALUE_MAX_WIDTH = 208.dp
private val ProductPriceTextStyle = TextStyle(
    fontFamily = PosFontFamily,
    fontSize = 15.sp,
    lineHeight = 19.sp,
    fontWeight = FontWeight.ExtraBold,
    fontFeatureSettings = "tnum",
)

@Composable
internal fun CashierCartPage(
    state: CashierUiState,
    actions: CashierActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Column(modifier = modifier.fillMaxSize()) {
        CashierPageHeader(title = strings.cashierCartTitle, onBack = onBack)
        if (state.cartLines.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_shopping_cart,
                title = strings.cashierEmptyCartTitle,
                supportingText = strings.cashierEmptyCartMsg,
                modifier = Modifier.weight(1f),
            )
        } else {
            CartLineList(
                lines = state.cartLines,
                actions = actions,
                contentPadding = PaddingValues(
                    start = CART_PAGE_HORIZONTAL_PADDING,
                    end = CART_PAGE_HORIZONTAL_PADDING,
                    top = PosSpacing.sm,
                    bottom = PosSpacing.lg,
                ),
                modifier = Modifier.weight(1f),
            )
            state.pricedCart?.let { pricedCart ->
                CartSummary(
                    pricedCart = pricedCart,
                    isShiftActive = state.activeShift != null,
                    onOpenQuickShift = { actions.onOpenQuickShift {} },
                    onOpenShift = { actions.onOpenShift(state.lineCount) { actions.onClearCart() } },
                    onProcess = actions.onOpenPaymentSheet,
                )
            }
        }
    }
}

@Composable
internal fun CashierCartPane(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = strings.cashierCartTitle,
                style = LocalAppTextStyles.current.sectionHeading,
                color = ProductPalette.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.lineCount > 0) {
                SecondaryTextAction(
                    text = strings.cashierClearCart,
                    onClick = actions.onClearCart,
                )
            }
        }
        if (state.cartLines.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_shopping_cart,
                title = strings.cashierEmptyCartTitle,
                supportingText = strings.cashierEmptyCartMsg,
                modifier = Modifier.weight(1f),
            )
        } else {
            CartLineList(
                lines = state.cartLines,
                actions = actions,
                contentPadding = PaddingValues(
                    start = PosSpacing.md,
                    end = PosSpacing.md,
                    top = PosSpacing.sm,
                    bottom = PosSpacing.lg,
                ),
                modifier = Modifier.weight(1f),
            )
            state.pricedCart?.let { pricedCart ->
                CartSummary(
                    pricedCart = pricedCart,
                    isShiftActive = state.activeShift != null,
                    onOpenQuickShift = { actions.onOpenQuickShift {} },
                    onOpenShift = { actions.onOpenShift(state.lineCount) { actions.onClearCart() } },
                    onProcess = actions.onOpenPaymentSheet,
                )
            }
        }
    }
}

@Composable
internal fun CashierCartBar(
    state: CashierUiState,
    actions: CashierActions,
    modifier: Modifier = Modifier,
) {
    if (state.lineCount == 0) return
    val strings = LocalPosStrings.current
    Surface(
        color = PosColors.ForestGreen,
        contentColor = Color.White,
        shape = PosShapes.large,
        shadowElevation = FLOATING_CART_BAR_ELEVATION.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm)
            .clickable(onClickLabel = strings.cashierOpenCart, onClick = actions.onOpenCartSheet),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PosSizes.touchTarget)
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = strings.cashierItemCountLine(
                        formatQuantity(state.totalQuantity),
                        state.lineCount,
                    ),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AutoSizeNumberText(
                    text = Rupiah.format(state.grandTotal),
                    style = LocalAppTextStyles.current.buttonLabel.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FilledTonalButton(
                onClick = actions.onOpenCartSheet,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White,
                    contentColor = PosColors.ForestGreen,
                ),
                contentPadding = PaddingValues(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
            ) {
                Text(
                    text = strings.cashierOpenCart,
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CartLineList(
    lines: List<CashierCartLine>,
    actions: CashierActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(CART_LINE_GAP),
    ) {
        items(items = lines, key = { line -> line.lineId }) { line ->
            CartLineCard(line = line, actions = actions)
        }
    }
}

@Composable
private fun CartLineCard(
    line: CashierCartLine,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    val revealWidth = 72.dp
    val revealWidthPx = with(LocalDensity.current) { revealWidth.toPx() }
    var dragOffset by remember(line.lineId) { mutableFloatStateOf(0f) }
    var isDragging by remember(line.lineId) { mutableStateOf(false) }
    val settledOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(durationMillis = 180),
        label = "cartLineSwipe",
    )
    val displayedOffset = if (isDragging) dragOffset else settledOffset
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ProductRadii.card)
                .background(ProductPalette.OutContainer),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ProductPalette.Surface)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = strings.cashierRemoveLine(line.productName),
                        onClick = { actions.onRemoveLine(line.lineId) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = strings.cashierRemoveLine(line.productName),
                    tint = ProductPalette.OutText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        ProductCard(
            card = ProductCardView(
                id = line.productId,
                name = line.productName,
                categoryName = line.categoryName,
                categoryColorKey = categoryColorKey(line.categoryName),
                priceFormatted = Rupiah.format(line.unitPrice),
                stock = line.stock,
                iconOrImageUrl = line.imagePath ?: line.icon,
                imagePath = line.imagePath,
                icon = line.icon,
                isActive = true,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(displayedOffset.roundToInt(), 0) }
                .pointerInput(line.lineId) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceIn(-revealWidthPx, 0f)
                        },
                        onDragEnd = {
                            isDragging = false
                            dragOffset = if (dragOffset <= -revealWidthPx / 2f) -revealWidthPx else 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = 0f
                        },
                    )
                },
            metaLabel = "${strings.cashierSubtotal}: ${Rupiah.format(line.lineSubtotal)}",
            trailingContent = {
                QuantityStepper(
                    quantity = line.quantity,
                    productName = line.productName,
                    onDecrease = { actions.onDecreaseQuantity(line.lineId) },
                    onIncrease = { actions.onIncreaseQuantity(line.lineId) },
                )
            },
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Double,
    productName: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        StepperButton(
            iconRes = R.drawable.ic_minus,
            contentDescription = strings.cashierDecreaseLineQuantity(productName),
            onClick = onDecrease,
        )
        Text(
            text = formatQuantity(quantity),
            style = LocalAppTextStyles.current.financialValue,
            color = ProductPalette.Ink,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = STEPPER_QUANTITY_MIN_WIDTH),
        )
        StepperButton(
            iconRes = R.drawable.ic_plus,
            contentDescription = strings.cashierIncreaseLineQuantity(productName),
            onClick = onIncrease,
        )
    }
}

@Composable
private fun StepperButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(STEPPER_SIZE)
            .clip(CircleShape)
            .background(ProductPalette.Background)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = ProductPalette.Ink,
            modifier = Modifier.size(STEPPER_ICON_SIZE),
        )
    }
}

@Composable
private fun CartSummary(
    pricedCart: PricedCart,
    isShiftActive: Boolean,
    onOpenQuickShift: () -> Unit,
    onOpenShift: () -> Unit,
    onProcess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Surface(
        color = ProductPalette.Surface,
        contentColor = ProductPalette.Ink,
        shape = SUMMARY_SHAPE,
        shadowElevation = SUMMARY_SHADOW,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SUMMARY_PADDING),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            SummaryRow(
                label = strings.cashierSubtotal,
                value = Rupiah.format(pricedCart.subtotal),
                isEmphasized = false,
            )
            CashierDashedDivider()
            if (!isShiftActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = strings.cashierShiftNotOpen,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = ProductPalette.MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    SecondaryTextAction(
                        text = strings.shiftQuickOpenSubmit,
                        onClick = onOpenQuickShift,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    SecondaryTextAction(
                        text = strings.cashierShiftSwitchAction,
                        onClick = onOpenShift,
                    )
                }
            }
            SummaryRow(
                label = strings.cashierTotal,
                value = Rupiah.format(pricedCart.grandTotal),
                isEmphasized = true,
            )
            Button(
                onClick = onProcess,
                shape = ProductRadii.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PosColors.ForestGreen,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(vertical = PosSpacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SUMMARY_BUTTON_MIN_HEIGHT),
            ) {
                Text(
                    text = strings.cashierProcess,
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isEmphasized: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (isEmphasized) {
                LocalAppTextStyles.current.sectionHeading
            } else {
                LocalAppTextStyles.current.supportingLabel
            },
            color = if (isEmphasized) ProductPalette.Ink else ProductPalette.MutedText,
            maxLines = 1,
        )
        AutoSizeNumberText(
            text = value,
            style = if (isEmphasized) {
                LocalAppTextStyles.current.financialValueLarge
            } else {
                LocalAppTextStyles.current.financialValue
            },
            modifier = Modifier.widthIn(
                max = if (isEmphasized) TOTAL_VALUE_MAX_WIDTH else TRAILING_VALUE_MAX_WIDTH,
            ),
            color = ProductPalette.Ink,
        )
    }
}
