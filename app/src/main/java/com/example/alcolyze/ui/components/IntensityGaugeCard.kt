package com.example.alcolyze.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.theme.ZoneDanger
import com.example.alcolyze.ui.theme.ZoneGolden
import com.example.alcolyze.ui.theme.ZoneSober
import com.example.alcolyze.ui.theme.ZoneWarmingUp
import com.example.alcolyze.ui.theme.toColor
import com.example.alcolyze.ui.util.formatIntoxicationGauge
import com.example.alcolyze.utils.BacCalculator
import com.example.alcolyze.utils.Trend
import kotlin.math.cos
import kotlin.math.sin

private val GAUGE_TRACK_START_ANGLE = 125f
private val GAUGE_TRACK_SWEEP_ANGLE = 290f

/**
 * Il riquadro in home con i due indicatori circolari: Intossicazione mostra il tasso alcolemico
 * (0-4), colorato come le fasce della barra Sicurezza; Euforia è un punteggio da 0 a 10. Su
 * ciascuno c'è anche un piccolo segno fisso nel punto del picco previsto.
 */
@Composable
fun IntensityGaugeCard(
    bac: Double,
    buzzScore: Double,
    trend: Trend,
    buzzTrend: Trend,
    projectedPeakBac: Double,
    projectedPeakEuphoria: Double,
    modifier: Modifier = Modifier,
    onClickIntossicazione: (() -> Unit)? = null,
    onClickEuforia: (() -> Unit)? = null
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GaugeColumn(
                modifier = Modifier
                    .weight(1f)
                    .let { if (onClickIntossicazione != null) it.clickable(onClick = onClickIntossicazione) else it },
                label = "Intossicazione (g/L)",
                valueText = formatIntoxicationGauge(bac),
                dialColor = BacCalculator.safetyZoneFor(bac).toColor(),
                fraction = (bac / BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L).coerceIn(0.0, 1.0).toFloat(),
                signed = false,
                peakFraction = (projectedPeakBac / BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L).coerceIn(0.0, 1.0).toFloat(),
                trendPill = { TrendPill(trend = trend, risingIsGood = false) }
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            GaugeColumn(
                modifier = Modifier
                    .weight(1f)
                    .let { if (onClickEuforia != null) it.clickable(onClick = onClickEuforia) else it },
                label = "Euforia",
                valueText = "%.1f".format(buzzScore),
                dialColor = euphoriaColorFor(buzzScore),
                fraction = (buzzScore / BacCalculator.EUPHORIA_SCORE_MAX).coerceIn(0.0, 1.0).toFloat(),
                signed = false,
                peakFraction = (projectedPeakEuphoria / BacCalculator.EUPHORIA_SCORE_MAX).coerceIn(0.0, 1.0).toFloat(),
                trendPill = { TrendPill(trend = buzzTrend, risingIsGood = true) }
            )
        }
    }
}

/** Un singolo indicatore circolare con valore, etichetta e freccetta. Usato anche nelle pagine di dettaglio. */
@Composable
fun GaugeColumn(
    label: String,
    valueText: String,
    dialColor: Color,
    fraction: Float,
    signed: Boolean,
    modifier: Modifier = Modifier,
    signPositive: Boolean = true,
    peakFraction: Float? = null,
    dialSize: Dp = 130.dp,
    trendPill: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(dialSize)) {
            GaugeDial(
                fraction = fraction,
                signed = signed,
                signPositive = signPositive,
                color = dialColor,
                peakFraction = peakFraction,
                modifier = Modifier.size(dialSize)
            )
            Text(text = valueText, style = MaterialTheme.typography.headlineMedium)
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        trendPill()
    }
}

