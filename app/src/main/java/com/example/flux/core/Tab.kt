package com.example.flux.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mozilla.geckoview.GeckoSession

data class Tab(
    val id: String,
    val session: GeckoSession
) {
    var title by mutableStateOf("Nouvel onglet")
    var url by mutableStateOf(SearchEngine.BRAVE.homeUrl)
    var blockedCount by mutableStateOf(0)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isFullScreen by mutableStateOf(false)
}