package com.example.flux.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.flux.core.GeckoRuntimeHolder
import com.example.flux.core.HistoryManager
import com.example.flux.core.PreferencesManager
import com.example.flux.core.SearchEngine
import com.example.flux.core.Tab
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSessionSettings
import java.util.UUID

fun normalizeInput(input: String, engine: SearchEngine): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val urlPattern = Regex("^[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)+(/\\S*)?$")
    if (urlPattern.matches(trimmed)) return "https://$trimmed"
    return engine.searchUrl(trimmed)
}

fun createTab(runtime: GeckoRuntime, context: Context): Tab {
    // 🎬 Configure la session pour bien gérer les vidéos
    val sessionSettings = GeckoSessionSettings.Builder()
        .suspendMediaWhenInactive(false)
        .build()
    
    val tab = Tab(id = UUID.randomUUID().toString(), session = GeckoSession(sessionSettings))
    tab.session.open(runtime)
    tab.url = PreferencesManager.loadSearchEngine(context).homeUrl
    tab.url = PreferencesManager.loadSearchEngine(context).homeUrl

    tab.session.contentBlockingDelegate = object : ContentBlocking.Delegate {
        override fun onContentBlocked(s: GeckoSession, e: ContentBlocking.BlockEvent) { tab.blockedCount++ }
        override fun onContentLoaded(s: GeckoSession, e: ContentBlocking.BlockEvent) {}
    }

    tab.session.navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onCanGoBack(s: GeckoSession, can: Boolean) { tab.canGoBack = can }
        override fun onCanGoForward(s: GeckoSession, can: Boolean) { tab.canGoForward = can }
    }

    tab.session.progressDelegate = object : GeckoSession.ProgressDelegate {
        override fun onPageStart(s: GeckoSession, url: String) {
            if (!url.startsWith("about:")) tab.url = url
        }
        override fun onPageStop(s: GeckoSession, success: Boolean) {
            if (success &&
                !tab.url.startsWith("about:") &&
                SearchEngine.entries.none { it.homeUrl == tab.url }
            ) {
                HistoryManager.addVisit(context, tab.title, tab.url)
            }
        }
    }

    // 🏷️ VRAI titre de la page (il vit dans ContentDelegate !)
    tab.session.contentDelegate = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(s: GeckoSession, title: String?) {
            if (!title.isNullOrBlank()) tab.title = title
        }
    }

    // 🎬 Autorise la lecture auto + vidéos DRM, refuse le reste (géo, notifications…)
    tab.session.permissionDelegate = object : GeckoSession.PermissionDelegate {
        override fun onContentPermissionRequest(
            s: GeckoSession,
            perm: GeckoSession.PermissionDelegate.ContentPermission
        ): GeckoResult<Int> {
            val response = when (perm.permission) {
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE,
                GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS ->
                    GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                else -> GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
            }
            return GeckoResult.fromValue(response)
        }

        override fun onMediaPermissionRequest(
            s: GeckoSession,
            uri: String,
            video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
            audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
            callback: GeckoSession.PermissionDelegate.MediaCallback
        ) {
            // grant prend deux paramètres individuels, pas des arrays
            callback.grant(
                video?.firstOrNull()?.id,
                audio?.firstOrNull()?.id
            )
        }
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
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val firstTab = remember { createTab(runtime, context) }
    val tabs = remember { mutableStateListOf(firstTab) }
    var activeTabId by remember { mutableStateOf(firstTab.id) }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    var urlInput by remember(activeTabId) { mutableStateOf(activeTab.url) }
    var isUrlFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab.url) {
        if (!isUrlFieldFocused) urlInput = activeTab.url
    }

    val onNewTab: () -> Unit = {
        val newTab = createTab(runtime, context)
        tabs.add(newTab)
        activeTabId = newTab.id
    }

    val openUrlInActiveTab: (String) -> Unit = { url ->
        activeTab.url = url
        urlInput = url
        activeTab.session.loadUri(url)
        showFavorites = false
        showHistory = false
        showSettings = false
    }

    val goHome: () -> Unit = {
        activeTab.url = searchEngine.homeUrl
        activeTab.title = "Nouvel onglet"
        urlInput = searchEngine.homeUrl
        activeTab.session.loadUri(searchEngine.homeUrl)
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
                                val t = createTab(runtime, context); tabs.add(t); activeTabId = t.id
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
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isUrlFieldFocused = it.isFocused },
                            label = { Text("URL ou recherche") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                openUrlInActiveTab(normalizeInput(urlInput, searchEngine))
                            })
                        )
                        Box {
                            var menuOpen by remember { mutableStateOf(false) }
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Moteur")
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
                        Button(onClick = { openUrlInActiveTab(normalizeInput(urlInput, searchEngine)) }) { Text("Go") }
                    }

                    NavigationRow(
                        tab = activeTab,
                        onNewTab = onNewTab,
                        onHome = goHome,
                        onAddFavorite = {
                            PreferencesManager.addFavorite(context, activeTab.title, activeTab.url)
                            Toast.makeText(context, "⭐ Ajouté aux favoris !", Toast.LENGTH_SHORT).show()
                        },
                        onOpenFavorites = { showFavorites = true },
                        onOpenHistory = { showHistory = true },
                        onOpenSettings = { showSettings = true },
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, activeTab.url)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager via"))
                        }
                    )

                    val shouldShowHomePage =
                        activeTab.url == searchEngine.homeUrl ||
                        SearchEngine.entries.any { engine -> activeTab.url == engine.homeUrl }

                    if (shouldShowHomePage) {
                        HomePage(
                            searchEngine = searchEngine,
                            onSearch = { query -> openUrlInActiveTab(normalizeInput(query, searchEngine)) },
                            onOpenUrl = { url -> openUrlInActiveTab(url) }
                        )
                    } else {
                        GeckoViewComposable(modifier = Modifier.fillMaxSize(), activeSession = activeTab.session)
                    }
                }
            }

            if (showFavorites) {
                FavoritesScreen(onBack = { showFavorites = false }, onOpenUrl = openUrlInActiveTab)
            }
            if (showHistory) {
                HistoryScreen(onBack = { showHistory = false }, onOpenUrl = openUrlInActiveTab)
            }
            if (showSettings) {
                SettingsScreen(
                    onBack = {
                        showSettings = false
                        searchEngine = PreferencesManager.loadSearchEngine(context)
                        isDarkMode = PreferencesManager.loadDarkMode(context)
                    }
                )
            }
        }
    }
}

