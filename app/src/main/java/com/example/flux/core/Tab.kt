package com.example.flux.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mozilla.geckoview.GeckoSession

data class Tab(
    val id: String,
    val session: GeckoSession,
    var title: String = "Nouvel onglet",
    var url: String = SearchEngine.BRAVE.homeUrl
) {
    // 🛡️ Réactif : l'UI se met à jour toute seule quand le compteur change
    var blockedCount by mutableStateOf(0)
}