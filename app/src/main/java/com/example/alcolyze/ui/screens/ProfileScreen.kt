package com.example.alcolyze.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.example.alcolyze.model.DriverCategory
import com.example.alcolyze.model.Gender
import com.example.alcolyze.model.UserProfile
import com.example.alcolyze.ui.util.displayLabel
import com.example.alcolyze.viewmodel.AlcolyzeViewModel

/**
 * La schermata Profilo: sesso, età, peso, altezza, categoria di patente e "bevitore abituale".
 * Questi dati cambiano il calcolo, quindi appena si salva il tasso della serata viene
 * ricalcolato. Le modifiche restano solo qui finché non si preme "Salva Profilo".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AlcolyzeViewModel, onBack: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()

    var gender by remember { mutableStateOf(profile.gender) }
    var age by remember { mutableStateOf(profile.age.toFloat()) }
    var weight by remember { mutableStateOf(profile.weightKg.toFloat()) }
    var height by remember { mutableStateOf(profile.heightCm.toFloat()) }
    var driverCategory by remember { mutableStateOf(profile.driverCategory) }
    var isHabitual by remember { mutableStateOf(profile.isHabitualDrinker) }

    Scaffold(
        // Lo spazio per la barra di sistema in fondo è già lasciato dalla schermata che contiene
        // questa: qui non va lasciato di nuovo.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Profilo") },
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
            Column {
                Text("Sesso", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { g ->
                        FilterChip(selected = g == gender, onClick = { gender = g }, label = { Text(g.displayLabel()) })
                    }
                }
            }

            Column {
                Text("Età: ${age.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = age, onValueChange = { age = it }, valueRange = 18f..99f)
            }

            Column {
                Text("Peso: ${weight.toInt()} kg", style = MaterialTheme.typography.labelLarge)
                Slider(value = weight, onValueChange = { weight = it }, valueRange = 30f..200f)
            }

            Column {
                Text("Altezza: ${height.toInt()} cm", style = MaterialTheme.typography.labelLarge)
                Slider(value = height, onValueChange = { height = it }, valueRange = 120f..220f)
            }

            Column {
                Text("Categoria Guidatore", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DriverCategory.entries.forEach { category ->
                        FilterChip(
                            selected = category == driverCategory,
                            onClick = { driverCategory = category },
                            label = {
                                Text(if (category == DriverCategory.STANDARD) "Standard" else "Neopatentato / Professionista")
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bevitore Abituale")
                Switch(checked = isHabitual, onCheckedChange = { isHabitual = it })
            }

            Button(
                onClick = {
                    viewModel.saveProfile(
                        UserProfile(
                            gender = gender,
                            weightKg = weight.toDouble(),
                            heightCm = height.toDouble(),
                            age = age.toInt(),
                            driverCategory = driverCategory,
                            isHabitualDrinker = isHabitual
                        )
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salva Profilo")
            }
        }
    }
}
