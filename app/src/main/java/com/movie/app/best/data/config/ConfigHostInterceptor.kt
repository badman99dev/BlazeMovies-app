package com.movie.app.best.data.config

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Resolves every service host at request time.
 *
 * Retrofit instances are built once (at DI time) with stable placeholder hosts
 * such as `zee5-api.invalid`, so no real backend host is baked into the app.
 * This interceptor maps those placeholder hosts — and the bundled bootstrap
 * front — to whatever the runtime config currently says. If the config has not
 * supplied a value yet, the placeholder is left as-is and the call fails
 * gracefully instead of hitting a wrong server.
 *
 * Only the main API (bootstrap front) gets the secondary-front failover.
 */
class ConfigHostInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val target = map[request.url.host] ?: return chain.proceed(request)

        val isMainApi = request.url.host == hostOf(RemoteConfig.apiBootstrap())
        val secondary = if (isMainApi) {
            RemoteConfig.apiSecondary()?.takeUnless { it == RemoteConfig.apiBase() }
        } else {
            null
        }

        val response = try {
            chain.proceed(request.withOrigin(target))
        } catch (io: IOException) {
            secondary?.let { fallback ->
                runCatching { chain.proceed(request.withOrigin(fallback)) }.getOrNull()?.let { return it }
            }
            throw io
        }

        if (secondary != null && response.code >= 500 && isRetryable(request)) {
            val code = response.code
            response.close()
            return runCatching { chain.proceed(request.withOrigin(secondary)) }.getOrElse { cause ->
                throw IOException("primary returned HTTP $code and the secondary front failed", cause)
            }
        }

        return response
    }

    private fun isRetryable(request: Request): Boolean =
        request.method == "GET" || request.method == "HEAD"

    private fun Request.withOrigin(origin: String): Request {
        val parsed = origin.toHttpUrlOrNull() ?: return this
        if (parsed.scheme == url.scheme && parsed.host == url.host && parsed.port == url.port) return this
        return newBuilder()
            .url(
                url.newBuilder()
                    .scheme(parsed.scheme)
                    .host(parsed.host)
                    .port(parsed.port)
                    .build()
            )
            .build()
    }

    private val map: Map<String, String>
        get() = buildMap {
            put(hostOf(RemoteConfig.apiBootstrap()), RemoteConfig.apiBase())
            RemoteConfig.apiSecondary()?.let { put(hostOf(it), RemoteConfig.apiBase()) }
            put("imdb-primary.invalid", RemoteConfig.imdbPrimary())
            put("imdb-backup.invalid", RemoteConfig.imdbBackup())
            put("search-key.invalid", RemoteConfig.searchKey())
            put("search-api.invalid", RemoteConfig.searchApi())
            put("search-ping.invalid", RemoteConfig.searchPing())
            put("dl-agent.invalid", RemoteConfig.downloadAgent())
            put("extractor.invalid", RemoteConfig.extractor())
            put("playlist-host.invalid", RemoteConfig.extractorPlaylistHost())
            put("player-alt.invalid", RemoteConfig.playerAlt())
            put("source-hub.invalid", RemoteConfig.sourceHub())
            put("sports.invalid", RemoteConfig.sports())
            put("zee5-api.invalid", RemoteConfig.zee5Api())
            put("zee5-token.invalid", RemoteConfig.zee5Token())
            put("cricket.invalid", RemoteConfig.cricketScores())
        }

    private companion object {
        fun hostOf(url: String): String =
            url.substringAfter("://", "").substringBefore('/').substringBefore(':')
    }
}
