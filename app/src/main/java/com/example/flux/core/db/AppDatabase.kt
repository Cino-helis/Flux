package com.example.flux.core.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ⭐ Entité Favori
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

// 📜 Entité Historique
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites WHERE url = :url")
    fun countByUrl(url: String): Int

    @Insert
    fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE url = :url")
    fun deleteByUrl(url: String)

    @Query("DELETE FROM favorites")
    fun deleteAll()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentFlow(): Flow<List<HistoryEntity>>

    @Insert
    fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 1")
    fun getLast(): HistoryEntity?

    @Query("UPDATE history SET title = :title, timestamp = :time WHERE id = :id")
    fun updateTitle(id: Long, title: String, time: Long)

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY timestamp DESC LIMIT 100)")
    fun trim()

    @Query("DELETE FROM history")
    fun deleteAll()
}

@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flux.db"
                ).build().also { INSTANCE = it }
            }
    }
}