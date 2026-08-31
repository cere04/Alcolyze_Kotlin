package com.example.alcolyze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.alcolyze.data.AlcolyzeRepository
import com.example.alcolyze.data.ThemePreferences
import com.example.alcolyze.ui.AlcolyzeApp
import com.example.alcolyze.ui.theme.AlcolyzeTheme
import com.example.alcolyze.viewmodel.AlcolyzeViewModel
import com.example.alcolyze.viewmodel.AlcolyzeViewModelFactory

/** La schermata iniziale dell'app: prepara i dati condivisi, sceglie il tema e mostra l'interfaccia. */
class MainActivity : ComponentActivity() {

    private val viewModel: AlcolyzeViewModel by viewModels {
        AlcolyzeViewModelFactory(
            AlcolyzeRepository((application as AlcolyzeApplication).database.alcolyzeDao()),
            ThemePreferences(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            AlcolyzeTheme(darkTheme = isDarkTheme) {
                AlcolyzeApp(viewModel = viewModel)
            }
        }
    }
}
