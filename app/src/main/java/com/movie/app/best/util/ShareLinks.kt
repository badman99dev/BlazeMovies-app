package com.movie.app.best.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.movie.app.best.data.config.RemoteConfig

/**
 * Shareable links.
 *
 * Shared URLs point at the permanent Netlify front
 * (https://blazemovies.netlify.app). That host is an Android App Link, so the
 * app receives the https URL and [toAppDeepLink] turns it into the app's own
 * deep link (e.g. app://movie/<slug>). Everyone without the app gets the
 * download page from Netlify's 301.
 */
object ShareLinks {

    const val HOST = "blazemovies.netlify.app"
    const val BASE = "https://$HOST"

    /** Hosts accepted as "shared content" links. */
    private val SHARE_HOSTS = setOf(
        HOST,
        "blazemovies.badman993944.workers.dev",
        "blazemovies.bkstudy8565.workers.dev",
        "blazemovies.vercel.app",
        "blazemovies.onrender.com",
    )

    fun movie(slug: String): String = "$BASE/movie/${Uri.encode(slug)}"

    fun series(slug: String): String = "$BASE/series/${Uri.encode(slug)}"

    /**
     * https share link → app deep link.
     *
     * https://blazemovies.netlify.app/movie/ramayana-2026-hdtc-full-movie
     *   → app://movie/ramayana-2026-hdtc-full-movie
     */
    fun toAppDeepLink(uri: String): String? {
        val parsed = runCatching { Uri.parse(uri.trim()) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = parsed.host?.lowercase() ?: return null
        if (host !in SHARE_HOSTS) return null

        val segments = parsed.pathSegments.orEmpty().filter { it.isNotBlank() }
        if (segments.isEmpty()) return "app://home"

        val slug = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
        return when (segments[0].lowercase()) {
            "movie", "movies", "film" -> slug?.let { "app://movie/${Uri.encode(it)}" }
            "series", "show", "shows", "tv" -> slug?.let { "app://series/${Uri.encode(it)}" }
            "app" -> when (segments.getOrNull(1)?.lowercase()) {
                "help", "help-feedback", "support" -> internalPage("/app/help")
                "about" -> internalPage("/app/about")
                else -> "app://home"
            }
            "request", "new-request" -> "app://request"
            "search" -> "app://search"
            else -> "app://home"
        }
    }

    private fun internalPage(path: String): String =
        "app://internal/${RemoteConfig.apiOrigin()}$path"

    /** Opens the system share sheet. */
    fun share(context: Context, url: String, title: String = "") {
        val subject = title.ifBlank { "BlazeMovies" }
        val text = if (title.isBlank()) {
            "$url\n\nWatch on BlazeMovies"
        } else {
            "$title\n$url\n\nWatch on BlazeMovies"
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(send, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}