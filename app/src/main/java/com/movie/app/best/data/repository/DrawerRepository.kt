package com.movie.app.best.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movie.app.best.data.model.DrawerMenuItem
import com.movie.app.best.data.remote.MovieApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawerRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val apiService: MovieApiService
) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("drawer_menu_cache", Context.MODE_PRIVATE)

    private val _items = MutableStateFlow(readCache())
    val items: StateFlow<List<DrawerMenuItem>> = _items.asStateFlow()

    private fun readCache(): List<DrawerMenuItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DrawerMenuItem>>() {}.type
            gson.fromJson<List<DrawerMenuItem>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches the drawer menu. A successful response (even with zero items)
     * replaces the cache so the admin panel stays the single source of truth.
     * On failure the last-good cached menu is kept.
     */
    suspend fun refresh(): Boolean {
        return try {
            val response = apiService.getDrawerMenu()
            if (response.status == "success" && response.data != null) {
                val fresh = response.data.items
                prefs.edit().putString(KEY_ITEMS, gson.toJson(fresh)).apply()
                _items.value = fresh
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        const val KEY_ITEMS = "items_json"
    }
}