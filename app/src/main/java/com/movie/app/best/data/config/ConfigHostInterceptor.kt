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
            // Bootstrap host (the only URL still baked into the build). Every other
            // base URL is created from the runtime config, so no other host needs
            // rewriting — and no other backend host is present in the APK.
            hostOf(BuildConfig.BASE_URL) to RemoteConfig.apiBase()
        )

    private companion object {
        fun hostOf(url: String): String =
            url.substringAfter("://").substringBefore('/').substringBefore(':')
    }
}