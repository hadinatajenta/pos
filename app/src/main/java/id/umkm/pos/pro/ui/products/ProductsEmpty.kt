package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings

private val EMPTY_ILLUSTRATION_SIZE = 140.dp
private val EMPTY_ILLUSTRATION_GLOW_SIZE = 128.dp
private val EMPTY_ILLUSTRATION_TILE_SIZE = 92.dp
private val EMPTY_ILLUSTRATION_TILE_SHAPE = RoundedCornerShape(24.dp)
private val EMPTY_CTA_SHAPE = RoundedCornerShape(16.dp)
private val EMPTY_MESSAGE_MAX_WIDTH = 280.dp

@Composable
internal fun ProductsZeroState(
    onAddFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(EMPTY_ILLUSTRATION_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(EMPTY_ILLUSTRATION_GLOW_SIZE)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ProductPalette.Gold.copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(EMPTY_ILLUSTRATION_TILE_SIZE)
                    .rotate(-6f)
                    .clip(EMPTY_ILLUSTRATION_TILE_SHAPE)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ProductPalette.Brand, ProductPalette.BrandDeep),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_package),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ProductPalette.Gold),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(ProductPalette.Gold.copy(alpha = 0.36f)),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = strings.productsEmptyTitle,
            color = ProductPalette.Ink,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.productsEmptyHint,
            color = ProductPalette.Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = EMPTY_MESSAGE_MAX_WIDTH),
        )
        Spacer(modifier = Modifier.height(22.dp))
        Surface(
            onClick = onAddFirst,
            color = ProductPalette.Gold,
            contentColor = ProductPalette.OnGold,
            shape = EMPTY_CTA_SHAPE,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = strings.productsAddFirst,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
        }
    }
}