@Composable
fun TabBar(tabs: List<Tab>, activeTabId: String, onTabSelected: (String) -> Unit, onTabClosed: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            TabChip(tab = tab, isActive = tab.id == activeTabId, onClick = { onTabSelected(tab.id) }, onClose = { onTabClosed(tab.id) })
        }
    }
}

@Composable
fun TabChip(tab: Tab, isActive: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.padding(end = 4.dp).clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Default.Close, contentDescription = "Fermer",
            modifier = Modifier.size(16.dp).clickable(onClick = onClose),
            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NavigationRow(
    tab: Tab,
    onNewTab: () -> Unit,
    onHome: () -> Unit,
    onAddFavorite: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onShare: () -> Unit
) {
    val session = tab.session
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { session.goBack() }, enabled = tab.canGoBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
        }
        IconButton(onClick = { session.goForward() }, enabled = tab.canGoForward) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avancer")
        }
        IconButton(onClick = { session.reload() }) { Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir") }
        IconButton(onClick = { session.stop() }) { Icon(Icons.Default.Close, contentDescription = "Arrêter") }
        IconButton(onClick = onHome) { Icon(Icons.Default.Home, contentDescription = "Accueil") }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = "Bloqués", tint = MaterialTheme.colorScheme.primary)
            Text(text = "${tab.blockedCount}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
        }

        Box {
            var mainMenuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { mainMenuOpen = true }) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
            DropdownMenu(expanded = mainMenuOpen, onDismissRequest = { mainMenuOpen = false }) {
                DropdownMenuItem(text = { Text("Nouvel onglet") }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { onNewTab(); mainMenuOpen = false })
                DropdownMenuItem(text = { Text("Ajouter aux favoris") }, leadingIcon = { Icon(Icons.Default.Star, null) }, onClick = { onAddFavorite(); mainMenuOpen = false })
                DropdownMenuItem(text = { Text("Favoris") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) }, onClick = { onOpenFavorites(); mainMenuOpen = false })
                DropdownMenuItem(text = { Text("Historique") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) }, onClick = { onOpenHistory(); mainMenuOpen = false })
                DropdownMenuItem(text = { Text("Partager") }, leadingIcon = { Icon(Icons.Default.Share, null) }, onClick = { onShare(); mainMenuOpen = false })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Paramètres") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { onOpenSettings(); mainMenuOpen = false })
            }
        }
    }
}

@Composable
fun GeckoViewComposable(modifier: Modifier = Modifier, activeSession: GeckoSession) {
    val geckoViewRef = remember { mutableStateOf<GeckoView?>(null) }
    LaunchedEffect(activeSession) { geckoViewRef.value?.setSession(activeSession) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            GeckoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                geckoViewRef.value = this
                setSession(activeSession)
            }
        },
        update = { view -> if (view.session != activeSession) view.setSession(activeSession) }
    )
}