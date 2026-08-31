package com.example.alcolyze.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.components.GaugeColumn
import com.example.alcolyze.ui.components.TrendChart
import com.example.alcolyze.ui.components.TrendPill
import com.example.alcolyze.ui.components.euphoriaColorFor
import com.example.alcolyze.ui.theme.toColor
import com.example.alcolyze.ui.util.dynamicYAxis
import com.example.alcolyze.ui.util.formatAxisTick
import com.example.alcolyze.ui.util.formatBac
import com.example.alcolyze.ui.util.formatIntoxicationGauge
import com.example.alcolyze.ui.util.formatTime
import com.example.alcolyze.utils.BacCalculator
import com.example.alcolyze.utils.Trend
import com.example.alcolyze.viewmodel.AlcolyzeViewModel
import java.time.Duration
import java.time.Instant
import kotlin.math.max

private val DETAIL_DIAL_SIZE = 180.dp
private val TREND_CHART_HORIZON_HOURS = BacCalculator.TREND_CHART_HORIZON_HOURS.toFloat()

private fun hoursBetween(from: Instant, to: Instant): Float =
    (Duration.between(from, to).toMillis() / 3_600_000.0).toFloat()

/**
 * La pagina di dettaglio "Intossicazione", aperta toccando l'indicatore in home. Rimostra lo
 * stesso indicatore in grande e sotto il grafico dell'andamento del tasso alcolemico nelle
 * prossime ore. Usa lo stesso calcolo del valore in home, così i due coincidono sempre.
 */
@Composable
fun IntossicazioneDetailScreen(viewModel: AlcolyzeViewModel, onBack: () -> Unit) {
    val bac by viewModel.currentBac.collectAsState()
    val trend by viewModel.bacTrend.collectAsState()
    val projectedPeakBac by viewModel.projectedPeakBac.collectAsState()
    val sessionDrinks by viewModel.sessionDrinks.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val peakBacTime by viewModel.projectedPeakBacTime.collectAsState()

    val anchor = remember(sessionDrinks, userProfile) { Instant.now() }
    val trajectory = remember(sessionDrinks, userProfile, anchor, peakBacTime) {
        BacCalculator.projectTrajectory(
            drinks = sessionDrinks,
            profile = userProfile,
            from = anchor
        )
    }
    // Aggiornata ogni secondo: la riga verticale "adesso" avanza da sola lungo la curva già
    // disegnata.
    val nowX = remember(bac) { hoursBetween(anchor, Instant.now()).coerceIn(0f, TREND_CHART_HORIZON_HOURS) }
    val dialColor = BacCalculator.safetyZoneFor(bac).toColor()

    val bacAbsoluteMax = BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L.toFloat()
    val yAxis = remember(trajectory, bac, projectedPeakBac) {
        val dataMax = max(trajectory.maxOfOrNull { it.bac.toFloat() } ?: 0f, max(bac.toFloat(), projectedPeakBac.toFloat()))
        dynamicYAxis(dataMax, bacAbsoluteMax, tickCount = 4)
    }
    // Il pallino di picco usa il picco già calcolato (lo stesso mostrato sull'indicatore), non un
    // massimo cercato dentro la curva visibile: se il picco vero è già passato, la curva mostrata
    // parte da "adesso" e non lo conterrebbe, facendo scambiare il valore attuale per il picco.
    val hasPeak = projectedPeakBac > 0.0
    val peakX = peakBacTime?.let { hoursBetween(anchor, it) }.takeIf { hasPeak }
    val peakY = projectedPeakBac.toFloat().takeIf { hasPeak }

    IntensityDetailScreen(
        title = "Intossicazione",
        valueText = formatIntoxicationGauge(bac),
        dialColor = dialColor,
        fraction = (bac / BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L).coerceIn(0.0, 1.0).toFloat(),
        peakFraction = (projectedPeakBac / BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L).coerceIn(0.0, 1.0).toFloat(),
        trend = trend,
        risingIsGood = false,
        chartPoints = trajectory.map { hoursBetween(anchor, it.time) to it.bac.toFloat() },
        yMax = yAxis.max,
        yTicks = yAxis.ticks,
        yTickLabel = { v -> if (v >= bacAbsoluteMax) "4+" else formatAxisTick(v, yAxis.step) },
        nowX = nowX,
        nowY = bac.toFloat(),
        startTime = anchor,
        peakX = peakX,
        peakY = peakY,
        peakValueLabel = if (hasPeak) formatBac(projectedPeakBac) else "",
        peakTimeLabel = peakBacTime?.let { formatTime(it) } ?: "",
        onBack = onBack
    )
}

/**
 * La pagina di dettaglio "Euforia": stessa struttura di quella dell'Intossicazione, ma il
 * grafico mostra il punteggio euforia invece del tasso alcolemico.
 */
