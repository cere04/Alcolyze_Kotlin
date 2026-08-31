package com.example.alcolyze.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alcolyze.utils.BacCalculator

/**
 * La pagina "Scopri il funzionamento" (aperta da Impostazioni): spiega in parole semplici come
 * l'app calcola il tasso alcolemico e l'euforia, e ricorda i limiti legali. È solo testo; i
 * pochi numeri citati sono presi direttamente dal motore di calcolo, così restano sempre uguali
 * a quelli davvero usati.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    Scaffold(
        // Lo spazio per la barra di sistema in fondo è già lasciato dalla schermata che contiene
        // questa: qui non va lasciato di nuovo.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Scopri il funzionamento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
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
            Text(
                "Il nostro motore scientifico",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Alcolyze non utilizza stime matematiche approssimative. Sotto il cofano gira un vero modello differenziale clinico che simula la biologia del tuo corpo minuto per minuto.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("1. Distribuzione (Acqua Corporea)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "L'alcol si distribuisce nell'acqua del nostro corpo. Utilizziamo la formula clinica di Watson per calcolare la tua acqua totale (TBW) basandoci su età, peso, altezza e sesso. Questo valore viene poi convertito nel volume di sangue disponibile per diluire l'alcol tramite il Fattore di Seidl.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("2. Assorbimento Gastrico e Cibo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "L'alcol non entra in circolo tutto in una volta. A stomaco vuoto, l'assorbimento è rapidissimo e l'alcol entra nel sangue con un'efficienza del 100%. " +
                        "Se invece hai mangiato, il cibo chiude temporaneamente la valvola dello stomaco: questo ritarda notevolmente l'assorbimento e permette agli enzimi digestivi di 'distruggere' fino al 34% dell'alcol prima ancora che arrivi nel circolo sanguigno.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("3. Eliminazione e Fegato", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Il fegato smaltisce l'alcol a una velocità costante di circa ${BacCalculator.METABOLIC_ELIMINATION_RATE} g/L all'ora " +
                        "(${BacCalculator.HABITUAL_ELIMINATION_RATE} g/L se hai indicato di essere un bevitore abituale). Tuttavia, quando il tasso alcolemico si avvicina allo zero, " +
                        "l'eliminazione rallenta gradualmente assecondando la reale biologia enzimatica (Cinetica di Michaelis-Menten), modellando esattamente i tempi necessari per tornare completamente sobri.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("4. Calcolo dell'Euforia (Effetto Buzz)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Non guardiamo solo al numero, ma anche a come ci arrivi. Il picco massimo di rilascio di dopamina si prova intorno a ${BacCalculator.EUPHORIA_PEAK_BAC_G_PER_L} g/L (Scala B-BAES). " +
                        "Tuttavia, l'intensità di questa sensazione dipende dalla velocità di assorbimento: bere uno shot a stomaco vuoto genererà un picco euforico altissimo. Bere lentamente a stomaco pieno smorzerà questa sensazione. " +
                        "Inoltre, a parità di tasso alcolemico, ti sentirai molto meno euforico (e più affaticato) quando la curva scende rispetto a quando stava salendo, simulando la reale tolleranza acuta del cervello (Effetto Mellanby).",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Limiti legali in Italia", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "• Standard: ${BacCalculator.LEGAL_LIMIT_STANDARD} g/L\n" +
                        "• Neopatentati e Professionisti: ${BacCalculator.LEGAL_LIMIT_NEOPATENTATO} g/L",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                "Nota bene: Le stime fornite sono calcolate tramite modelli clinici rigorosi, ma restano puramente indicative a causa dell'imprevedibile variabilità umana e non sostituiscono un etilometro reale. Non metterti mai alla guida dopo aver bevuto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}