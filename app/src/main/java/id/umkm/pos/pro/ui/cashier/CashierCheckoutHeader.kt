package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.products.ProductPalette
import id.umkm.pos.pro.ui.products.ProductRadii

private val CHECKOUT_HEADER_HORIZONTAL_PADDING = 20.dp
private val CHECKOUT_HEADER_TOP_PADDING = 8.dp
private val CHECKOUT_HEADER_BOTTOM_PADDING = 20.dp
private val CLAY_BUTTON_SIZE = 44.dp
private val CLAY_BUTTON_SHAPE = CircleShape

@Composable
internal fun CashierDashedDivider(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        val dash = 6.dp.toPx()
        val gap = 5.dp.toPx()
        drawLine(
            color = ProductPalette.Line,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = with(density) { 1.dp.toPx() },
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap)),
        )
    }
}

@Composable
internal fun CashierPageHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = CHECKOUT_HEADER_HORIZONTAL_PADDING,
                end = CHECKOUT_HEADER_HORIZONTAL_PADDING,
                top = CHECKOUT_HEADER_TOP_PADDING,
                bottom = CHECKOUT_HEADER_BOTTOM_PADDING,
            ),
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            color = ProductPalette.Ink,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(CLAY_BUTTON_SIZE)
                .shadow(
                    elevation = 6.dp,
                    shape = CLAY_BUTTON_SHAPE,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.10f),
                )
                .clip(CLAY_BUTTON_SHAPE)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ProductPalette.Surface,
                            ProductPalette.Background,
                        ),
                    ),
                )
                .clickable(
                    role = Role.Button,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = LocalPosStrings.current.actionBack,
                tint = ProductPalette.Ink,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
