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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SlugDetailsResult {
    data class Success(val event: SportEvent) : SlugDetailsResult
    object NotFound : SlugDetailsResult
    data class Error(val message: String) : SlugDetailsResult
}

sealed interface EventWatchResult {
    data class Success(val event: SportEvent, val streams: List<SportStream>) : EventWatchResult
    object NotFound : EventWatchResult
    data class Error(val message: String) : EventWatchResult
}

@Singleton
class SportsRepository @Inject constructor(
    private val apiService: SportsApiService,
    private val gson: Gson
) {
    private var cachedCategories: List<SportCategory> = emptyList()
    private var cachedEvents: List<SportEvent> = emptyList()

    // Transient one-time prefetch (consumed once and cleared immediately, never retained as persistent cache)
    @Volatile
    private var pendingPrefetch: Pair<String, Deferred<EventWatchResult>>? = null
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun prefetchWatchEvent(slug: String) {
        if (slug.isBlank()) return
        // Cancel any pending prefetch to free up resources
        pendingPrefetch?.second?.cancel()
        val deferred = prefetchScope.async {
            executeWatchEventFetch(slug)
        }
        pendingPrefetch = Pair(slug, deferred)
    }

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

    suspend fun getSlugDetails(slug: String): SlugDetailsResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSlugDetails(slug)
            when {
                response.isSuccessful && response.body() != null -> {
                    SlugDetailsResult.Success(response.body()!!)
                }
                response.code() == 404 -> {
                    SlugDetailsResult.NotFound
                }
                else -> {
                    SlugDetailsResult.Error("Failed to load match details (HTTP ${response.code()})")
                }
            }
        } catch (e: Exception) {
            SlugDetailsResult.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun getWatchEvent(slug: String): EventWatchResult = withContext(Dispatchers.IO) {
        val prefetch = pendingPrefetch
        // Consume if it matches the current slug, and IMMEDIATELY clear so next time it fetches fresh
        if (prefetch != null && prefetch.first == slug) {
            pendingPrefetch = null
            try {
                return@withContext prefetch.second.await()
            } catch (e: Exception) {
                return@withContext executeWatchEventFetch(slug)
            }
        }
        executeWatchEventFetch(slug)
    }

    private suspend fun executeWatchEventFetch(slug: String): EventWatchResult {
        return try {
            val response = apiService.getEvent(slug)
            if (response.code() == 404) {
                EventWatchResult.NotFound
            } else if (!response.isSuccessful || response.body() == null) {
                EventWatchResult.Error("Failed to load match streams (HTTP ${response.code()})")
            } else {
                val jsonString = response.body()!!.string()
                val jsonElement = JsonParser.parseString(jsonString)

                if (jsonElement.isJsonObject) {
                    val obj = jsonElement.asJsonObject
                    val event = gson.fromJson(obj, SportEvent::class.java)

                    val streamList = when {
                        obj.has("streams") && obj.get("streams").isJsonArray -> {
                            val type = object : TypeToken<List<SportStream>>() {}.type
                            gson.fromJson<List<SportStream>>(obj.get("streams"), type) ?: emptyList()
                        }
                        obj.has("data") && obj.get("data").isJsonArray -> {
                            val type = object : TypeToken<List<SportStream>>() {}.type
                            gson.fromJson<List<SportStream>>(obj.get("data"), type) ?: emptyList()
                        }
                        else -> event.streams
                    }
                    EventWatchResult.Success(event, streamList)
                } else if (jsonElement.isJsonArray) {
                    val type = object : TypeToken<List<SportStream>>() {}.type
                    val streamList: List<SportStream> = gson.fromJson(jsonElement, type) ?: emptyList()
                    val fallbackEvent = SportEvent(slug = slug, title = slug.replace("-", " ").replaceFirstChar { it.uppercase() })
                    EventWatchResult.Success(fallbackEvent, streamList)
                } else {
                    EventWatchResult.Error("Invalid response format")
                }
            }
        } catch (e: Exception) {
            EventWatchResult.Error(e.message ?: "Network error occurred")
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
