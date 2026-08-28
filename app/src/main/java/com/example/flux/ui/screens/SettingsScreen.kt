package com.example.flux.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.flux.core.HistoryManager
import com.example.flux.core.PreferencesManager
import com.example.flux.core.SearchEngine

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var searchEngine by remember { mutableStateOf(PreferencesManager.loadSearchEngine(context)) }
    var isDarkMode by remember { mutableStateOf(PreferencesManager.loadDarkMode(context)) }
    var showClearDialog by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
                Text("Paramètres", style = MaterialTheme.typography.titleLarge)
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Recherche",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                
                Box {
                    var engineMenuOpen by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text("Moteur de recherche") },
                        supportingContent = { Text(searchEngine.displayName) },
                        leadingContent = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.clickable { engineMenuOpen = true }
                    )
                    DropdownMenu(expanded = engineMenuOpen, onDismissRequest = { engineMenuOpen = false }) {
                        SearchEngine.entries.forEach { engine ->
                            DropdownMenuItem(
                                text = { Text(if (engine == searchEngine) "✓ ${engine.displayName}" else engine.displayName) },
                                onClick = {
                                    searchEngine = engine
                                    PreferencesManager.saveSearchEngine(context, engine)
                                    engineMenuOpen = false
                                    Toast.makeText(context, "Moteur changé : ${engine.displayName}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Text(
                    "Apparence",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Mode sombre") },
                    supportingContent = { Text(if (isDarkMode) "Activé" else "Désactivé") },
                    leadingContent = { Icon(Icons.Default.Settings, null) },
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = {
                                isDarkMode = it
                                PreferencesManager.saveDarkMode(context, isDarkMode)
                            }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Text(
                    "Données",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Effacer les données") },
                    supportingContent = { Text("Favoris, historique, paramètres") },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showClearDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Text(
                    "À propos",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Flux Browser") },
                    supportingContent = { Text("Version 1.0 - Navigateur privé") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Effacer toutes les données ?") },
            text = { Text("Cette action supprimera vos favoris, votre historique et réinitialisera les paramètres.") },
            confirmButton = {
                TextButton(onClick = {
                    PreferencesManager.saveSearchEngine(context, SearchEngine.BRAVE)
                    PreferencesManager.saveDarkMode(context, false)
                    HistoryManager.clear(context)
                    PreferencesManager.clearFavorites(context)
                    searchEngine = SearchEngine.BRAVE
                    isDarkMode = false
                    showClearDialog = false
                    Toast.makeText(context, "✓ Données effacées", Toast.LENGTH_SHORT).show()
                }) { Text("Effacer") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Annuler") }
            }
        )
    }
}