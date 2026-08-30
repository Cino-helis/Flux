package com.example.flux.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.db.AppDatabase
import com.example.flux.core.db.FavoriteEntity
import com.example.flux.core.db.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val favoriteDao = AppDatabase.getInstance(app).favoriteDao()
    private val historyDao = AppDatabase.getInstance(app).historyDao()

    // 📡 Flux réactifs : les écrans se mettent à jour tout seuls
    val favorites: StateFlow<List<FavoriteEntity>> =
        favoriteDao.getAllFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> =
        historyDao.getRecentFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ⭐ Favoris
    fun addFavorite(title: String, url: String) = viewModelScope.launch(Dispatchers.IO) {
        if (favoriteDao.countByUrl(url) == 0) {
            favoriteDao.insert(FavoriteEntity(title = title, url = url))
        }
    }

    fun removeFavorite(url: String) = viewModelScope.launch(Dispatchers.IO) {
        favoriteDao.deleteByUrl(url)
    }

    fun clearFavorites() = viewModelScope.launch(Dispatchers.IO) {
        favoriteDao.deleteAll()
    }

    // 📜 Historique
    fun addVisit(title: String, url: String) = viewModelScope.launch(Dispatchers.IO) {
        if (url.isBlank() || url.startsWith("about:")) return@launch
        val last = historyDao.getLast()
        if (last?.url == url) {
            // Même page rechargée : on met juste à jour l'heure
            historyDao.updateTitle(last.id, title, System.currentTimeMillis())
        } else {
            historyDao.insert(HistoryEntity(title = title, url = url))
            historyDao.trim() // Garde max 100 entrées
        }
    }

    fun clearHistory() = viewModelScope.launch(Dispatchers.IO) {
        historyDao.deleteAll()
    }
}