@Composable
fun EuforiaDetailScreen(viewModel: AlcolyzeViewModel, onBack: () -> Unit) {
    val buzzScore by viewModel.buzzScore.collectAsState()
    val trend by viewModel.buzzTrend.collectAsState()
    val projectedPeakEuphoria by viewModel.projectedPeakEuphoria.collectAsState()
    val sessionDrinks by viewModel.sessionDrinks.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val peakBacTime by viewModel.projectedPeakBacTime.collectAsState()
    val chartPeakEuphoria by viewModel.chartPeakEuphoria.collectAsState()
    val chartPeakEuphoriaTime by viewModel.chartPeakEuphoriaTime.collectAsState()

    val anchor = remember(sessionDrinks, userProfile) { Instant.now() }
    val trajectory = remember(sessionDrinks, userProfile, anchor, peakBacTime) {
        BacCalculator.projectTrajectory(
            drinks = sessionDrinks,
            profile = userProfile,
            from = anchor
        )
    }
    val nowX = remember(buzzScore) { hoursBetween(anchor, Instant.now()).coerceIn(0f, TREND_CHART_HORIZON_HOURS) }
    val dialColor = euphoriaColorFor(buzzScore)

    val euphoriaAbsoluteMax = BacCalculator.EUPHORIA_SCORE_MAX.toFloat()
    val yAxis = remember(trajectory, buzzScore, chartPeakEuphoria) {
        val dataMax = max(trajectory.maxOfOrNull { it.euphoria.toFloat() } ?: 0f, max(buzzScore.toFloat(), chartPeakEuphoria.toFloat()))
        dynamicYAxis(dataMax, euphoriaAbsoluteMax, tickCount = 5)
    }
    // Come per l'Intossicazione: usa il picco di euforia davvero raggiunto dalla curva, non un
    // massimo cercato nella parte visibile, altrimenti se il picco è già passato si mostrerebbe
    // il valore attuale al suo posto.
    val hasPeak = chartPeakEuphoria > 0.0
    val peakX = chartPeakEuphoriaTime?.let { hoursBetween(anchor, it) }.takeIf { hasPeak }
    val peakY = chartPeakEuphoria.toFloat().takeIf { hasPeak }

    IntensityDetailScreen(
        title = "Euforia",
        valueText = "%.1f".format(buzzScore),
        dialColor = dialColor,
        fraction = (buzzScore / BacCalculator.EUPHORIA_SCORE_MAX).coerceIn(0.0, 1.0).toFloat(),
        peakFraction = (projectedPeakEuphoria / BacCalculator.EUPHORIA_SCORE_MAX).coerceIn(0.0, 1.0).toFloat(),
        trend = trend,
        risingIsGood = true,
        chartPoints = trajectory.map { hoursBetween(anchor, it.time) to it.euphoria.toFloat() },
        yMax = yAxis.max,
        yTicks = yAxis.ticks,
        yTickLabel = { v -> formatAxisTick(v, yAxis.step) },
        nowX = nowX,
        nowY = buzzScore.toFloat(),
        startTime = anchor,
        peakX = peakX,
        peakY = peakY,
        peakValueLabel = if (hasPeak) "%.1f".format(chartPeakEuphoria) else "",
        peakTimeLabel = chartPeakEuphoriaTime?.let { formatTime(it) } ?: "",
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntensityDetailScreen(
    title: String,
    valueText: String,
    dialColor: Color,
    fraction: Float,
    peakFraction: Float?,
    trend: Trend,
    risingIsGood: Boolean,
    chartPoints: List<Pair<Float, Float>>,
    yMax: Float,
    yTicks: List<Float>,
    yTickLabel: (Float) -> String,
    nowX: Float,
    nowY: Float,
    startTime: Instant,
    peakX: Float?,
    peakY: Float?,
    peakValueLabel: String,
    peakTimeLabel: String,
    onBack: () -> Unit
) {
    Scaffold(
        // Lo spazio per la barra di sistema in fondo è già lasciato dalla schermata che contiene
        // questa: qui non va lasciato di nuovo.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GaugeColumn(
                        label = title,
                        valueText = valueText,
                        dialColor = dialColor,
                        fraction = fraction,
                        signed = false,
                        peakFraction = peakFraction,
                        dialSize = DETAIL_DIAL_SIZE,
                        trendPill = { TrendPill(trend = trend, risingIsGood = risingIsGood) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ANDAMENTO PROSSIME ${TREND_CHART_HORIZON_HOURS.toInt()} ORE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    TrendChart(
                        points = chartPoints,
                        horizonHours = TREND_CHART_HORIZON_HOURS,
                        yMax = yMax,
                        yTicks = yTicks,
                        yTickLabel = yTickLabel,
                        lineColor = dialColor,
                        nowX = nowX,
                        nowY = nowY,
                        startTime = startTime,
                        peakX = peakX,
                        peakY = peakY,
                        peakValueLabel = peakValueLabel,
                        peakTimeLabel = peakTimeLabel,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
