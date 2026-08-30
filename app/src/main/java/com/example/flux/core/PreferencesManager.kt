package com.example.flux.core

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "flux_prefs"
    private const val KEY_SEARCH_ENGINE = "search_engine"
    private const val KEY_DARK_MODE = "dark_mode"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSearchEngine(context: Context, engine: SearchEngine) {
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
}