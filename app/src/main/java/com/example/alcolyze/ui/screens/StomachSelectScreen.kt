package com.example.alcolyze.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alcolyze.model.StomachState
import com.example.alcolyze.ui.util.displayLabel
import com.example.alcolyze.viewmodel.AlcolyzeViewModel

private fun StomachState.description(): String = when (this) {
    StomachState.EMPTY -> "Niente nello stomaco - l'alcol viene assorbito più in fretta"
    StomachState.LIGHT_MEAL -> "Uno spuntino o un pasto leggero"
    StomachState.NORMAL_MEAL -> "Un pasto normale e bilanciato"
    StomachState.FULL_MEAL -> "Un pasto abbondante - l'alcol viene assorbito più lentamente"
}

/**
 * La seconda schermata iniziale, prima della home: chiede quanto è pieno lo stomaco all'inizio.
 * Questa scelta vale per i drink registrati finché non si aggiunge un pasto vero. Qui, a
 * differenza di "Registra un pasto", c'è anche l'opzione "stomaco vuoto".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StomachSelectScreen(viewModel: AlcolyzeViewModel, onContinue: () -> Unit) {
    val currentStomachState by viewModel.stomachState.collectAsState()
    var selected by remember { mutableStateOf(currentStomachState) }

    Scaffold(
        // Lo spazio per le barre di sistema (sopra e sotto) è già lasciato dalla schermata che
        // contiene questa: senza questa riga verrebbe conteggiato due volte, con una fascia
        // scura sotto che copre il bottone "CONTINUA".
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(title = { Text("Cosa hai nello stomaco?") })
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
            Text(
                text = "Quanto hai mangiato? Influisce sulla velocità con cui l'alcol entra in circolo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StomachState.entries.forEach { state ->
                    StomachOptionCard(
                        state = state,
                        selected = state == selected,
                        onClick = { selected = state }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.updateStomachState(selected)
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("CONTINUA")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StomachOptionCard(state: StomachState, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.displayLabel(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
