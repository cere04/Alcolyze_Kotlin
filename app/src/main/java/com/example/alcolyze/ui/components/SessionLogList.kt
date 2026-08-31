package com.example.alcolyze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alcolyze.model.Drink
import com.example.alcolyze.model.MealLog
import com.example.alcolyze.ui.theme.ZoneDanger
import com.example.alcolyze.ui.util.formatTime
import com.example.alcolyze.ui.util.mealSessionLabel
import java.time.Instant

private sealed interface SessionLogRow {
    val time: Instant

    data class DrinkRow(val drink: Drink) : SessionLogRow {
        override val time: Instant get() = drink.ingestionTime
    }

    data class MealRow(val meal: MealLog) : SessionLogRow {
        override val time: Instant get() = meal.time
    }
}

/**
 * Il registro della serata in home: ogni drink e pasto registrato, dal più recente, con un
 * cestino per toglierli (viene chiesta conferma, perché cancellare cambia i numeri mostrati).
 * Non compare nulla se la serata è vuota.
 */
@Composable
fun SessionLogList(
    drinks: List<Drink>,
    meals: List<MealLog>,
    onRemoveDrink: (Drink) -> Unit,
    onRemoveMeal: (MealLog) -> Unit,
    modifier: Modifier = Modifier
) {
    if (drinks.isEmpty() && meals.isEmpty()) return

    val rows = (drinks.map { SessionLogRow.DrinkRow(it) } + meals.map { SessionLogRow.MealRow(it) })
        .sortedByDescending { it.time }

    var drinkPendingRemoval by remember { mutableStateOf<Drink?>(null) }
    var mealPendingRemoval by remember { mutableStateOf<MealLog?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "REGISTRO SESSIONE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                when (row) {
                    is SessionLogRow.DrinkRow -> DrinkLogRow(drink = row.drink, onRemove = { drinkPendingRemoval = row.drink })
                    is SessionLogRow.MealRow -> MealLogRow(meal = row.meal, onRemove = { mealPendingRemoval = row.meal })
                }
            }
        }
    }

    drinkPendingRemoval?.let { drink ->
        RemoveConfirmationDialog(
            title = "Rimuovere \"${drink.name}\"?",
            content = "Questa voce verrà eliminata dal registro della sessione corrente e il " +
                "tasso alcolemico verrà ricalcolato di conseguenza.",
            onDismiss = { drinkPendingRemoval = null },
            onConfirm = {
                onRemoveDrink(drink)
                drinkPendingRemoval = null
            }
        )
    }

    mealPendingRemoval?.let { meal ->
        RemoveConfirmationDialog(
            title = "Rimuovere questo pasto?",
            content = "Questa voce verrà eliminata dal registro della sessione corrente e i tempi " +
                "di assorbimento dei drink successivi verranno ricalcolati di conseguenza.",
            onDismiss = { mealPendingRemoval = null },
            onConfirm = {
                onRemoveMeal(meal)
                mealPendingRemoval = null
            }
        )
    }
}

@Composable
private fun RemoveConfirmationDialog(title: String, content: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Rimuovi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

@Composable
private fun SessionLogCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    time: Instant,
    trailing: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = formatTime(time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            trailing()
        }
    }
}

@Composable
private fun DrinkLogRow(drink: Drink, onRemove: () -> Unit) {
    SessionLogCard(
        icon = iconForCategory(drink.category),
        title = drink.name,
        subtitle = "${drink.volumeMl.toInt()} ml • ${drink.alcoholByVolume}% ABV",
        time = drink.ingestionTime,
        trailing = {
            IconButton(onClick = onRemove) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Elimina drink", tint = ZoneDanger)
            }
        }
    )
}

@Composable
private fun MealLogRow(meal: MealLog, onRemove: () -> Unit) {
    SessionLogCard(
        icon = Icons.Default.Restaurant,
        title = meal.stomachState.mealSessionLabel(),
        subtitle = "Pasto",
        time = meal.time,
        trailing = {
            IconButton(onClick = onRemove) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Elimina pasto", tint = ZoneDanger)
            }
        }
    )
}

// Icona della riga in base alla categoria del drink, riconosciuta cercando una parola nel testo
// (le categorie sono testo libero, non un elenco fisso): birra, vino e analcolico hanno la loro
// icona, tutto il resto usa un bicchiere generico.
private fun iconForCategory(category: String?): ImageVector {
    val normalized = category?.lowercase().orEmpty()
    return when {
        normalized.contains("birra") || normalized.contains("beer") -> Icons.Default.SportsBar
        normalized.contains("vino") || normalized.contains("wine") -> Icons.Default.WineBar
        normalized.contains("analcolic") || normalized.contains("soft") -> Icons.Default.LocalDrink
        else -> Icons.Default.LocalBar
    }
}
