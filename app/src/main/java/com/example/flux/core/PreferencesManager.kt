package com.example.flux.core

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object PreferencesManager {
    private const val PREFS_NAME = "flux_prefs"
    private const val KEY_SEARCH_ENGINE = "search_engine"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_FAVORITES = "favorites"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSearchEngine(context: Context, engine: SearchEngine) {
        // ✅ commit() = écriture immédiate et garantie (apply() est asynchrone)
        prefs(context).edit().putString(KEY_SEARCH_ENGINE, engine.name).commit()
    }

    fun loadSearchEngine(context: Context): SearchEngine {
        val name = prefs(context).getString(KEY_SEARCH_ENGINE, SearchEngine.BRAVE.name)
        return try { SearchEngine.valueOf(name!!) } catch (e: Exception) { SearchEngine.BRAVE }
    }

    fun saveDarkMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).commit()
    }

    fun loadDarkMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK_MODE, false)

    fun loadFavorites(context: Context): List<Pair<String, String>> {
        val json = prefs(context).getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                obj.getString("title") to obj.getString("url")
            }
        } catch (e: Exception) { emptyList() }
    }

    fun addFavorite(context: Context, title: String, url: String) {
        val list = loadFavorites(context).toMutableList()
        if (list.any { it.second == url }) return
        list.add(title to url)
        saveFavorites(context, list)
    }

    fun removeFavorite(context: Context, url: String) {
        saveFavorites(context, loadFavorites(context).filter { it.second != url })
    }

    fun clearFavorites(context: Context) {
        prefs(context).edit().remove(KEY_FAVORITES).commit()
    }

    private fun saveFavorites(context: Context, list: List<Pair<String, String>>) {
        val arr = JSONArray()
        list.forEach { (t, u) ->
            arr.put(JSONObject().put("title", t).put("url", u))
        }
        prefs(context).edit().putString(KEY_FAVORITES, arr.toString()).commit()
    }
}