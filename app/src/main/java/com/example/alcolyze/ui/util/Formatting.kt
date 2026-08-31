package com.example.alcolyze.ui.util

import com.example.alcolyze.model.Gender
import com.example.alcolyze.model.StomachState
import com.example.alcolyze.utils.BacCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// I valori del tasso alcolemico usano sempre il punto come separatore decimale, non la virgola,
// a prescindere dalla lingua del telefono, per restare coerenti nel resto dell'app.
private val BAC_LOCALE = Locale.US

/** Scrive il tasso alcolemico come "0.25" (due decimali). */
fun formatBac(bac: Double): String = String.format(BAC_LOCALE, "%.2f", bac)

/** Come [formatBac], ma sopra la soglia di pericolo mostra "4.0+" invece di un numero senza limite. */
fun formatIntoxicationGauge(bac: Double): String =
    if (bac > BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L) "4.0+" else formatBac(bac)

/** Etichette dell'asse verticale del grafico: i decimali si adattano alla distanza tra le tacche. */
fun formatAxisTick(value: Float, step: Float): String = when {
    step >= 1f -> String.format(BAC_LOCALE, "%.0f", value)
    step >= 0.1f -> String.format(BAC_LOCALE, "%.1f", value)
    else -> String.format(BAC_LOCALE, "%.2f", value)
}

// Riquadro "Durata" della serata in home: "4m" sotto l'ora, "1h 23m" oltre. I secondi non si
// mostrano mai: è un tempo che sale, il minuto basta.
fun formatSessionDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

// Conto alla rovescia "quanto manca per guidare": qui il tempo scende verso zero, quindi
// sotto il minuto si mostrano i secondi, altrimenti resterebbe fermo su "0m" per un minuto intero.
fun formatCountdown(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0L)
    val hours = clamped / 3600
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

/** Scrive un orario come "21:30", nel fuso orario del telefono. */
fun formatTime(instant: Instant): String =
    TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(instant)

/** Nome per esteso di quanto è pieno lo stomaco. */
fun StomachState.displayLabel(): String = when (this) {
    StomachState.EMPTY -> "A stomaco vuoto"
    StomachState.LIGHT_MEAL -> "Pasto leggero"
    StomachState.NORMAL_MEAL -> "Pasto normale"
    StomachState.FULL_MEAL -> "Pasto abbondante"
}

/** Nome per esteso del sesso. */
fun Gender.displayLabel(): String = when (this) {
    Gender.MALE -> "Uomo"
    Gender.FEMALE -> "Donna"
}

/** Nome del pasto mostrato nel registro della serata. "Stomaco vuoto" non compare qui: è solo una scelta iniziale, non un pasto da registrare. */
fun StomachState.mealSessionLabel(): String = when (this) {
    StomachState.LIGHT_MEAL -> "Pasto leggero"
    StomachState.NORMAL_MEAL -> "Pasto medio"
    StomachState.FULL_MEAL -> "Pasto completo"
    StomachState.EMPTY -> "Pasto"
}
