package com.example.alcolyze.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alcolyze.ui.components.AddSessionEntrySheet
import com.example.alcolyze.ui.components.LogDrinkSheet
import com.example.alcolyze.ui.components.LogMealSheet
import com.example.alcolyze.ui.screens.EuforiaDetailScreen
import com.example.alcolyze.ui.screens.HomeScreen
import com.example.alcolyze.ui.screens.HowItWorksScreen
import com.example.alcolyze.ui.screens.InfoScreen
import com.example.alcolyze.ui.screens.IntossicazioneDetailScreen
import com.example.alcolyze.ui.screens.ProfileScreen
import com.example.alcolyze.ui.screens.SafetyScreen
import com.example.alcolyze.ui.screens.StatsScreen
import com.example.alcolyze.ui.screens.StomachSelectScreen
import com.example.alcolyze.ui.screens.WelcomeScreen
import com.example.alcolyze.viewmodel.AlcolyzeViewModel

private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_STOMACH_SELECT = "stomach_select"
private const val ROUTE_HOME = "home"
private const val ROUTE_STATS = "stats"
private const val ROUTE_SAFETY = "safety"
private const val ROUTE_INFO = "info"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_HOW_IT_WORKS = "how_it_works"
private const val ROUTE_INTOSSICAZIONE_DETAIL = "intossicazione_detail"
private const val ROUTE_EUFORIA_DETAIL = "euforia_detail"

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

private val bottomBarDestinations = listOf(
    BottomDestination(ROUTE_HOME, "Home", Icons.Default.Home),
    BottomDestination(ROUTE_STATS, "Statistiche", Icons.Default.QueryStats),
    BottomDestination(ROUTE_SAFETY, "Recupero", Icons.Default.FavoriteBorder),
    BottomDestination(ROUTE_INFO, "Impostazioni", Icons.Default.Settings)
)

private enum class AddEntryStep { NONE, CHOOSER, DRINK, MEAL }

/**
 * La schermata principale dell'app. Decide se mostrare l'introduzione (benvenuto e scelta dello
 * stomaco) o andare dritti alla home, gestisce la barra in basso con le 4 sezioni (Home,
 * Statistiche, Recupero, Impostazioni) e le pagine aperte da lì (Profilo, Come funziona,
 * dettaglio Intossicazione ed Euforia), e il bottone "+" per registrare un drink o un pasto.
 */
@Composable
fun AlcolyzeApp(viewModel: AlcolyzeViewModel) {
    // Finché non si sa se c'è una serata da riprendere si mostra solo un cerchietto di attesa,
    // così non compare per un istante "Inizia la serata" prima di saltare alla home.
    val hasActiveSession by viewModel.hasActiveSession.collectAsState()
    if (hasActiveSession == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != ROUTE_WELCOME && currentRoute != ROUTE_STOMACH_SELECT

    var addEntryStep by remember { mutableStateOf(AddEntryStep.NONE) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Si adatta all'altezza reale della barra di sistema (gesti o pulsanti) su
                        // ogni telefono, così la barra in basso resta sempre sopra di essa.
                        .navigationBarsPadding()
                        // Un filo di respiro sopra e sotto: la barra resta vicina a quella di
                        // sistema senza lasciare un vuoto evidente in mezzo.
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomBarDestinations.forEach { destination ->
                                CompactNavItem(
                                    icon = destination.icon,
                                    label = destination.label,
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                    AddEntryButton(onClick = { addEntryStep = AddEntryStep.CHOOSER })
                }
            } else {
                // Le schermate di apertura (benvenuto e scelta stomaco) non hanno la barra in
                // basso: lasciamo comunque lo spazio della barra di sistema, così il contenuto
                // (es. il bottone CONTINUA) non finisce sotto la barra di navigazione del telefono.
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (hasActiveSession == true) ROUTE_HOME else ROUTE_WELCOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(ROUTE_WELCOME) {
                WelcomeScreen(onStartJourney = { navController.navigate(ROUTE_STOMACH_SELECT) })
            }
            composable(ROUTE_STOMACH_SELECT) {
                StomachSelectScreen(
                    viewModel = viewModel,
                    onContinue = {
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(ROUTE_WELCOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(ROUTE_HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenProfile = { navController.navigate(ROUTE_PROFILE) },
                    onOpenIntossicazioneDetail = { navController.navigate(ROUTE_INTOSSICAZIONE_DETAIL) },
                    onOpenEuforiaDetail = { navController.navigate(ROUTE_EUFORIA_DETAIL) }
                )
            }
            composable(ROUTE_STATS) { StatsScreen(viewModel = viewModel) }
            composable(ROUTE_SAFETY) { SafetyScreen(viewModel = viewModel) }
            composable(ROUTE_INFO) {
                val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                InfoScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { viewModel.setDarkTheme(it) },
                    onOpenProfile = { navController.navigate(ROUTE_PROFILE) },
                    onOpenHowItWorks = { navController.navigate(ROUTE_HOW_IT_WORKS) }
                )
            }
            composable(ROUTE_PROFILE) {
                ProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(ROUTE_HOW_IT_WORKS) {
                HowItWorksScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_INTOSSICAZIONE_DETAIL) {
                IntossicazioneDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(ROUTE_EUFORIA_DETAIL) {
                EuforiaDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }

    when (addEntryStep) {
        AddEntryStep.CHOOSER -> AddSessionEntrySheet(
            onDismiss = { addEntryStep = AddEntryStep.NONE },
            onSelectDrink = { addEntryStep = AddEntryStep.DRINK },
            onSelectMeal = { addEntryStep = AddEntryStep.MEAL }
        )
        AddEntryStep.DRINK -> LogDrinkSheet(
            viewModel = viewModel,
            onDismiss = { addEntryStep = AddEntryStep.NONE }
        )
        AddEntryStep.MEAL -> LogMealSheet(
            viewModel = viewModel,
            onDismiss = { addEntryStep = AddEntryStep.NONE }
        )
        AddEntryStep.NONE -> Unit
    }
}

@Composable
private fun RowScope.CompactNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    // Le 4 voci occupano parti uguali della larghezza disponibile, così le etichette non si
    // accavallano sui telefoni stretti e restano ben distanziate su quelli larghi.
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddEntryButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Aggiungi", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
