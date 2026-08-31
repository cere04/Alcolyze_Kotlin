package com.example.alcolyze.ui.util

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/** La scala verticale di un grafico: valore massimo, tacche e distanza tra le tacche. */
data class YAxisScale(val max: Float, val ticks: List<Float>, val step: Float)

private val NICE_FRACTIONS = floatArrayOf(1f, 2f, 2.5f, 5f, 10f)

// Arrotonda un intervallo tra tacche a un numero "comodo" (1, 2, 2.5, 5, 10 e loro multipli).
private fun niceStep(rawStep: Float): Float {
    val safeStep = max(rawStep, 0.0001f)
    val magnitude = 10f.pow(floor(log10(safeStep.toDouble())).toFloat())
    val normalized = safeStep / magnitude
    val nice = NICE_FRACTIONS.first { normalized <= it }
    return nice * magnitude
}

/**
 * Calcola la scala verticale del grafico dell'andamento. Con una scala fissa la curva
 * resterebbe schiacciata in fondo nella maggior parte dei casi, perché di solito i valori
 * stanno ben sotto il massimo. Qui invece il massimo si adatta ai dati (con un po' di margine
 * sopra il picco e un minimo per non avere un grafico piatto a inizio serata), ma non supera
 * mai [absoluteMax].
 */
fun dynamicYAxis(dataMax: Float, absoluteMax: Float, tickCount: Int = 4): YAxisScale {
    val minVisibleMax = absoluteMax * 0.1f
    val paddedMax = (dataMax.coerceAtLeast(0f) * 1.15f)
        .coerceAtLeast(minVisibleMax)
        .coerceAtMost(absoluteMax)
    val step = niceStep(paddedMax / tickCount)
    val axisMax = (step * tickCount).coerceAtMost(absoluteMax)
    val ticks = (0..tickCount).map { (it * step).coerceAtMost(axisMax) }.distinct()
    return YAxisScale(axisMax, ticks, step)
}
