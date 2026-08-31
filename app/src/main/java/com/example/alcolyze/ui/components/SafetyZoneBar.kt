package com.example.alcolyze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alcolyze.model.UserProfile
import com.example.alcolyze.ui.theme.SafetyZoneFelice
import com.example.alcolyze.ui.theme.SafetyZonePericolo
import com.example.alcolyze.ui.theme.SafetyZoneSobrio
import com.example.alcolyze.ui.theme.SafetyZoneUbriaco
import com.example.alcolyze.ui.theme.toColor
import com.example.alcolyze.utils.BacCalculator

// Fine della fascia "Pericolo": oltre questo valore il cursore resta fermo in fondo alla barra
// invece di uscirne.
private const val DISPLAY_MAX_BAC_G_PER_L = 6.0

private val ZONE_COLORS = listOf(SafetyZoneSobrio, SafetyZoneFelice, SafetyZoneUbriaco, SafetyZonePericolo)

/**
 * La barra "Sicurezza" in home: le 4 fasce (Sobrio, Felice, Ubriaco, In pericolo) tutte della
 * stessa larghezza per leggibilità, con un cursore che segue il tasso attuale e, sotto, il nome
 * e la descrizione della fascia. Dentro ogni fascia il cursore si posiziona in proporzione al
 * valore reale.
 */
@Composable
fun SafetyZoneBar(bac: Double, profile: UserProfile, modifier: Modifier = Modifier) {
    val zone = BacCalculator.safetyZoneFor(bac)
    val description = BacCalculator.safetyZoneDescription(zone, bac, profile)
    val zoneColor = zone.toColor()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SafetyZoneTrack(fraction = gaugeFraction(bac), cursorColor = zoneColor)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = zone.label, style = MaterialTheme.typography.titleMedium, color = zoneColor)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SafetyZoneTrack(fraction: Float, cursorColor: Color, modifier: Modifier = Modifier) {
    val trackHeight = 20.dp
    val cursorSize = 26.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(cursorSize)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
        ) {
            ZONE_COLORS.forEach { color ->
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(color))
            }
        }

        val cursorOffsetX = (maxWidth - cursorSize) * fraction.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = cursorOffsetX)
                .size(cursorSize)
                .background(Color.White, CircleShape)
                .border(3.dp, cursorColor, CircleShape)
        )
    }
}

// Posizione del cursore lungo la barra, da 0 a 1, attraverso le 4 fasce di uguale larghezza.
private fun gaugeFraction(bac: Double): Float {
    val clamped = bac.coerceAtLeast(0.0)
    val (zoneMin, zoneMax, sliceIndex) = when {
        clamped < BacCalculator.SAFETY_ZONE_FELICE_THRESHOLD_G_PER_L ->
            Triple(0.0, BacCalculator.SAFETY_ZONE_FELICE_THRESHOLD_G_PER_L, 0)
        clamped < BacCalculator.SAFETY_ZONE_UBRIACO_THRESHOLD_G_PER_L ->
            Triple(BacCalculator.SAFETY_ZONE_FELICE_THRESHOLD_G_PER_L, BacCalculator.SAFETY_ZONE_UBRIACO_THRESHOLD_G_PER_L, 1)
        clamped <= BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L ->
            Triple(BacCalculator.SAFETY_ZONE_UBRIACO_THRESHOLD_G_PER_L, BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L, 2)
        else ->
            Triple(BacCalculator.SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L, DISPLAY_MAX_BAC_G_PER_L, 3)
    }
    val withinZone = ((clamped - zoneMin) / (zoneMax - zoneMin)).coerceIn(0.0, 1.0)
    return ((sliceIndex + withinZone) / ZONE_COLORS.size).toFloat()
}
