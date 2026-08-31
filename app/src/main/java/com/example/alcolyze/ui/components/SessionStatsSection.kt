package com.example.alcolyze.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.util.formatIntoxicationGauge
import com.example.alcolyze.ui.util.formatSessionDuration
import kotlin.math.roundToInt

/**
 * I riquadri con i numeri della serata in home. I primi 4 (Durata, Alcol totale, Calorie,
 * Drink) si aggiornano in tempo reale; gli ultimi 2 (Picco alcolemico, Picco euforia) mostrano
 * i valori previsti, che cambiano solo quando si registra un drink o un pasto.
 */
@Composable
fun SessionStatsSection(
    durationSeconds: Long,
    totalAlcoholMl: Double,
    totalCalories: Double,
    drinkCount: Int,
    peakBac: Double,
    peakEuphoria: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "STATISTICHE SESSIONE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SessionStatCard(
                icon = Icons.Default.Schedule,
                label = "Durata",
                value = formatSessionDuration(durationSeconds),
                modifier = Modifier.weight(1f)
            )
            SessionStatCard(
                icon = Icons.Default.LocalBar,
                label = "Alcol totale",
                value = "${totalAlcoholMl.roundToInt()} ml",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SessionStatCard(
                icon = Icons.Default.LocalFireDepartment,
                label = "Calorie",
                value = "${totalCalories.roundToInt()} kcal",
                modifier = Modifier.weight(1f)
            )
            SessionStatCard(
                icon = Icons.Default.LocalDrink,
                label = "Drink",
                value = drinkCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SessionStatCard(
                icon = Icons.AutoMirrored.Default.TrendingUp,
                label = "Picco alcolemico",
                value = "${formatIntoxicationGauge(peakBac)} g/L",
                modifier = Modifier.weight(1f)
            )
            SessionStatCard(
                icon = Icons.Default.Mood,
                label = "Picco euforia",
                value = "%.1f".format(peakEuphoria),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SessionStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
