package com.example.alcolyze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alcolyze.data.DrinkEntity
import com.example.alcolyze.viewmodel.AlcolyzeViewModel
import com.example.alcolyze.viewmodel.TimeOffset

/**
 * Il foglio "Registra un drink": scegli quando l'hai bevuto, filtra o cerca nel listino, tocca
 * un drink per registrarlo. Da qui si possono anche creare, modificare ed eliminare i drink
 * personalizzati.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDrinkSheet(viewModel: AlcolyzeViewModel, onDismiss: () -> Unit) {
    val catalog by viewModel.drinksDisponibili.collectAsState()
    // L'elenco delle categorie non è fisso: cresce da solo man mano che ne compaiono di nuove
    // nel listino.
    val availableCategories by viewModel.availableCategories.collectAsState()
    val selectedOffset by viewModel.selectedTimeOffset.collectAsState()
    var showCustomDialog by remember { mutableStateOf(false) }
    var editingDrink by remember { mutableStateOf<DrinkEntity?>(null) }
    var drinkPendingDelete by remember { mutableStateOf<DrinkEntity?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCategoryFilter by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Registra un drink", style = MaterialTheme.typography.titleLarge)
                Row {
                    IconButton(onClick = {
                        showCategoryFilter = !showCategoryFilter
                        if (!showCategoryFilter) selectedCategory = null
                    }) {
                        Icon(
                            imageVector = if (showCategoryFilter) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = if (showCategoryFilter) "Rimuovi filtro categoria" else "Filtra per categoria",
                            tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (showSearch) "Chiudi ricerca" else "Cerca drink"
                        )
                    }
                }
            }

            if (showCategoryFilter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Tutte") }
                    )
                    availableCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category) }
                        )
                    }
                }
            }

            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cerca un drink...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeOffset.entries.forEach { offset ->
                    FilterChip(
                        selected = offset == selectedOffset,
                        onClick = { viewModel.selectTimeOffset(offset) },
                        label = { Text(offset.label) }
                    )
                }
            }

            TextButton(onClick = { showCustomDialog = true }) {
                Text("+ Crea Drink Personalizzato")
            }

            val categoryFiltered = if (selectedCategory == null) {
                catalog
            } else {
                catalog.filter { it.categoria == selectedCategory }
            }
            val filteredCatalog = if (searchQuery.isBlank()) {
                categoryFiltered
            } else {
                categoryFiltered.filter { it.nome.contains(searchQuery, ignoreCase = true) }
            }
            val drinksByCategory = filteredCatalog.groupBy { it.categoria }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Categorie in ordine alfabetico (sono testo libero, non un elenco fisso).
                drinksByCategory.keys.sorted().forEach { category ->
                    val drinks = drinksByCategory[category].orEmpty().sortedBy { it.nome }
                    if (drinks.isNotEmpty()) {
                        item { Text(category, style = MaterialTheme.typography.labelLarge) }
                        items(drinks) { drink ->
                            DrinkRow(
                                drink = drink,
                                onClick = {
                                    viewModel.logDrink(drink)
                                    onDismiss()
                                },
                                onEdit = if (drink.isCustom) {
                                    { editingDrink = drink }
                                } else null,
                                onDelete = if (drink.isCustom) {
                                    { drinkPendingDelete = drink }
                                } else null
                            )
                        }
                    }
                }
                if (filteredCatalog.isEmpty()) {
                    item {
                        val categoryLabel = selectedCategory
                        Text(
                            when {
                                searchQuery.isBlank() && categoryLabel == null -> "Nessun drink disponibile al momento."
                                searchQuery.isBlank() -> "Nessun drink in \"$categoryLabel\"."
                                categoryLabel == null -> "Nessun drink corrisponde a \"$searchQuery\"."
                                else -> "Nessun drink corrisponde a \"$searchQuery\" in \"$categoryLabel\"."
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomDrinkDialog(
            categories = availableCategories,
            onDismiss = { showCustomDialog = false },
            onSave = { name, volumeMl, abv, category ->
                viewModel.createCustomDrink(name, volumeMl, abv, category)
                showCustomDialog = false
            }
        )
    }

    editingDrink?.let { drink ->
        CustomDrinkDialog(
            categories = availableCategories,
            onDismiss = { editingDrink = null },
            onSave = { name, volumeMl, abv, category ->
                viewModel.updateCustomDrink(drink, name, volumeMl, abv, category)
                editingDrink = null
            },
            initialDrink = drink
        )
    }

    drinkPendingDelete?.let { drink ->
        AlertDialog(
            onDismissRequest = { drinkPendingDelete = null },
            title = { Text("Eliminare \"${drink.nome}\"?") },
            text = {
                Text(
                    "Il drink personalizzato e tutte le voci di consumo associate nel tuo " +
                        "storico verranno eliminati definitivamente: l'azione non è reversibile."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomDrink(drink)
                    drinkPendingDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { drinkPendingDelete = null }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun DrinkRow(
    drink: DrinkEntity,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(drink.nome, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${drink.volumeMl.toInt()} ml • ${drink.gradazioneAbv}% ABV" +
                        if (drink.isCustom) " • Personalizzato" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Menu "⋮" (Modifica / Elimina), solo per i drink creati dall'utente.
                if (onEdit != null && onDelete != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Gestisci drink personalizzato",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Modifica") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Elimina") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%.1f".format(drink.unitaAlcoliche), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "unità",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
