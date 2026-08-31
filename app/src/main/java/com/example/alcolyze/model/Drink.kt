package com.example.alcolyze.model

import java.time.Instant

/**
 * Un drink davvero bevuto e registrato nella serata in corso (diverso da una voce di listino).
 * Porta con sé i dati di assorbimento dello stato dello stomaco al momento in cui è stato bevuto,
 * così il motore di calcolo può gestire correttamente drink diversi bevuti in condizioni diverse
 * nella stessa serata.
 */
data class Drink(
    val id: String,
    val name: String,
    val volumeMl: Double,
    val alcoholByVolume: Double,
    val ingestionTime: Instant,
    val stomachAbsorptionFactor: Double,
    val absorptionDelayHours: Double = StomachState.EMPTY.absorptionDelayMinutes / 60.0,
    val absorptionKaPerHour: Double = StomachState.EMPTY.kaPerHour,
    val isCustomLocalDrink: Boolean = false,
    // Categoria a testo libero (es. "Birra"); può arrivare dal listino online o da un drink
    // personalizzato.
    val category: String? = null,
    // Il numero di riga nel database di questa registrazione: serve al registro della serata per
    // cancellare esattamente quella riga.
    val dbId: Long? = null
) {
    /** Grammi di alcol puro contenuti nel drink: è il valore che entra nelle formule del calcolo. */
    val pureEthanolGrams: Double
        get() = volumeMl * (alcoholByVolume / 100.0) * 0.789
}
