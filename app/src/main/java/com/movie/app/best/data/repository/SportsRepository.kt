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
import kotlinx.coroutines.launch
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
    companion object {
        val DEFAULT_CATEGORIES: List<SportCategory> = listOf(
            SportCategory(id = 1, title = "All", image = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj-hKIdsTEB8MkBtUuda-NxuqPRc-4fdmRYrH9uJu6Q6fIz9E_5VMJgZGmNF-GrgT7vDm4vFhzvm5KF9ui2GOzn58RAA_MFpg4G02pl0-hOKHze7aKLS_HvdzIeuSqMx6-JjV-PlRMkaeKmz-W091kTM7ZiixDID_UvcYM6rjYdk-KA2l03xczFUaJkhEM/s1600/1000010646.png"),
            SportCategory(id = 2, title = "Cricket", image = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgDjbnf-Ujlmu63p2vrgTYcP5aGmNjuiAgRUBTjCjq0_LsB-7SG940YVDzxY3eVphGHaBWyl5cetBcF_Lp_bE5_-19TPaxcdJw-Qz7QdDfipXoYuA-CTQ_hqz5jyR4o4ThBGjxOBB_LpAAwLsiq_ZEnGMEIyxEy5TJc8lM7GdLtlYHTMOZHdwPIFr1FiDw/s1600/1000010573.png"),
            SportCategory(id = 3, title = "Football", image = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEieROLn9bEsxaaE0SX9jDmu3RKZUAFsM5cGLX4C1BPvU4rl4Wg_BUePMzgz0S2kENrTSnJIx-LU94iGND2-toy2SyMTaYmCTatjqXvLvEDQGWpLPvn40MrPzRXz0ojJKuiNAbJXNu9cjEnY7NTNp1spjkQYoorFoXRTCOzM9GJriZsnCN-7slOnabjEvSE/s1600/1000010583.png"),
            SportCategory(id = 9, title = "Motorsport", image = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgg6H0T3D97fV0A7WtqE0Lfvvel7FjdsOF-IbFPqOSHXsKhoaYD11FTO1u-3XDzCSpoP-i3eel3YAdFQ44Iua5BsOzCJ9avzNT7U7sYHoWrjqPuEc_cB_fdxETtFVKlCg6TCS_wM3bg7ivdhi6Ga4Bt_tcTkGa9niFeP1-lmTYChaS8j7xI57-NbbDNkFI/s1600/1000010868.png"),
            SportCategory(id = 4, title = "Boxing", image = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEi0uFuLJNLyJenyGqhzreiekiC_rnp8H1uKe5J1WMevvdZcy33xKE4KQRgRbVGsednFQJhToiqUm-Xc9qosXMsEApNTgrxgXVVyVRsN3rJbPGrFOVPaj-ORmYRJDyqn8h_pPANj5CH2KSJEoeCmW1rOLx7tSrW1LlTR1se4GAlKDcga4_d3rGbypb4dDEQ/s1600/1000010611.png"),
            SportCategory(id = 13, title = "Tennis", image = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgCrY-dmvNk5fkUEZiILfkZcx5xzSll5ht5xbIrVbqhg-Jx1Cvr1TC6siu3d-fXrYrIRcDsW6druwt3BVmzncqcN3NaTP4tlgS-kVQWLQje9CLkhjL1pOl8K-X-VkD5da5XNDPVyJik-aPzQy3IwJSsUM5HGikS705TgYCHCJeAXHmN_RKCboqdjnxP2EQ/s1600/1000022992.png"),
            SportCategory(id = 5, title = "Basketball", image = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiqbsNkqcG25WaIpaz0CM7i8zhcaGKsuhk3cj8cB2X4nwNGuMjdvmntXbhXUFvLFS1rg8IDU8BJ5OTwEjah16j1D3zHrouluOrmbG82qVC6NT-Amn6KjGkG48ey4bGv7di7rm7WGz8baYO3Mw53TKv8wElmjf5eJNsI4G-AuaTC_6naXZBoBsoN9FRJsTA/s1600/1000010603.png")
        )
    }

    private var cachedCategories: List<SportCategory> = DEFAULT_CATEGORIES
    private var cachedEvents: List<SportEvent> = emptyList()

    // Transient one-time prefetch (consumed once and cleared immediately, never retained as persistent cache)
    @Volatile
    private var pendingPrefetch: Pair<String, Deferred<EventWatchResult>>? = null
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Pre-warm fresh categories from backend in background so they are ready before user opens Sports tab
        prefetchScope.launch {
            getCategories(forceRefresh = false)
        }
    }

    fun getInitialCategories(): List<SportCategory> = cachedCategories

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
