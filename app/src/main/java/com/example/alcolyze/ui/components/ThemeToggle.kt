package com.example.alcolyze.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Voce "Tema" delle impostazioni: un selettore a due pulsanti "Chiaro"/"Scuro", sempre
// entrambi visibili, con quello attivo evidenziato.
@Composable
fun ThemeSettingsItem(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Scegli l'aspetto dell'app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ThemeToggle(isDarkTheme = isDarkTheme, onThemeChange = onThemeChange)
        }
    }
}

@Composable
private fun ThemeToggle(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ThemeToggleOption(
            label = "Chiaro",
            selected = !isDarkTheme,
            onClick = { onThemeChange(false) },
            modifier = Modifier.weight(1f)
        )
        ThemeToggleOption(
            label = "Scuro",
            selected = isDarkTheme,
            onClick = { onThemeChange(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThemeToggleOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "themeToggleBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "themeToggleContent"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}
