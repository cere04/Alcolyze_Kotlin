package com.example.alcolyze.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.components.FavoriteDrinksPodium
import com.example.alcolyze.ui.components.StatCard
import com.example.alcolyze.ui.util.formatBac
import com.example.alcolyze.viewmodel.AlcolyzeViewModel
import com.example.alcolyze.viewmodel.StatsRange

/**
 * La sezione "Statistiche": totali di tutte le serate passate nel periodo scelto (podio dei
 * drink più bevuti e alcuni riquadri di numeri). Se nel periodo non c'è ancora nessun drink,
 * mostra solo il selettore del periodo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: AlcolyzeViewModel) {
    val range by viewModel.selectedStatsRange.collectAsState()
    val summary by viewModel.statsSummary.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Statistiche", style = MaterialTheme.typography.headlineSmall)

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            TextField(
                value = range.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Periodo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                StatsRange.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            viewModel.selectStatsRange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        val currentSummary = summary
        if (currentSummary == null || currentSummary.totalDrinks == 0) {
            Text("Nessuna statistica disponibile per il periodo selezionato.")
        } else {
            Text("Drink Preferiti", style = MaterialTheme.typography.titleMedium)
            FavoriteDrinksPodium(topDrinks = currentSummary.topDrinks)

            Text("Statistiche Generali", style = MaterialTheme.typography.titleMedium)
            val rows = listOf(
                "Serate Registrate" to currentSummary.seratesCount.toString(),
                "Drink Totali" to currentSummary.totalDrinks.toString(),
                "Drink Medi/Serata" to "%.1f".format(currentSummary.avgDrinksPerSerata),
                "Alcol Consumato (g)" to "%.1f".format(currentSummary.totalAlcoholGrams),
                "Calorie Assunte (kcal)" to "%.0f".format(currentSummary.totalCalories),
                "Picco BAC Massimo (g/L)" to formatBac(currentSummary.peakBac)
            )
            rows.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (label, value) ->
                        StatCard(label = label, value = value, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) {
                        Column(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }
    }
}
