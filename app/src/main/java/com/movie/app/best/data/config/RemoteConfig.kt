package com.movie.app.best.data.config

import android.content.Context
import android.util.Base64
import com.movie.app.best.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Runtime configuration.
 *
 * Flow:
 *  1. [init] loads the cached blob from SharedPreferences — instant, works offline
 *  2. [startRefresh] always fetches `<base>/config` in the background and re-caches
 *     it (cache stays in use meanwhile — no startup latency)
 *  3. every getter falls back to its hardcoded BuildConfig value
 *
 * The endpoint, the cache and the bundled defaults are three independent sources,
 * so the app keeps working even if one (or two) of them is unavailable.
 */
object RemoteConfig {

    private const val PREFS = "bm_runtime_config"
    private const val KEY_BLOB = "blob"
    private const val KEY_TS = "fetched_at"
    private const val XOR_KEY = "k9X!rT2z"
    private const val CONFIG_PATH = "config"

    /** Minimum gap between two background refreshes (avoids hammering). */
    private const val REFRESH_FLOOR_MS = 60_000L

    private const val ZEE5_API_DEFAULT = "https://zee5-no-ads.onrender.com/"
    private const val ZEE5_TOKEN_DEFAULT = "https://artemis.zee5.com/"
    private const val CRICKET_DEFAULT = "https://lscorexapi.onrender.com/"
    private const val PLAYER_REFERER_DEFAULT = "https://wasmer-jhns970ko-badals-projects-03fab3df.vercel.app/"

    @Volatile private var endpoints: Map<String, String> = emptyMap()
    @Volatile private var bases: List<String> = emptyList()
    @Volatile private var fetchedAt: Long = 0L
    @Volatile private var refreshSeconds: Long = 21600L
    @Volatile private var minAppVersion: Int = 0
    @Volatile private var ready = false

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    val isLoaded: Boolean get() = ready
    val forceUpdateBelow: Int get() = minAppVersion

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        fetchedAt = prefs.getLong(KEY_TS, 0L)
        prefs.getString(KEY_BLOB, null)?.let { apply(it) }
    }

    /**
     * Called on every startup.
     *
     * The cached config is already in memory (see [init]) and is used as-is, so
     * startup latency is unaffected. This only kicks off a background fetch that
     * replaces the cache for later launches / later requests in this session.
     */
    fun startRefresh(context: Context) {
        if (System.currentTimeMillis() - fetchedAt < REFRESH_FLOOR_MS) return
        Thread {
            runCatching { fetch(context) }
        }.apply { isDaemon = true }.start()
    }

    private fun fetch(context: Context) {
        val candidates = (bases + listOf(BuildConfig.BASE_URL)).distinct()
        for (base in candidates) {
            val body = httpGet(base.trimEnd('/') + "/" + CONFIG_PATH) ?: continue
            val blob = runCatching { JSONObject(body).optString("d") }.getOrNull()
            if (!blob.isNullOrEmpty() && apply(blob)) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_BLOB, blob)
                    .putLong(KEY_TS, System.currentTimeMillis())
                    .apply()
                return
            }
        }
    }

    private fun httpGet(url: String): String? = runCatching {
        client.newCall(
            Request.Builder().url(url).header("Accept", "application/json").build()
        ).execute().use { if (it.isSuccessful) it.body?.string() else null }
    }.getOrNull()

    private fun apply(blob: String): Boolean = runCatching {
        val raw = Base64.decode(blob, Base64.DEFAULT)
        val out = ByteArray(raw.size)
        for (i in raw.indices) {
            out[i] = (raw[i].toInt() xor XOR_KEY[i % XOR_KEY.length].code).toByte()
        }
        val json = JSONObject(String(out, Charsets.UTF_8))

        val map = HashMap<String, String>()
        json.optJSONObject("endpoints")?.let { eps ->
            eps.keys().forEach { key -> map[key] = eps.optString(key) }
        }
        val list = ArrayList<String>()
        json.optJSONArray("api_bases")?.let { arr ->
            for (i in 0 until arr.length()) list.add(arr.optString(i))
        }
        if (map.isEmpty() && list.isEmpty()) return false

        endpoints = map
        if (list.isNotEmpty()) bases = list
        refreshSeconds = json.optLong("refresh_seconds", refreshSeconds)
        minAppVersion = json.optInt("min_app_version", 0)
        ready = true
        true
    }.getOrDefault(false)

    fun raw(key: String, fallback: String): String =
        endpoints[key]?.takeIf { it.isNotBlank() } ?: fallback

    // ── API surfaces ─────────────────────────────────────────────
    fun apiBase(): String = bases.firstOrNull() ?: BuildConfig.BASE_URL
    fun apiMirrors(): List<String> = bases
    fun apiOrigin(): String = apiBase().removeSuffix("v1/").trimEnd('/')
    fun imdbPrimary(): String = raw("imdb_primary", BuildConfig.IMDB1_BASE_URL)
    fun imdbBackup(): String = raw("imdb_backup", BuildConfig.IMDB2_BASE_URL)
    fun searchKey(): String = raw("search_key", BuildConfig.MEILI_KEY_URL)
    fun searchApi(): String = raw("search_api", BuildConfig.MEILI_SEARCH_URL)
    fun searchPing(): String = raw("search_ping", BuildConfig.MEILI_PING_URL)
    fun downloadAgent(): String = raw("download_agent", BuildConfig.DL_AGENT_BASE_URL)
    fun extractor(): String = raw("extractor", BuildConfig.GEMMA_BASE_URL)
    fun extractorPlaylistHost(): String =
        raw("extractor_playlist_host", BuildConfig.GEMMA_PLAYLIST_HOST)
    fun playerAlt(): String = raw("player_alt", BuildConfig.SPARKLE_BASE_URL)
    fun sourceHub(): String = raw("source_hub", BuildConfig.SOURCE_HUB_BASE_URL)
    fun sports(): String = raw("sports", BuildConfig.SPORTS_BASE_URL)
    fun zee5Api(): String = raw("zee5_api", ZEE5_API_DEFAULT)
    fun zee5Token(): String = raw("zee5_token", ZEE5_TOKEN_DEFAULT)
    fun cricketScores(): String = raw("cricket_scores", CRICKET_DEFAULT)
    fun playerReferer(): String = raw("player_referer", PLAYER_REFERER_DEFAULT)
}