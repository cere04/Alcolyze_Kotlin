package com.example.alcolyze.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alcolyze.model.DriverCategory
import com.example.alcolyze.ui.util.formatCountdown

/**
 * Il riquadro "quanto manca per guidare", sotto la barra Sicurezza. Mostra sempre un solo
 * messaggio alla volta:
 * - per neopatentati e professionisti (limite zero): quanto manca per tornare a poter guidare;
 * - per gli altri, se si è sotto il limite: quanto manca prima di superarlo;
 * - per gli altri, se si è già oltre il limite: quanto manca per tornare sotto;
 * - se la serata è troppo leggera per superare mai il limite: il riquadro non compare.
 */
@Composable
fun DrivingLimitSection(
    driverCategory: DriverCategory,
    currentBac: Double,
    legalLimit: Double,
    hasActiveSession: Boolean,
    secondsUntilOverLimit: Long?,
    secondsUntilLegalToDrive: Long?,
    modifier: Modifier = Modifier
) {
    if (!hasActiveSession) return

    val label: String
    val valueText: String
    val isPositive: Boolean
    when {
        driverCategory == DriverCategory.NEOPATENTATO_OR_PROFESSIONAL -> {
            label = "Tempo alla guida sicura"
            valueText = secondsUntilLegalToDrive?.let { formatCountdown(it) } ?: "Puoi guidare"
            isPositive = secondsUntilLegalToDrive == null
        }
        currentBac >= legalLimit -> {
            label = "Per tornare a guidare"
            valueText = secondsUntilLegalToDrive?.let { formatCountdown(it) } ?: "Puoi guidare"
            isPositive = secondsUntilLegalToDrive == null
        }
        secondsUntilOverLimit != null -> {
            label = "Prima di superare il limite legale"
            valueText = formatCountdown(secondsUntilOverLimit)
            isPositive = false
        }
        else -> return // serata troppo leggera per superare il limite: niente da mostrare.
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            DrivingTimerRow(label = label, valueText = valueText, isPositive = isPositive)
        }
    }
}

@Composable
private fun DrivingTimerRow(label: String, valueText: String, isPositive: Boolean) {
    // Lo spazio flessibile è dato alla scritta di sinistra, non al valore: così il valore resta
    // sempre leggibile per intero e la scritta va a capo prima di toccarlo.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
