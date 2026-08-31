package com.example.alcolyze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alcolyze.model.StomachState
import com.example.alcolyze.ui.util.mealSessionLabel
import com.example.alcolyze.viewmodel.AlcolyzeViewModel
import com.example.alcolyze.viewmodel.TimeOffset

// "Stomaco vuoto" è solo una scelta iniziale, non un pasto: qui non compare.
private val MEAL_OPTIONS = listOf(StomachState.LIGHT_MEAL, StomachState.NORMAL_MEAL, StomachState.FULL_MEAL)

private fun StomachState.description(): String = when (this) {
    StomachState.LIGHT_MEAL -> "Uno spuntino o un pasto leggero"
    StomachState.NORMAL_MEAL -> "Un pasto normale e bilanciato"
    StomachState.FULL_MEAL -> "Un pasto abbondante - rallenta l'assorbimento"
    StomachState.EMPTY -> ""
}

/**
 * Il foglio "Registra un pasto": scegli quando hai mangiato e quanto, e toccando un'opzione la
 * registri subito. Qui non c'è l'opzione "stomaco vuoto": non ha senso registrare un pasto per
 * dire che non si è mangiato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMealSheet(viewModel: AlcolyzeViewModel, onDismiss: () -> Unit) {
    val selectedOffset by viewModel.selectedTimeOffset.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Registra un pasto", style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeOffset.entries.forEach { offset ->
                    FilterChip(
                        selected = offset == selectedOffset,
                        onClick = { viewModel.selectTimeOffset(offset) },
                        label = { Text(offset.label) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MEAL_OPTIONS.forEach { state ->
                    MealOptionRow(state = state) {
                        viewModel.logMeal(state)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun MealOptionRow(state: StomachState, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(state.mealSessionLabel(), style = MaterialTheme.typography.bodyLarge)
            Text(
                state.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
