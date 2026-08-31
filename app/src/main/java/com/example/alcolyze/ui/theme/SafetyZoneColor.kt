package com.example.alcolyze.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.alcolyze.utils.SafetyZone

/** Il colore di ciascuna delle 4 fasce della barra Sicurezza. */
fun SafetyZone.toColor(): Color = when (this) {
    SafetyZone.SOBRIO -> SafetyZoneSobrio
    SafetyZone.FELICE -> SafetyZoneFelice
    SafetyZone.UBRIACO -> SafetyZoneUbriaco
    SafetyZone.PERICOLO -> SafetyZonePericolo
}
