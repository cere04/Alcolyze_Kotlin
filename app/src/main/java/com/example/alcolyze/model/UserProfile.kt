package com.example.alcolyze.model

/** Sesso della persona: serve a stimare quanta acqua contiene il corpo, e quindi quanto si diluisce l'alcol. */
enum class Gender { MALE, FEMALE }

/** Categoria di patente: decide il limite legale per guidare (0.5 per gli standard, 0.0 per neopatentati e professionisti). */
enum class DriverCategory { STANDARD, NEOPATENTATO_OR_PROFESSIONAL }

// Per ogni stato dello stomaco:
// - absorptionFactor: quanta parte dell'alcol viene davvero assorbita (più basso con lo stomaco
//   pieno);
// - absorptionDelayMinutes: quanto tarda a INIZIARE l'assorbimento (non lo rallenta);
// - kaPerHour: velocità dell'assorbimento una volta iniziato, uguale per tutti gli stati.
// Rallentare la velocità con lo stomaco pieno farebbe scendere il tasso quasi a zero dopo un
// pasto abbondante anche con più drink, cosa non realistica.
enum class StomachState(val absorptionFactor: Double, val absorptionDelayMinutes: Double, val kaPerHour: Double) {
    EMPTY(1.00, 2.0, 4.8),
    LIGHT_MEAL(0.85, 15.0, 4.8),
    NORMAL_MEAL(0.75, 30.0, 4.8),
    FULL_MEAL(0.66, 60.0, 4.8)
}

/**
 * I dati personali dell'utente (schermata Profilo). Sono la base di tutti i calcoli: sesso, peso
 * e altezza dicono quanto si diluisce l'alcol nel corpo, la categoria di patente dà il limite
 * legale, e chi beve spesso smaltisce l'alcol un po' più in fretta. C'è un solo profilo.
 */
data class UserProfile(
    val gender: Gender,
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val driverCategory: DriverCategory,
    val isHabitualDrinker: Boolean = false
)
