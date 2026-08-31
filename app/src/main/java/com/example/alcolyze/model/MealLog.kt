package com.example.alcolyze.model

import java.time.Instant

/**
 * Un pasto registrato nella serata in corso. [stomachState] è quello che influenza il calcolo
 * per i drink bevuti dopo questo pasto; [time] non è quando il pasto viene salvato, ma il
 * momento scelto dall'utente (Ora, 15 min fa, ...).
 */
data class MealLog(
    val id: String,
    val stomachState: StomachState,
    val time: Instant
)
