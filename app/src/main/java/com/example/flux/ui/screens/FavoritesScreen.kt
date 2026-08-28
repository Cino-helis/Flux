package com.example.flux.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flux.core.PreferencesManager

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(PreferencesManager.loadFavorites(context)) }

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
                Text("Favoris", style = MaterialTheme.typography.titleLarge)
            }

            if (favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun favori pour l'instant.\nUtilise ☰ → Ajouter aux favoris !")
                }
            } else {
                LazyColumn {
                    items(favorites, key = { it.second }) { fav ->
                        ListItem(
                            headlineContent = { Text(fav.first) },
                            supportingContent = {
                                Text(fav.second, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            leadingContent = {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    PreferencesManager.removeFavorite(context, fav.second)
                                    favorites = PreferencesManager.loadFavorites(context)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                                }
                            },
                            modifier = Modifier.clickable { onOpenUrl(fav.second) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}