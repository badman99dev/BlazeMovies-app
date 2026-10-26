package com.movie.app.best.data.config

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Sends front-end API calls to the runtime configured base(s).
 *
 *  - any request whose host is a known front (the bootstrap host or the bundled
 *    secondary host) is pointed at the config primary
 *  - if the primary fails — I/O error, or 5xx on a body-less/GET style request —
 *    the call is retried once against the secondary front
 *
 * Every other base URL in the app is created from the runtime config, so no
 * other backend host is baked into the build.
 */
class ConfigHostInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host !in frontHosts()) return chain.proceed(request)

        val secondary = RemoteConfig.apiSecondary()?.takeUnless { it == RemoteConfig.apiBase() }
        val primaryReq = request.withOrigin(RemoteConfig.apiBase())

        val response = try {
            chain.proceed(primaryReq)
        } catch (io: IOException) {
            secondary?.let { target ->
                runCatching { chain.proceed(request.withOrigin(target)) }.getOrNull()?.let { return it }
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

    private fun frontHosts(): Set<String> = setOf(
        hostOf(RemoteConfig.apiBootstrap()),
        hostOf(RemoteConfig.apiBase()),
        hostOf(RemoteConfig.apiSecondary() ?: "")
    ).filter { it.isNotEmpty() }.toSet()

    private companion object {
        fun hostOf(url: String): String =
            url.substringAfter("://", "").substringBefore('/').substringBefore(':')
    }
}
