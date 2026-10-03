package com.movie.app.best.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.movie.app.best.data.model.SportCategory
import com.movie.app.best.data.model.SportEvent
import com.movie.app.best.data.model.SportStream
import com.movie.app.best.data.remote.SportsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SportsRepository @Inject constructor(
    private val apiService: SportsApiService,
    private val gson: Gson
) {
    private var cachedCategories: List<SportCategory> = emptyList()
    private var cachedEvents: List<SportEvent> = emptyList()

    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<SportCategory>> = withContext(Dispatchers.IO) {
        try {
            if (!forceRefresh && cachedCategories.isNotEmpty()) {
                return@withContext Result.success(cachedCategories)
            }
            val resp = apiService.getEventCategories()
            if (resp.ok && resp.data.isNotEmpty()) {
                cachedCategories = resp.data
                Result.success(resp.data)
            } else {
                Result.success(cachedCategories)
            }
        } catch (e: Exception) {
            if (cachedCategories.isNotEmpty()) {
                Result.success(cachedCategories)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getEvents(forceRefresh: Boolean = false): Result<List<SportEvent>> = withContext(Dispatchers.IO) {
        try {
            if (!forceRefresh && cachedEvents.isNotEmpty()) {
                return@withContext Result.success(cachedEvents)
            }
            val resp = apiService.getEvents()
            if (resp.ok && resp.data.isNotEmpty()) {
                cachedEvents = resp.data
                Result.success(resp.data)
            } else {
                Result.success(cachedEvents)
            }
        } catch (e: Exception) {
            if (cachedEvents.isNotEmpty()) {
                Result.success(cachedEvents)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getEventStreams(slug: String): Result<List<SportStream>> = withContext(Dispatchers.IO) {
        try {
            val responseBody = apiService.getEventStreams(slug)
            val jsonString = responseBody.string()
            val jsonElement = JsonParser.parseString(jsonString)

            val streamList = when {
                jsonElement.isJsonArray -> {
                    val type = object : TypeToken<List<SportStream>>() {}.type
                    gson.fromJson<List<SportStream>>(jsonElement, type) ?: emptyList()
                }
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    when {
                        obj.has("streams") && obj.get("streams").isJsonArray -> {
                            val type = object : TypeToken<List<SportStream>>() {}.type
                            gson.fromJson<List<SportStream>>(obj.get("streams"), type) ?: emptyList()
                        }
                        obj.has("data") && obj.get("data").isJsonArray -> {
                            val type = object : TypeToken<List<SportStream>>() {}.type
                            gson.fromJson<List<SportStream>>(obj.get("data"), type) ?: emptyList()
                        }
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
            Result.success(streamList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCategoryCount(categoryTitle: String, events: List<SportEvent>): Int {
        if (categoryTitle.equals("All", ignoreCase = true)) return events.size
        return events.count { event ->
            val cat = event.eventInfo?.eventCat ?: ""
            val eventName = event.eventInfo?.eventName ?: ""
            val mainCat = event.cat ?: ""
            cat.contains(categoryTitle, ignoreCase = true) ||
                eventName.contains(categoryTitle, ignoreCase = true) ||
                mainCat.contains(categoryTitle, ignoreCase = true)
        }
    }
}
