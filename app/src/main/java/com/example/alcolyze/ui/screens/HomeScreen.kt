package com.example.alcolyze.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.components.DrivingLimitSection
import com.example.alcolyze.ui.components.IntensityGaugeCard
import com.example.alcolyze.ui.components.SafetyZoneBar
import com.example.alcolyze.ui.components.SessionLogList
import com.example.alcolyze.ui.components.SessionStatsSection
import com.example.alcolyze.utils.BacCalculator
import com.example.alcolyze.viewmodel.AlcolyzeViewModel

/**
 * La schermata principale (sezione "Home"): barra Sicurezza, conto alla rovescia per guidare, i
 * due indicatori Intossicazione ed Euforia (che aprono le pagine di dettaglio al tocco), il
 * bottone "Torna sobrio", le statistiche della serata in corso e il registro di ogni drink e
 * pasto. I numeri arrivano già pronti; qui si mettono solo insieme i pezzi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AlcolyzeViewModel,
    onOpenProfile: () -> Unit,
    onOpenIntossicazioneDetail: () -> Unit,
    onOpenEuforiaDetail: () -> Unit
) {
    val currentBac by viewModel.currentBac.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val buzzScore by viewModel.buzzScore.collectAsState()
    val bacTrend by viewModel.bacTrend.collectAsState()
    val buzzTrend by viewModel.buzzTrend.collectAsState()
    val projectedPeakBac by viewModel.projectedPeakBac.collectAsState()
    val projectedPeakEuphoria by viewModel.projectedPeakEuphoria.collectAsState()
    val sessionDrinks by viewModel.sessionDrinks.collectAsState()
    val sessionMeals by viewModel.sessionMeals.collectAsState()
    val sessionDurationSeconds by viewModel.sessionDurationSeconds.collectAsState()
    val driveInSeconds by viewModel.driveInSeconds.collectAsState()
    val timeUntilOverLimitSeconds by viewModel.timeUntilOverLimitSeconds.collectAsState()
    var showResetConfirmation by remember { mutableStateOf(false) }

    // Alcol totale e calorie si ricavano direttamente dai drink della serata: si aggiornano da
    // soli ogni volta che si aggiunge o toglie un drink.
    val totalAlcoholMl = sessionDrinks.sumOf { it.volumeMl * (it.alcoholByVolume / 100.0) }
    val totalCalories = BacCalculator.estimatedCalories(sessionDrinks.sumOf { it.pureEthanolGrams })

    Scaffold(
        // Lo spazio per la barra di sistema in fondo è già lasciato dalla schermata che
        // contiene questa: senza questa riga verrebbe lasciato due volte, con un vuoto extra.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Alcolyze") },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profilo")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SICUREZZA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SafetyZoneBar(bac = currentBac, profile = userProfile)

                DrivingLimitSection(
                    driverCategory = userProfile.driverCategory,
                    currentBac = currentBac,
                    legalLimit = BacCalculator.legalLimit(userProfile),
                    hasActiveSession = sessionDrinks.isNotEmpty(),
                    secondsUntilOverLimit = timeUntilOverLimitSeconds,
                    secondsUntilLegalToDrive = driveInSeconds
                )

                IntensityGaugeCard(
                    bac = currentBac,
                    buzzScore = buzzScore,
                    trend = bacTrend,
                    buzzTrend = buzzTrend,
                    projectedPeakBac = projectedPeakBac,
                    projectedPeakEuphoria = projectedPeakEuphoria,
                    onClickIntossicazione = onOpenIntossicazioneDetail,
                    onClickEuforia = onOpenEuforiaDetail
                )
            }

            OutlinedButton(onClick = { showResetConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                Text("TORNA SOBRIO")
            }

            SessionStatsSection(
                durationSeconds = sessionDurationSeconds,
                totalAlcoholMl = totalAlcoholMl,
                totalCalories = totalCalories,
                drinkCount = sessionDrinks.size,
                peakBac = projectedPeakBac,
                peakEuphoria = projectedPeakEuphoria
            )

            SessionLogList(
                drinks = sessionDrinks,
                meals = sessionMeals,
                onRemoveDrink = { viewModel.removeDrink(it) },
                onRemoveMeal = { viewModel.removeMeal(it) }
            )
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Tornare sobrio?") },
            text = { Text("Questo azzera la sessione corrente (drink, pasti e statistiche): l'azione non è reversibile.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    viewModel.resetToSober()
                }) {
                    Text("Torna sobrio")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
