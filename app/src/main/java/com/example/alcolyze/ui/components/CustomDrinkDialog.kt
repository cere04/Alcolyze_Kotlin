package com.example.alcolyze.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alcolyze.data.DrinkEntity
import com.example.alcolyze.utils.BacCalculator

/**
 * Il modulo per creare un drink personalizzato (nome, volume, gradazione, categoria), con
 * un'anteprima di grammi di alcol e calorie. Il selettore di categoria mostra quelle davvero
 * presenti nel listino; se è vuoto propone "Cocktail".
 *
 * Passando [initialDrink] lo stesso modulo serve a modificare un drink personalizzato esistente:
 * i campi sono già compilati e titolo e bottone cambiano di conseguenza.
 */
@Composable
fun CustomDrinkDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, volumeMl: Double, abv: Double, category: String) -> Unit,
    initialDrink: DrinkEntity? = null
) {
    val isEditing = initialDrink != null
    val effectiveCategories = categories.ifEmpty { listOf("Cocktail") }
    var name by remember { mutableStateOf(initialDrink?.nome ?: "") }
    var volumeText by remember { mutableStateOf(initialDrink?.volumeMl?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var abvText by remember { mutableStateOf(initialDrink?.gradazioneAbv?.toString() ?: "") }
    var category by remember {
        mutableStateOf(
            initialDrink?.categoria?.takeIf { it in effectiveCategories } ?: effectiveCategories.first()
        )
    }

    val volume = volumeText.toDoubleOrNull()
    val abv = abvText.toDoubleOrNull()
    val isValid = name.isNotBlank() && volume != null && volume > 0.0 && abv != null && abv > 0.0 && abv <= 100.0

    val ethanolGrams = if (volume != null && abv != null) volume * (abv / 100.0) * 0.789 else 0.0
    val calories = BacCalculator.estimatedCalories(ethanolGrams)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Modifica Drink" else "Crea Drink Personalizzato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Drink") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = volumeText,
                    onValueChange = { volumeText = it },
                    label = { Text("Volume (ml)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = abvText,
                    onValueChange = { abvText = it },
                    label = { Text("Gradazione Alcolica (ABV %)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Categoria")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    effectiveCategories.forEach { entry ->
                        FilterChip(
                            selected = entry == category,
                            onClick = { category = entry },
                            label = { Text(entry) }
                        )
                    }
                }
                Text("~ ${"%.1f".format(ethanolGrams)} g etanolo puro - ${"%.0f".format(calories)} kcal")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, volume ?: 0.0, abv ?: 0.0, category) },
                enabled = isValid
            ) { Text(if (isEditing) "Salva Modifiche" else "Salva Drink") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
