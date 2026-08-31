package com.example.alcolyze.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.util.formatTime
import java.time.Instant

private val CHART_HEIGHT = 180.dp
private val Y_AXIS_LABEL_WIDTH = 34.dp
private val AXIS_LABEL_GAP = 6.dp
private val PEAK_TAP_TARGET = 40.dp

/**
 * Il grafico dell'andamento, usato nelle pagine di dettaglio Intossicazione ed Euforia. In
 * orizzontale ci sono le ore da adesso, in verticale il valore. Una riga verticale con un
 * pallino segna il punto "adesso"; un secondo pallino (a cerchio vuoto, per distinguerlo) segna
 * il picco previsto e, se toccato, mostra un riquadro con valore e orario. Se non c'è un picco
 * (es. serata senza drink), pallino e riquadro non compaiono.
 */
@Composable
fun TrendChart(
    points: List<Pair<Float, Float>>,
    horizonHours: Float,
    yMax: Float,
    yTicks: List<Float>,
    yTickLabel: (Float) -> String,
    lineColor: Color,
    nowX: Float,
    nowY: Float,
    startTime: Instant,
    peakX: Float? = null,
    peakY: Float? = null,
    peakValueLabel: String = "",
    peakTimeLabel: String = "",
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val nowLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val nowDotHaloColor = MaterialTheme.colorScheme.surface
    val xTickCount = horizonHours.toInt().coerceAtLeast(1)
    val hasPeak = peakX != null && peakY != null
    // Se il picco si sposta (es. dopo aver aggiunto un drink) il riquadro si chiude da solo,
    // invece di restare aperto su un pallino che ormai è altrove.
    var showPeakInfo by remember(peakX, peakY) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(Y_AXIS_LABEL_WIDTH)
                    .height(CHART_HEIGHT),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yTicks.sortedDescending().forEach { tick ->
                    Text(
                        text = yTickLabel(tick),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(AXIS_LABEL_GAP))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT)
            ) {
                val chartWidth = maxWidth

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gridStrokeWidth = 1.dp.toPx()

                    fun xPx(hours: Float) = (hours / horizonHours).coerceIn(0f, 1f) * w
                    fun yPx(value: Float) = h - (value / yMax).coerceIn(0f, 1f) * h

                    yTicks.forEach { tick ->
                        val y = yPx(tick)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = gridStrokeWidth)
                    }
                    for (i in 0..xTickCount) {
                        val x = xPx(i.toFloat())
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = gridStrokeWidth)
                    }

                    if (points.size >= 2) {
                        val path = Path()
                        points.forEachIndexed { index, (hours, value) ->
                            val x = xPx(hours)
                            val y = yPx(value)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    if (peakX != null && peakY != null) {
                        val peakXPx = xPx(peakX)
                        val peakYPx = yPx(peakY)
                        drawCircle(nowDotHaloColor, radius = 9.dp.toPx(), center = Offset(peakXPx, peakYPx))
                        drawCircle(
                            color = lineColor,
                            radius = 9.dp.toPx(),
                            center = Offset(peakXPx, peakYPx),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(peakXPx, peakYPx))
                    }

                    val nowXPx = xPx(nowX)
                    val nowYPx = yPx(nowY)
                    drawLine(
                        color = nowLineColor,
                        start = Offset(nowXPx, 0f),
                        end = Offset(nowXPx, h),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(color = nowDotHaloColor, radius = 7.dp.toPx(), center = Offset(nowXPx, nowYPx))
                    drawCircle(
                        color = lineColor,
                        radius = 7.dp.toPx(),
                        center = Offset(nowXPx, nowYPx),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                if (hasPeak) {
                    val fracX = (peakX!! / horizonHours).coerceIn(0f, 1f)
                    val fracY = (peakY!! / yMax).coerceIn(0f, 1f)
                    val offsetX = (chartWidth * fracX) - PEAK_TAP_TARGET / 2
                    val offsetY = (CHART_HEIGHT * (1f - fracY)) - PEAK_TAP_TARGET / 2
                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(PEAK_TAP_TARGET)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = "Mostra il picco previsto"
                            ) { showPeakInfo = !showPeakInfo }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Y_AXIS_LABEL_WIDTH + AXIS_LABEL_GAP, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..xTickCount) {
                Text(
                    text = formatTime(startTime.plusSeconds(i * 3600L)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasPeak) {
            AnimatedVisibility(visible = showPeakInfo) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Y_AXIS_LABEL_WIDTH + AXIS_LABEL_GAP, top = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = lineColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Picco previsto",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$peakValueLabel  ·  $peakTimeLabel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = lineColor
                        )
                    }
                }
            }
        }
    }
}
