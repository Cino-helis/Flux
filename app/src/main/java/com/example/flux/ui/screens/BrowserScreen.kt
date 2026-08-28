package com.example.flux.ui.screens

import android.content.Intent
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.flux.core.GeckoRuntimeHolder
import com.example.flux.core.PreferencesManager
import com.example.flux.core.SearchEngine
import com.example.flux.core.Tab
import androidx.compose.ui.unit.dp
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.util.UUID

fun normalizeInput(input: String, engine: SearchEngine): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val urlPattern = Regex("^[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)+(/\\S*)?$")
    if (urlPattern.matches(trimmed)) return "https://$trimmed"
    return engine.searchUrl(trimmed)
}

// 🏭 Fabrique un onglet + branche son compteur anti-pub
fun createTab(runtime: GeckoRuntime): Tab {
    val tab = Tab(id = UUID.randomUUID().toString(), session = GeckoSession())
    tab.session.open(runtime)
    tab.session.contentBlockingDelegate = object : ContentBlocking.Delegate {
        override fun onContentBlocked(s: GeckoSession, e: ContentBlocking.BlockEvent) {
            tab.blockedCount++
        }
        override fun onContentLoaded(s: GeckoSession, e: ContentBlocking.BlockEvent) {}
    }
    return tab
}

@Composable
fun BrowserScreen() {
    val context = LocalContext.current
    val runtime = GeckoRuntimeHolder.runtime!!
    var searchEngine by remember { mutableStateOf(PreferencesManager.loadSearchEngine(context)) }
    var isDarkMode by remember { mutableStateOf(PreferencesManager.loadDarkMode(context)) }
    var showFavorites by remember { mutableStateOf(false) }

    val firstTab = remember { createTab(runtime) }
    val tabs = remember { mutableStateListOf(firstTab) }
    var activeTabId by remember { mutableStateOf(firstTab.id) }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    var urlInput by remember(activeTabId) { mutableStateOf(activeTab.url) }

    val onNewTab: () -> Unit = {
        val newTab = createTab(runtime)
        tabs.add(newTab)
        activeTabId = newTab.id
    }

    LaunchedEffect(isDarkMode) {
        runtime.settings.preferredColorScheme =
            if (isDarkMode) GeckoRuntimeSettings.COLOR_SCHEME_DARK
            else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
    }

    MaterialTheme(colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    TabBar(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelected = { activeTabId = it },
                        onTabClosed = { tabId ->
                            tabs.find { it.id == tabId }?.session?.close()
                            tabs.removeAll { it.id == tabId }
                            if (tabs.isEmpty()) {
                                val t = createTab(runtime); tabs.add(t); activeTabId = t.id
                            } else if (activeTabId == tabId) {
                                activeTabId = tabs.last().id
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("URL ou recherche") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                val url = normalizeInput(urlInput, searchEngine)
                                activeTab.url = url
                                activeTab.session.loadUri(url)
                            })
                        )
                        Box {
                            var menuOpen by remember { mutableStateOf(false) }
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Moteur de recherche")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                SearchEngine.entries.forEach { engine ->
                                    DropdownMenuItem(
                                        text = { Text(if (engine == searchEngine) "✓ ${engine.displayName}" else engine.displayName) },
                                        onClick = { searchEngine = engine; PreferencesManager.saveSearchEngine(context, engine); menuOpen = false }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(if (isDarkMode) "✓ Mode sombre" else "Mode sombre") },
                                    onClick = { isDarkMode = !isDarkMode; PreferencesManager.saveDarkMode(context, isDarkMode) }
                                )
                            }
                        }
                        Button(onClick = {
                            val url = normalizeInput(urlInput, searchEngine)
                            activeTab.url = url
                            activeTab.session.loadUri(url)
                        }) { Text("Go") }
                    }

                    NavigationRow(
                        tab = activeTab,
                        onNewTab = onNewTab,
                        onAddFavorite = {
                            PreferencesManager.addFavorite(context, activeTab.title, activeTab.url)
                            Toast.makeText(context, "⭐ Ajouté aux favoris !", Toast.LENGTH_SHORT).show()
                        },
                        onOpenFavorites = { showFavorites = true },
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, activeTab.url)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager via"))
                        },
                        onPlaceholder = { name ->
                            Toast.makeText(context, "$name : bientôt disponible !", Toast.LENGTH_SHORT).show()
                        }
                    )

                    GeckoViewComposable(
                        modifier = Modifier.fillMaxSize(),
                        activeSession = activeTab.session
                    )
                }
            }

            // ⭐ ÉCRAN FAVORIS (affiché par-dessus le navigateur)
            if (showFavorites) {
                FavoritesScreen(
                    onBack = { showFavorites = false },
                    onOpenUrl = { url ->
                        activeTab.url = url
                        urlInput = url
                        activeTab.session.loadUri(url)
                        showFavorites = false
                    }
                )
            }
        }
    }
}

