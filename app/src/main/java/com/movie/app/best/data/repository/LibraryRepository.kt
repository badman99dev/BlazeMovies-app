package com.movie.app.best.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class HistoryItem(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val isSeries: Boolean,
    val timestamp: Long
)

data class PlaylistItem(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val isSeries: Boolean
)

@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_library", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getHistory(): List<HistoryItem> {
        cleanOldHistory()
        val json = prefs.getString("history", "[]") ?: "[]"
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addToHistory(item: HistoryItem) {
        val current = getHistory().toMutableList()
        current.removeAll { it.slug == item.slug }
        current.add(0, item)
        if (current.size > 50) current.subList(50, current.size).clear()
        prefs.edit().putString("history", gson.toJson(current)).apply()
    }

    fun removeFromHistory(slug: String) {
        val current = getHistory().toMutableList()
        current.removeAll { it.slug == slug }
        prefs.edit().putString("history", gson.toJson(current)).apply()
    }

    fun clearHistory() {
        prefs.edit().putString("history", "[]").apply()
    }

    private fun cleanOldHistory() {
        val json = prefs.getString("history", "[]") ?: "[]"
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        val list: MutableList<HistoryItem> = gson.fromJson(json, type)
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L)
        val cleaned = list.filter { it.timestamp > twoDaysAgo }
        if (cleaned.size != list.size) {
            prefs.edit().putString("history", gson.toJson(cleaned)).apply()
        }
    }

    fun getPlaylist(name: String): List<PlaylistItem> {
        val json = prefs.getString("playlist_$name", "[]") ?: "[]"
        val type = object : TypeToken<List<PlaylistItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addToPlaylist(name: String, item: PlaylistItem) {
        val current = getPlaylist(name).toMutableList()
        if (current.any { it.slug == item.slug }) return
        current.add(0, item)
        prefs.edit().putString("playlist_$name", gson.toJson(current)).apply()
    }

    fun removeFromPlaylist(name: String, slug: String) {
        val current = getPlaylist(name).toMutableList()
        current.removeAll { it.slug == slug }
        prefs.edit().putString("playlist_$name", gson.toJson(current)).apply()
    }

    fun isInPlaylist(name: String, slug: String): Boolean {
        return getPlaylist(name).any { it.slug == slug }
    }

    fun getDefaultPlayer(): Int {
        return prefs.getInt("default_player", 0)
    }

    fun setDefaultPlayer(player: Int) {
        prefs.edit().putInt("default_player", player).apply()
    }
}
