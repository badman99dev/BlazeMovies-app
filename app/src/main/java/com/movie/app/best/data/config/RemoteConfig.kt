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
 *  3. every getter falls back to a bundled default (only BASE_URL lives in gradle)
 *
 * The endpoint, the cache and the bundled defaults are three independent sources,
 * so the app keeps working even if one (or two) of them is unavailable.
 */
object RemoteConfig {

    private const val PREFS = "bm_runtime_config"
    private const val KEY_BLOB = "blob"
    private const val KEY_TS = "fetched_at"
    private const val KEY_SOURCE = "source"

    private const val SOURCE_WORKER = "worker"
    private const val SOURCE_FIREBASE = "firebase"
    private const val XOR_KEY = "k9X!rT2z"

    /**
     * Base64 + XOR decoder. Used for the bundled base URLs (which are encoded at
     * build time by Gradle) and for the config blob itself.
     */
    private fun decodeXor(b64: String): String = runCatching {
        val raw = Base64.decode(b64, Base64.DEFAULT)
        val out = ByteArray(raw.size)
        for (i in raw.indices) out[i] = (raw[i].toInt() xor XOR_KEY[i % XOR_KEY.length].code).toByte()
        String(out, Charsets.UTF_8)
    }.getOrDefault("")
    private const val CONFIG_PATH = "config"

    /** Decoded once from the build-time encoded BuildConfig values. */
    private val bundledPrimary: String by lazy { decodeXor(BuildConfig.BASE_URL_ENC) }
    private val bundledSecondary: String by lazy { decodeXor(BuildConfig.SECONDARY_BASE_ENC) }


    /** Minimum gap between two background refreshes (avoids hammering). */
    private const val REFRESH_FLOOR_MS = 60_000L

    /**
     * Firebase Remote Config parameter that carries the very same blob.
     * It is only used when the worker endpoint is unreachable (e.g. an ISP
     * blocks *.workers.dev), so we never depend on a single transport.
     */
    private const val FIREBASE_PARAM = "cfg"
    private const val FIREBASE_MIN_FETCH_SECONDS = 1800L

    /**
     * Per-service placeholders. Retrofit instances are built once at DI time,
     * so each service gets its own stable (but unroutable) host; the interceptor
     * rewrites it to the real one from the config on every request. That keeps
     * zero real hosts inside the app and still honours a config that arrives
     * after a service was first created.
     */
    private const val PH_IMDB_PRIMARY = "https://imdb-primary.invalid/"
    private const val PH_IMDB_BACKUP = "https://imdb-backup.invalid/"
    private const val PH_SEARCH_KEY = "https://search-key.invalid/"
    private const val PH_SEARCH_API = "https://search-api.invalid/"
    private const val PH_SEARCH_PING = "https://search-ping.invalid/"
    private const val PH_DL_AGENT = "https://dl-agent.invalid/"
    private const val PH_EXTRACTOR = "https://extractor.invalid/"
    private const val PH_PLAYLIST = "https://playlist-host.invalid"
    private const val PH_PLAYER_ALT = "https://player-alt.invalid/"
    private const val PH_SOURCE_HUB = "https://source-hub.invalid/"
    private const val PH_SPORTS = "https://sports.invalid/"
    private const val PH_ZEE5_API = "https://zee5-api.invalid/"
    private const val PH_ZEE5_TOKEN = "https://zee5-token.invalid/"
    private const val PH_CRICKET = "https://cricket.invalid/"

    /** Placeholder hosts the interceptor knows how to map. */
    val placeholderHosts: Set<String> = setOf(
        "imdb-primary.invalid", "imdb-backup.invalid", "search-key.invalid",
        "search-api.invalid", "search-ping.invalid", "dl-agent.invalid",
        "extractor.invalid", "playlist-host.invalid", "player-alt.invalid",
        "source-hub.invalid", "sports.invalid", "zee5-api.invalid",
        "zee5-token.invalid", "cricket.invalid"
    )

    @Volatile private var endpoints: Map<String, String> = emptyMap()
    @Volatile private var bases: List<String> = emptyList()
    @Volatile private var fetchedAt: Long = 0L
    @Volatile private var refreshSeconds: Long = 21600L
    @Volatile private var minAppVersion: Int = 0
    @Volatile private var ready = false

    /** Bumped whenever the applied config changes — lets consumers cache. */
    @Volatile var configVersion: Int = 0
        private set

    /** Where the config currently in memory came from ("worker" / "firebase"). */
    @Volatile var source: String = "builtin"
        private set

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
        source = prefs.getString(KEY_SOURCE, null) ?: "builtin"
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

