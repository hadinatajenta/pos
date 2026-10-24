package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizeNumberText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 11.sp,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start,
) {
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = constraints.maxWidth
        val fittedFontSize = remember(text, availableWidth, style, minFontSize) {
            if (availableWidth <= 0 || text.isEmpty()) return@remember style.fontSize
            var candidate = style.fontSize
            val step = 0.5f
            while (candidate.value > minFontSize.value) {
                val width = textMeasurer.measure(
                    text = text,
                    style = style.copy(fontSize = candidate),
                    maxLines = maxLines,
                ).size.width
                if (width <= availableWidth) break
                candidate = (candidate.value - step).sp
            }
            candidate
        }
        Text(
            text = text,
            style = style.copy(fontSize = fittedFontSize),
            color = color,
            maxLines = maxLines,
            textAlign = textAlign,
            modifier = if (textAlign == TextAlign.Start) Modifier else Modifier.fillMaxWidth(),
        )
    }
}
