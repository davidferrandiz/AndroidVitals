package com.ferryapps.vitals.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ferryapps.vitals.designsystem.theme.TabularNumbers
import com.ferryapps.vitals.designsystem.theme.VitalsTheme

/**
 * Gráfica mínima de los últimos N valores. El más reciente queda en el extremo final
 * (derecha, o izquierda en idiomas RTL).
 *
 * @param maxValue tope del eje Y; null = el máximo de los datos.
 */
@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    secondaryValues: List<Float>? = null,
    secondaryColor: Color = color.copy(alpha = 0.6f),
    capacity: Int = values.size,
    height: Dp = 48.dp,
    fill: Boolean = true,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val baseline = VitalsTheme.palette.divider
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
    ) {
        drawLine(baseline, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1.dp.toPx())
        val top = maxValue ?: maxOf(values.maxOrNull() ?: 0f, secondaryValues?.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val slots = maxOf(capacity, values.size, secondaryValues?.size ?: 0).coerceAtLeast(2)
        if (secondaryValues != null && secondaryValues.size >= 2) {
            drawSeries(secondaryValues, slots, top, secondaryColor, fill = false, dashed = true, rtl = rtl)
        }
        if (values.size >= 2) drawSeries(values, slots, top, color, fill = fill, dashed = false, rtl = rtl)
    }
}

private fun DrawScope.drawSeries(
    values: List<Float>,
    slots: Int,
    top: Float,
    color: Color,
    fill: Boolean,
    dashed: Boolean,
    rtl: Boolean,
) {
    val stepX = size.width / (slots - 1)
    val usableHeight = size.height - 2.dp.toPx()
    // Alineado al final: el último valor siempre en el borde "ahora".
    val startSlot = slots - values.size
    fun x(i: Int): Float {
        val ltr = (startSlot + i) * stepX
        return if (rtl) size.width - ltr else ltr
    }
    fun y(v: Float): Float = size.height - 1.dp.toPx() - (v / top).coerceIn(0f, 1f) * usableHeight

    val line = Path().apply {
        values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
    }
    if (fill) {
        val area = Path().apply {
            addPath(line)
            lineTo(x(values.lastIndex), size.height)
            lineTo(x(0), size.height)
            close()
        }
        drawPath(area, color.copy(alpha = 0.12f))
    }
    drawPath(
        line,
        color,
        style = Stroke(
            width = (if (dashed) 1.5f else 2f) * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(3f * density, 3f * density)) else null
        )
    )
}

/** Barra de progreso fina con la pista en el color de acento al 15 %. */
@Composable
fun LinearMeter(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(shape)
                .background(color)
        )
    }
}

/** Anillo de progreso con el valor en el centro (nivel de batería). */
@Composable
fun RingGauge(
    progress: Float,
    color: Color,
    centerText: String,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    strokeWidth: Dp = 10.dp,
    caption: String? = null,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(color.copy(alpha = 0.16f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerText,
                style = MaterialTheme.typography.headlineSmall.merge(TabularNumbers),
                fontWeight = FontWeight.ExtraBold
            )
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

/** Punto de una serie temporal. [highlighted] sombrea el tramo que empieza en él (cargando). */
data class TimePoint(val timeMs: Long, val value: Float, val highlighted: Boolean = false)

/**
 * Serie temporal con huecos: a diferencia de [Sparkline], el eje X es tiempo real, así que
 * un periodo sin muestras (Doze, móvil apagado) se ve como un hueco y no como una línea recta.
 * Respeta RTL igual que [Sparkline]: "ahora" queda en el borde final.
 */
@Composable
fun TimeSeriesChart(
    points: List<TimePoint>,
    startMs: Long,
    endMs: Long,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    maxValue: Float = 100f,
    highlightColor: Color = color,
    maxGapMs: Long = 2 * 60 * 60 * 1000L,
    height: Dp = 96.dp,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val grid = VitalsTheme.palette.divider
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
    ) {
        val span = (endMs - startMs).coerceAtLeast(1L).toFloat()
        val usableHeight = size.height - 2.dp.toPx()
        fun x(t: Long): Float {
            val ltr = ((t - startMs) / span).coerceIn(0f, 1f) * size.width
            return if (rtl) size.width - ltr else ltr
        }
        fun y(v: Float): Float = size.height - 1.dp.toPx() - (v / maxValue).coerceIn(0f, 1f) * usableHeight

        val dash = PathEffect.dashPathEffect(floatArrayOf(3f * density, 4f * density))
        listOf(0.5f, 1f).forEach { fraction ->
            val gy = y(maxValue * fraction)
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        }
        drawLine(grid, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1.dp.toPx())

        val sorted = points.sortedBy { it.timeMs }
        // Tramos cargando: banda vertical suave detrás de la línea.
        sorted.zipWithNext().forEach { (a, b) ->
            if (a.highlighted && b.timeMs - a.timeMs <= maxGapMs) {
                val left = minOf(x(a.timeMs), x(b.timeMs))
                val right = maxOf(x(a.timeMs), x(b.timeMs))
                drawRect(
                    highlightColor.copy(alpha = 0.10f),
                    topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(right - left, size.height)
                )
            }
        }

        // Grupos contiguos: cada uno con su línea y su relleno.
        val groups = mutableListOf<MutableList<TimePoint>>()
        sorted.forEach { point ->
            val last = groups.lastOrNull()?.lastOrNull()
            if (last == null || point.timeMs - last.timeMs > maxGapMs) groups += mutableListOf(point) else groups.last() += point
        }
        groups.forEach { group ->
            if (group.size == 1) {
                drawCircle(color, radius = 2.5f * density, center = Offset(x(group[0].timeMs), y(group[0].value)))
                return@forEach
            }
            val line = Path().apply {
                group.forEachIndexed { i, p -> if (i == 0) moveTo(x(p.timeMs), y(p.value)) else lineTo(x(p.timeMs), y(p.value)) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(group.last().timeMs), size.height)
                lineTo(x(group.first().timeMs), size.height)
                close()
            }
            drawPath(area, color.copy(alpha = 0.12f))
            drawPath(line, color, style = Stroke(width = 2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
