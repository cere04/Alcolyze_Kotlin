package com.example.alcolyze.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.alcolyze.ui.components.BestPracticesSection
import com.example.alcolyze.utils.BacCalculator
import com.example.alcolyze.viewmodel.AlcolyzeViewModel

/**
 * La sezione "Recupero": un bottone per chiamare il 112, il promemoria del proprio limite
 * legale e i consigli su cosa fare e cosa evitare.
 */
@Composable
fun SafetyScreen(viewModel: AlcolyzeViewModel) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sicurezza", style = MaterialTheme.typography.headlineSmall)

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SOS - CHIAMA EMERGENZA")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Il tuo limite legale", style = MaterialTheme.typography.titleMedium)
                Text("${BacCalculator.legalLimit(profile)} g/L, in base alla categoria di patente selezionata nel profilo.")
            }
        }

        BestPracticesSection()
    }
}
