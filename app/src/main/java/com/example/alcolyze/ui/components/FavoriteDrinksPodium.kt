package com.example.alcolyze.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Altezze del podio: il 1° posto più alto, il 3° più basso.
private val PODIUM_HEIGHTS = listOf(160.dp, 130.dp, 100.dp)

/** Il podio dei drink più bevuti nella pagina Statistiche: fino a 3, dal più frequente. */
@Composable
fun FavoriteDrinksPodium(topDrinks: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    if (topDrinks.isEmpty()) return

    if (topDrinks.size == 1) {
        val (name, count) = topDrinks[0]
        PodiumCard(name = name, count = count, height = PODIUM_HEIGHTS[0], modifier = modifier.fillMaxWidth())
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            topDrinks.forEachIndexed { index, (name, count) ->
                PodiumCard(
                    name = name,
                    count = count,
                    height = PODIUM_HEIGHTS[index],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PodiumCard(name: String, count: Int, height: Dp, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(height)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleLarge)
            Text(text = "$count volte", style = MaterialTheme.typography.labelMedium)
        }
    }
}
