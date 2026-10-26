package com.movie.app.best.data.config

import com.movie.app.best.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Swaps request hosts for the runtime configured ones.
 *
 * Retrofit keeps using the hardcoded BuildConfig URLs (so DI stays untouched),
 * and this interceptor redirects any request whose host matches a known default
 * host to whatever the runtime config says right now.
 */
class ConfigHostInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mapped = mapHost(request.url) ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().url(mapped).build())
    }

    private fun mapHost(url: HttpUrl): HttpUrl? {
        val target = originMap[url.host] ?: return null
        val parsed = target.toHttpUrlOrNull() ?: return null
        if (parsed.scheme == url.scheme && parsed.host == url.host && parsed.port == url.port) return null
        return url.newBuilder()
            .scheme(parsed.scheme)
            .host(parsed.host)
            .port(parsed.port)
            .build()
    }

    private val originMap: Map<String, String>
        get() = mapOf(
            hostOf(BuildConfig.BASE_URL) to RemoteConfig.apiBase(),
            "blazemovies.vercel.app" to RemoteConfig.apiBase(),
            "api.tiffara.com" to RemoteConfig.imdbPrimary(),
            "imdxapi.badman993944.workers.dev" to RemoteConfig.imdbBackup(),
            "meilisearch.badman993944.workers.dev" to RemoteConfig.searchKey(),
            "meilisearch-rs25.onrender.com" to RemoteConfig.searchPing(),
            "sparkling-breeze-1ad6.badman993944.workers.dev" to RemoteConfig.playerAlt(),
            "source-hub-41xg.vercel.app" to RemoteConfig.sourceHub(),
            "slast430did.com" to RemoteConfig.extractor(),
            "dl-agent.badman993944.workers.dev" to RemoteConfig.downloadAgent(),
            "cric.badman993944.workers.dev" to RemoteConfig.sports(),
            "zee5-no-ads.onrender.com" to RemoteConfig.zee5Api(),
            "artemis.zee5.com" to RemoteConfig.zee5Token(),
            "lscorexapi.onrender.com" to RemoteConfig.cricketScores()
        )

    private companion object {
        fun hostOf(url: String): String =
            url.substringAfter("://").substringBefore('/').substringBefore(':')
    }
}