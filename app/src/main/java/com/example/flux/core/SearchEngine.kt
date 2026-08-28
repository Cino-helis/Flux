package com.example.flux.core

import android.net.Uri

enum class SearchEngine(
    val displayName: String,
    val homeUrl: String,
    private val template: String
) {
    BRAVE("Brave Search", "https://search.brave.com", "https://search.brave.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com", "https://duckduckgo.com/?q=%s"),
    STARTPAGE("Startpage", "https://www.startpage.com", "https://www.startpage.com/sp/search?query=%s"),
    QWANT("Qwant", "https://www.qwant.com", "https://www.qwant.com/?q=%s"),
    GOOGLE("Google", "https://www.google.com", "https://www.google.com/search?q=%s");

    fun searchUrl(query: String): String = template.replace("%s", Uri.encode(query))
}