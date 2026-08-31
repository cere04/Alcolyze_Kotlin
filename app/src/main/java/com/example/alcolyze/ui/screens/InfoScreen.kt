package com.example.alcolyze.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.components.SettingsMenuItem
import com.example.alcolyze.ui.components.ThemeSettingsItem

/**
 * La sezione "Impostazioni": collegamenti a Profilo e "Scopri il funzionamento", l'interruttore
 * tema chiaro/scuro e la versione dell'app.
 */
@Composable
fun InfoScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHowItWorks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineSmall)

        SettingsMenuItem(
            title = "Dati Personali",
            subtitle = "Visualizza e modifica il tuo profilo",
            onClick = onOpenProfile
        )

        SettingsMenuItem(
            title = "Scopri il funzionamento",
            subtitle = "Come calcoliamo il tuo tasso alcolemico",
            onClick = onOpenHowItWorks
        )

        ThemeSettingsItem(isDarkTheme = isDarkTheme, onThemeChange = onThemeChange)

        Text(
            text = "Alcolyze V. 1.0",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