@Composable
private fun GaugeDial(
    fraction: Float,
    signed: Boolean,
    signPositive: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    peakFraction: Float? = null
) {
    // Colore dell'arco di sfondo: scelto così che si distingua bene dallo sfondo del riquadro
    // anche col tema chiaro.
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val peakColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val trackStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        // Estremità piatta per l'arco colorato: con l'estremità arrotondata sporgerebbe un po'
        // oltre il valore reale e coprirebbe il segno del picco quando ci si avvicina.
        val valueStroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        drawArc(
            color = trackColor,
            startAngle = GAUGE_TRACK_START_ANGLE,
            sweepAngle = GAUGE_TRACK_SWEEP_ANGLE,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = trackStroke
        )

        if (signed) {
            val half = GAUGE_TRACK_SWEEP_ANGLE / 2
            val center = GAUGE_TRACK_START_ANGLE + half
            val filledSweep = half * fraction
            if (filledSweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = if (signPositive) center else center - filledSweep,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = valueStroke
                )
            }
        } else {
            val filledSweep = GAUGE_TRACK_SWEEP_ANGLE * fraction
            if (filledSweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = GAUGE_TRACK_START_ANGLE,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = valueStroke
                )
                // Un cerchietto pieno all'inizio dell'arco colorato ne arrotonda solo l'inizio,
                // lasciando l'altra estremità piatta per allinearsi bene al segno del picco.
                drawCircle(
                    color = color,
                    radius = strokeWidth / 2,
                    center = pointOnArc(GAUGE_TRACK_START_ANGLE, topLeft, arcSize)
                )
            }
        }

        if (peakFraction != null && peakFraction > 0f) {
            drawPeakMarker(
                fraction = peakFraction,
                trackTopLeft = topLeft,
                trackSize = arcSize,
                strokeWidthPx = strokeWidth,
                color = peakColor
            )
        }
    }
}

/** Il punto sull'arco a un certo angolo. */
private fun pointOnArc(angleDeg: Float, topLeft: Offset, size: Size): Offset {
    val angleRad = Math.toRadians(angleDeg.toDouble())
    val center = Offset(topLeft.x + size.width / 2, topLeft.y + size.height / 2)
    val radius = size.width / 2
    return Offset(
        x = center.x + radius * cos(angleRad).toFloat(),
        y = center.y + radius * sin(angleRad).toFloat()
    )
}

/** Il piccolo trattino sull'arco che segna il punto del picco previsto. */
private fun DrawScope.drawPeakMarker(
    fraction: Float,
    trackTopLeft: Offset,
    trackSize: Size,
    strokeWidthPx: Float,
    color: Color
) {
    val angleRad = Math.toRadians((GAUGE_TRACK_START_ANGLE + GAUGE_TRACK_SWEEP_ANGLE * fraction).toDouble())
    val center = Offset(trackTopLeft.x + trackSize.width / 2, trackTopLeft.y + trackSize.height / 2)
    val radius = trackSize.width / 2
    val halfMarkerLength = strokeWidthPx * 0.9f
    val dx = cos(angleRad).toFloat()
    val dy = sin(angleRad).toFloat()
    drawLine(
        color = color,
        start = Offset(center.x + (radius - halfMarkerLength) * dx, center.y + (radius - halfMarkerLength) * dy),
        end = Offset(center.x + (radius + halfMarkerLength) * dx, center.y + (radius + halfMarkerLength) * dy),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/** La pilloletta con la freccetta e il testo "In salita" / "In discesa" / "Stabile". */
@Composable
fun TrendPill(trend: Trend, risingIsGood: Boolean) {
    val label = when (trend) {
        Trend.UP -> "In salita"
        Trend.DOWN -> "In discesa"
        Trend.FLAT -> "Stabile"
    }
    val color = when (trend) {
        Trend.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
        Trend.UP -> if (risingIsGood) ZoneSober else ZoneDanger
        Trend.DOWN -> if (risingIsGood) ZoneDanger else ZoneSober
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (trend != Trend.FLAT) {
                Icon(
                    imageVector = if (trend == Trend.UP) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(text = label, color = color, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// L'euforia non è un rischio come l'intossicazione: non diventa mai rossa, va dal verde
// all'arancione man mano che sale.
/** Il colore dell'indicatore Euforia in base al punteggio. */
fun euphoriaColorFor(score: Double): Color = when {
    score < 3.0 -> ZoneSober
    score < 7.0 -> ZoneWarmingUp
    else -> ZoneGolden
}
