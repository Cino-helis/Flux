package com.example.flux.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flux.core.HistoryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    val context = LocalContext.current
    var history by remember { mutableStateOf(HistoryManager.load(context)) }
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    BackHandler { onBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                Text("Historique", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = { HistoryManager.clear(context); history = emptyList() }) { Text("Effacer") }
                }
            }

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun historique pour l'instant.")
                }
            } else {
                LazyColumn {
                    items(history, key = { it.url + it.timestamp }) { visit ->
                        ListItem(
                            headlineContent = { Text(visit.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(visit.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(dateFormat.format(Date(visit.timestamp)), style = MaterialTheme.typography.bodySmall)
                            },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.List, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { onOpenUrl(visit.url) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}