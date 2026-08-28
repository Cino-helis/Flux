package com.example.flux.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HistoryManager {
    private const val PREFS_NAME = "flux_prefs"
    private const val KEY_HISTORY = "history"
    private const val MAX_ENTRIES = 100

    data class Visit(val title: String, val url: String, val timestamp: Long)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): List<Visit> {
        val json = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Visit(o.getString("title"), o.getString("url"), o.getLong("timestamp"))
            }
        } catch (e: Exception) { emptyList() }
    }

    fun addVisit(context: Context, title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val list = load(context).toMutableList()
        // Évite les doublons consécutifs (ex: quand tu rafraîchis la page)
        if (list.firstOrNull()?.url == url) {
            list[0] = list[0].copy(title = title, timestamp = System.currentTimeMillis())
        } else {
            list.add(0, Visit(title, url, System.currentTimeMillis()))
        }
        val arr = JSONArray()
        list.take(MAX_ENTRIES).forEach { v ->
            arr.put(JSONObject().put("title", v.title).put("url", v.url).put("timestamp", v.timestamp))
        }
        prefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_HISTORY).apply()
    }
}