    /**
     * Suspending wait for the first config (used before config-dependent
     * prefetches). UI is never blocked — callers are already in a coroutine.
     */
    suspend fun awaitReady(timeoutMs: Long = 900L): Boolean {
        if (ready) return true
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(60L)
            if (ready) return true
        }
        return ready
    }

    private fun fetch(context: Context) {
        // 1) worker endpoint(s) — authoritative and instant
        val candidates = (mirrors() + bundledPrimary).distinct()
        for (base in candidates) {
            val body = httpGet(base.trimEnd('/') + "/" + CONFIG_PATH) ?: continue
            val blob = runCatching { JSONObject(body).optString("d") }.getOrNull()
            if (!blob.isNullOrEmpty() && apply(blob)) {
                cache(context, blob, SOURCE_WORKER)
                return
            }
        }
        // 2) Firebase Remote Config — reliable fallback transport
        val fbBlob = fetchFromFirebase()
        if (!fbBlob.isNullOrEmpty() && apply(fbBlob)) {
            cache(context, fbBlob, SOURCE_FIREBASE)
        }
    }

    private fun fetchFromFirebase(): String? = runCatching {
        val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
        val settings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
            .Builder()
            .setMinimumFetchIntervalInSeconds(FIREBASE_MIN_FETCH_SECONDS)
            .build()
        com.google.android.gms.tasks.Tasks.await(rc.setConfigSettingsAsync(settings), 3, TimeUnit.SECONDS)
        com.google.android.gms.tasks.Tasks.await(rc.fetchAndActivate(), 5, TimeUnit.SECONDS)

        // Accepts both shapes: the plain blob (String parameter) or {"d":"<blob>"}
        // (JSON parameter), so either setup in the console works.
        val value = rc.getString(FIREBASE_PARAM)?.trim().orEmpty()
        when {
            value.isEmpty() -> null
            value.startsWith("{") ->
                runCatching { JSONObject(value).optString("d") }.getOrNull()?.takeIf { it.isNotBlank() }
            else -> value
        }
    }.getOrNull()

    private fun cache(context: Context, blob: String, source: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BLOB, blob)
            .putLong(KEY_TS, System.currentTimeMillis())
            .putString(KEY_SOURCE, source)
            .apply()
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
        configVersion++
        if (source == "builtin") source = "cache"
        ready = true
        true
    }.getOrDefault(false)

    /** true when the runtime config actually supplied this key. */
    fun has(key: String): Boolean = !endpoints[key].isNullOrBlank()

    fun raw(key: String, fallback: String): String =
        endpoints[key]?.takeIf { it.isNotBlank() } ?: fallback

    // ── API surfaces ─────────────────────────────────────────────
    /** Bundled primary front (decoded from the build-time encoded value). */
    fun apiBootstrap(): String = bundledPrimary

    fun apiBase(): String = bases.firstOrNull() ?: bundledPrimary

    /** All known fronts — primary first, secondary second. */
    fun mirrors(): List<String> =
        (bases.ifEmpty { listOf(bundledPrimary, bundledSecondary) }).distinct()

    /** Secondary front, used by the interceptor when the primary fails. */
    fun apiSecondary(): String? = mirrors().getOrNull(1)
    fun apiOrigin(): String = apiBase().removeSuffix("v1/").trimEnd('/')
    fun imdbPrimary(): String = raw("imdb_primary", PH_IMDB_PRIMARY)
    fun imdbBackup(): String = raw("imdb_backup", PH_IMDB_BACKUP)
    fun searchKey(): String = raw("search_key", PH_SEARCH_KEY)
    fun searchApi(): String = raw("search_api", PH_SEARCH_API)
    fun searchPing(): String = raw("search_ping", PH_SEARCH_PING)
    fun downloadAgent(): String = raw("download_agent", PH_DL_AGENT)
    fun extractor(): String = raw("extractor", PH_EXTRACTOR)
    fun extractorPlaylistHost(): String =
        raw("extractor_playlist_host", PH_PLAYLIST)
    fun playerAlt(): String = raw("player_alt", PH_PLAYER_ALT)
    fun sourceHub(): String = raw("source_hub", PH_SOURCE_HUB)
    fun sports(): String = raw("sports", PH_SPORTS)
    fun zee5Api(): String = raw("zee5_api", PH_ZEE5_API)
    fun zee5Token(): String = raw("zee5_token", PH_ZEE5_TOKEN)
    fun cricketScores(): String = raw("cricket_scores", PH_CRICKET)
    fun playerReferer(): String = raw("player_referer", "")
}