@Composable
fun TabBar(
    tabs: List<Tab>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            TabChip(
                tab = tab,
                isActive = tab.id == activeTabId,
                onClick = { onTabSelected(tab.id) },
                onClose = { onTabClosed(tab.id) }
            )
        }
    }
}

@Composable
fun TabChip(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tab.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Default.Close,
            contentDescription = "Fermer l'onglet",
            modifier = Modifier.size(16.dp).clickable(onClick = onClose),
            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NavigationRow(
    tab: Tab,
    onNewTab: () -> Unit,
    onAddFavorite: () -> Unit,
    onOpenFavorites: () -> Unit,
    onShare: () -> Unit,
    onPlaceholder: (String) -> Unit
) {
    val session = tab.session
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    DisposableEffect(session) {
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(s: GeckoSession, canGoBackNew: Boolean) { canGoBack = canGoBackNew }
            override fun onCanGoForward(s: GeckoSession, canGoForwardNew: Boolean) { canGoForward = canGoForwardNew }
        }
        onDispose { session.navigationDelegate = null }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { session.goBack() }, enabled = canGoBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
        }
        IconButton(onClick = { session.goForward() }, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avancer")
        }
        IconButton(onClick = { session.reload() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
        }
        IconButton(onClick = { session.stop() }) {
            Icon(Icons.Default.Close, contentDescription = "Arrêter")
        }
        IconButton(onClick = { session.loadUri(SearchEngine.BRAVE.homeUrl) }) {
            Icon(Icons.Default.Home, contentDescription = "Accueil")
        }

        Spacer(modifier = Modifier.weight(1f))

        // 🛡️ Bouclier + compteur de l'onglet actif
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = "Traqueurs bloqués", tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "${tab.blockedCount}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // ☰ Menu hamburger
        Box {
            var mainMenuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { mainMenuOpen = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu principal")
            }
            DropdownMenu(expanded = mainMenuOpen, onDismissRequest = { mainMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Nouvel onglet") },
                    leadingIcon = { Icon(Icons.Default.Add, null) },
                    onClick = { onNewTab(); mainMenuOpen = false }
                )
                DropdownMenuItem(
                    text = { Text("Ajouter aux favoris") },
                    leadingIcon = { Icon(Icons.Default.Star, null) },
                    onClick = { onAddFavorite(); mainMenuOpen = false }
                )
                DropdownMenuItem(
                    text = { Text("Favoris") },
                    leadingIcon = { Icon(Icons.Default.List, null) },
                    onClick = { onOpenFavorites(); mainMenuOpen = false }
                )
                DropdownMenuItem(
                    text = { Text("Historique") },
                    leadingIcon = { Icon(Icons.Default.List, null) },
                    onClick = { onPlaceholder("Historique"); mainMenuOpen = false }
                )
                DropdownMenuItem(
                    text = { Text("Partager") },
                    leadingIcon = { Icon(Icons.Default.Share, null) },
                    onClick = { onShare(); mainMenuOpen = false }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Paramètres") },
                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                    onClick = { onPlaceholder("Paramètres"); mainMenuOpen = false }
                )
            }
        }
    }
}

@Composable
fun GeckoViewComposable(
    modifier: Modifier = Modifier,
    activeSession: GeckoSession
) {
    val geckoViewRef = remember { mutableStateOf<GeckoView?>(null) }

    LaunchedEffect(activeSession) {
        geckoViewRef.value?.setSession(activeSession)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            GeckoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                geckoViewRef.value = this
                setSession(activeSession)
            }
        },
        update = { view ->
            if (view.session != activeSession) {
                view.setSession(activeSession)
            }
        }
    )
}