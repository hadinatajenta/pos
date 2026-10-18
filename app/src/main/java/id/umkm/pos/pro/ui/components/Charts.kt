package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

data class ChartPoint(val label: String, val value: Double)

data class ChartSlice(val label: String, val value: Double, val color: Color)

private const val GRID_LINES = 4
private const val MAX_X_LABELS = 7

@Composable
fun SalesLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    if (points.isEmpty()) {
        ChartEmptyState(modifier = modifier, height = height)
        return
    }
    val palette = LocalPosSemanticPalette.current
    val maxValue = points.maxOf { it.value }.coerceAtLeast(1.0)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            YAxisLabels(maxValue = maxValue, height = height)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(height),
            ) {
                val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
                for (gridIndex in 0..GRID_LINES) {
                    val y = size.height * gridIndex / GRID_LINES
                    drawLine(
                        color = palette.chartGrid,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                val offsets = points.mapIndexed { index, point ->
                    val x = if (points.size == 1) size.width / 2f else stepX * index
                    Offset(x = x, y = size.height - (point.value / maxValue * size.height).toFloat())
                }
                if (offsets.size == 1) {
                    val singleOffset = offsets.first()
                    val barWidth = 44.dp.toPx().coerceAtMost(size.width * 0.35f)
                    val barHeight = (size.height - singleOffset.y).coerceAtLeast(4.dp.toPx())
                    drawRoundRect(
                        color = palette.chartArea,
                        topLeft = Offset(singleOffset.x - barWidth / 2f, singleOffset.y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                    )
                    drawLine(
                        color = palette.chartLine,
                        start = Offset(singleOffset.x - barWidth / 2f + 4.dp.toPx(), singleOffset.y),
                        end = Offset(singleOffset.x + barWidth / 2f - 4.dp.toPx(), singleOffset.y),
                        strokeWidth = 3.dp.toPx(),
                    )
                } else if (offsets.size >= 2) {
                    val areaPath = Path().apply {
                        moveTo(offsets.first().x, size.height)
                        offsets.forEach { lineTo(it.x, it.y) }
                        lineTo(offsets.last().x, size.height)
                        close()
                    }
                    drawPath(path = areaPath, color = palette.chartArea)
                    val linePath = Path().apply {
                        offsets.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(path = linePath, color = palette.chartLine, style = Stroke(width = 2.5.dp.toPx()))
                }
                offsets.forEach { offset ->
                    drawCircle(color = palette.chartLine, radius = 4.5.dp.toPx(), center = offset)
                }
            }
        }
        XAxisLabels(points = points, modifier = Modifier.padding(start = PosSpacing.xl))
    }
}

@Composable
fun DailySalesBarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    if (points.isEmpty()) {
        ChartEmptyState(modifier = modifier, height = height)
        return
    }
    val palette = LocalPosSemanticPalette.current
    val maxValue = points.maxOf { it.value }.coerceAtLeast(1.0)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            YAxisLabels(maxValue = maxValue, height = height)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(height),
            ) {
                for (gridIndex in 0..GRID_LINES) {
                    val y = size.height * gridIndex / GRID_LINES
                    drawLine(
                        color = palette.chartGrid,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                val slot = size.width / points.size
                val barWidth = (slot * 0.55f).coerceAtMost(28.dp.toPx())
                points.forEachIndexed { index, point ->
                    val barHeight = (point.value / maxValue * size.height).toFloat()
                    val left = slot * index + (slot - barWidth) / 2f
                    drawRoundRect(
                        color = palette.chartBar,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    )
                }
            }
        }
        XAxisLabels(points = points, modifier = Modifier.padding(start = PosSpacing.xl))
    }
}

@Composable
fun PaymentMethodDonut(
    slices: List<ChartSlice>,
    totalLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
) {
    val total = slices.sumOf { it.value }
    val emptyTrackColor = LocalPosSemanticPalette.current.neutralContainer
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2f
            if (total <= 0.0) {
                drawArc(
                    color = emptyTrackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(width = stroke),
                )
                return@Canvas
            }
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.value / total * 360f).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(width = stroke),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = LocalPosStrings.current.chartSeriesTotal,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AutoSizeNumberText(
                text = totalLabel,
                style = LocalAppTextStyles.current.financialValue,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(max = size * 0.7f),
            )
        }
    }
}

@Composable
fun ChartLegend(
    slices: List<ChartSlice>,
    valueFormatter: (Double, Double) -> String,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }
    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(PosSpacing.sm),
    ) {
        slices.forEach { slice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = slice.color, shape = androidx.compose.foundation.shape.CircleShape),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = PosSpacing.sm),
                ) {
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = valueFormatter(slice.value, total),
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun YAxisLabels(maxValue: Double, height: Dp) {
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier
            .width(44.dp)
            .height(height),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        for (gridIndex in 0..GRID_LINES) {
            val value = maxValue * (GRID_LINES - gridIndex) / GRID_LINES
            Text(
                text = compactRupiah(value, strings),
                style = LocalAppTextStyles.current.tableHeader,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun XAxisLabels(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val indices = xLabelIndices(points.size)
    Row(modifier = modifier.fillMaxWidth()) {
        points.forEachIndexed { index, point ->
            Text(
                text = if (index in indices) point.label else "",
                style = LocalAppTextStyles.current.tableHeader,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChartEmptyState(modifier: Modifier, height: Dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = LocalPosStrings.current.chartEmpty,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun xLabelIndices(count: Int): Set<Int> {
    if (count <= MAX_X_LABELS) return (0 until count).toSet()
    val step = (count - 1).toDouble() / (MAX_X_LABELS - 1)
    return (0 until MAX_X_LABELS).map { (it * step).toInt() }.toSet()
}

fun compactRupiah(value: Double, strings: PosStrings): String = when {
    value >= 1_000_000.0 ->
        String.format(java.util.Locale.US, "%.1f %s", value / 1_000_000.0, strings.chartCompactMillion)

    value >= 1_000.0 ->
        String.format(java.util.Locale.US, "%.0f %s", value / 1_000.0, strings.chartCompactThousand)

    else -> String.format(java.util.Locale.US, "%.0f", value)
}
