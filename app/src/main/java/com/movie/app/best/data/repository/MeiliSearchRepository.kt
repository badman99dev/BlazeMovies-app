package com.movie.app.best.data.repository

import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.MeiliHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeiliSearchRepository @Inject constructor() {

    @Volatile
    private var cachedKey: String? = null
    private var keyExpiry: Long = 0
    private val KEY_TTL = 3600000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val KEY_URL = BuildConfig.MEILI_KEY_URL
    private val SEARCH_URL = BuildConfig.MEILI_SEARCH_URL
    private val PING_URL = BuildConfig.MEILI_PING_URL
    private val JSON = "application/json".toMediaType()

    suspend fun pingAndPrefetchKey() {
        coroutineScope {
            launch(Dispatchers.IO) {
                try {
                    val req = Request.Builder().url(PING_URL).get().build()
                    client.newCall(req).execute().close()
                } catch (_: Exception) {}
            }
            launch(Dispatchers.IO) {
                try { refreshKey() } catch (_: Exception) {}
            }
        }
    }

    private suspend fun refreshKey(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(KEY_URL).get().build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string()
                resp.close()
                val key = JSONObject(body ?: "").optString("key", "")
                if (key.isNotEmpty()) {
                    cachedKey = key
                    keyExpiry = System.currentTimeMillis() + KEY_TTL
                    key
                } else null
            } catch (_: Exception) { null }
        }
    }

    suspend fun getKey(): String? {
        cachedKey?.let { key ->
            if (System.currentTimeMillis() < keyExpiry) return key
        }
        return refreshKey()
    }

    private fun doSearch(query: String, limit: Int, offset: Int, key: String): MeiliSearchResponse {
        val jsonBody = JSONObject().apply {
            put("q", query)
            put("limit", limit)
            put("offset", offset)
            put("attributesToRetrieve", JSONArray().apply {
                listOf("id","slug","title","poster_url","release_year","rating",
                    "quality_label","is_series","stream_avl","audio_label",
                    "poster_moderation","content_moderation"
                ).forEach { put(it) }
            })
            put("attributesToHighlight", JSONArray().apply { put("title") })
            put("highlightPreTag", "\u0001")
            put("highlightPostTag", "\u0002")
        }

        val req = Request.Builder().url(SEARCH_URL)
            .post(jsonBody.toString().toRequestBody(JSON))
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .build()

        val resp = client.newCall(req).execute()
        val body = resp.body?.string()
        resp.close()

        if (!resp.isSuccessful || body.isNullOrEmpty()) return MeiliSearchResponse()

        return parseResponse(JSONObject(body))
    }

    private fun parseResponse(json: JSONObject): MeiliSearchResponse {
        val hitsArr = json.optJSONArray("hits") ?: return MeiliSearchResponse()
        val hits = mutableListOf<MeiliHit>()
        for (i in 0 until hitsArr.length()) {
            val h = hitsArr.optJSONObject(i) ?: continue
            hits.add(MeiliHit(
                id = h.optInt("id", 0),
                slug = h.optString("slug", ""),
                title = h.optString("title", ""),
                posterUrl = h.optString("poster_url", ""),
                releaseYear = h.optInt("release_year", 0),
                rating = h.optDouble("rating", 0.0),
                qualityLabel = h.optString("quality_label", ""),
                isSeries = h.optInt("is_series", 0),
                streamAvl = if (h.has("stream_avl")) h.optBoolean("stream_avl") else null,
                audioLabel = h.optString("audio_label", ""),
                posterModeration = h.optString("poster_moderation", "safe").takeIf { it != "safe" },
                contentModeration = h.optJSONObject("content_moderation")?.let { cm ->
                    mapOf(
                        "poster" to cm.optString("poster", "safe"),
                        "screenshots" to cm.optString("screenshots", "safe"),
                        "storyline" to cm.optString("storyline", "none")
                    )
                },
                formatted = h.optJSONObject("_formatted")?.let { f ->
                    mapOf("title" to f.optString("title", ""))
                }
            ))
        }
        return MeiliSearchResponse(
            hits = hits,
            estimatedTotalHits = json.optInt("estimatedTotalHits", 0),
            limit = json.optInt("limit", 20),
            offset = json.optInt("offset", 0)
        )
    }

    suspend fun search(query: String, limit: Int = 20, offset: Int = 0): MeiliSearchResponse {
        val key = getKey() ?: return MeiliSearchResponse()
        return withContext(Dispatchers.IO) {
            try {
                doSearch(query, limit, offset, key)
            } catch (_: Exception) {
                cachedKey = null
                val freshKey = refreshKey()
                if (freshKey != null) {
                    try { doSearch(query, limit, offset, freshKey) }
                    catch (_: Exception) { MeiliSearchResponse() }
                } else MeiliSearchResponse()
            }
        }
    }

    suspend fun suggest(query: String, limit: Int = 8): MeiliSearchResponse {
        return search(query, limit = limit, offset = 0)
    }
}

data class MeiliSearchResponse(
    val hits: List<MeiliHit> = emptyList(),
    val estimatedTotalHits: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0
)
