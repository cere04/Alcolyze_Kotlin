package com.example.alcolyze.utils

import kotlin.math.ceil

/**
 * Calcola le "unità alcoliche" mostrate accanto a ogni drink nel listino: un numero facile da
 * confrontare tra drink diversi (un'unità è circa un bicchiere standard). Arrotondato sempre per
 * eccesso, per non far sembrare un drink più leggero di quanto sia. Calcolato una volta sola, al
 * salvataggio del drink.
 */
object AlcoholUnitCalculator {
    private const val ETHANOL_SPECIFIC_WEIGHT = 0.79
    private const val GRAMS_PER_ALCOHOL_UNIT = 12.0

    /** Grammi di alcol puro in un drink di [volumeMl] millilitri con gradazione [abv] per cento. */
    fun gramsOfAlcohol(volumeMl: Double, abv: Double): Double =
        volumeMl * (abv / 100.0) * ETHANOL_SPECIFIC_WEIGHT

    /** Unità alcoliche di un drink, arrotondate per eccesso al decimo. */
    fun alcoholUnits(volumeMl: Double, abv: Double): Double {
        val units = gramsOfAlcohol(volumeMl, abv) / GRAMS_PER_ALCOHOL_UNIT
        return ceil(units * 10.0) / 10.0
    }
}
