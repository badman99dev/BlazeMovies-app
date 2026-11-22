package com.movie.app.best.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.movie.app.best.data.config.RemoteConfig

/**
 * Shareable links.
 *
 * Every shared link points at the permanent Netlify front
 * (https://blazemovies.netlify.app). The Netlify site is set up as an Android
 * App Link: the app verifies `/.well-known/assetlinks.json` on install, so app
 * users open the content directly while everyone else is redirected to the
 * download page (/apps).
 */
object ShareLinks {

    const val HOST = "blazemovies.netlify.app"
    const val BASE = "https://$HOST"

    /** Hosts we accept as "shared content" links (incl. the API fronts). */
    private val SHARE_HOSTS = setOf(
        HOST,
        "blazemovies.badman993944.workers.dev",
        "blazemovies.bkstudy8565.workers.dev",
        "blazemovies.vercel.app",
        "blazemovies.onrender.com",
    )

    fun movie(slug: String): String = "$BASE/movie/${Uri.encode(slug)}"

    fun series(slug: String): String = "$BASE/series/${Uri.encode(slug)}"

    sealed interface Target {
        /** Navigate to an internal NavHost route. */
        data class Route(val route: String) : Target

        /** Open inside the in-app browser. */
        data class Browser(val url: String, val heading: String = "") : Target

        /** Recognised share link with nothing specific to open (e.g. /, /apps). */
        data object Home : Target
    }

    /**
     * Maps an incoming https share link to an in-app destination.
     * Returns null when the link is not ours (caller should use its fallback).
     */
    fun parse(uri: String): Target? {
        val parsed = runCatching { Uri.parse(uri.trim()) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = parsed.host?.lowercase() ?: return null
        if (host !in SHARE_HOSTS) return null

        val segments = parsed.pathSegments.orEmpty().filter { it.isNotBlank() }
        if (segments.isEmpty()) return Target.Home

        return when (segments[0].lowercase()) {
            "movie", "movies", "film" ->
                segments.getOrNull(1)?.let { Target.Route("movie/${Uri.encode(it)}") } ?: Target.Home

            "series", "show", "shows", "tv" ->
                segments.getOrNull(1)?.let { Target.Route("series/${Uri.encode(it)}") } ?: Target.Home

            "app" -> when (segments.getOrNull(1)?.lowercase()) {
                "help", "help-feedback", "support" ->
                    Target.Browser("${RemoteConfig.apiOrigin()}/app/help", "")
                "about" ->
                    Target.Browser("${RemoteConfig.apiOrigin()}/app/about", "")
                else -> Target.Home
            }

            "request", "new-request" -> Target.Route("request")

            "search" -> Target.Route("search")

            else -> Target.Home
        }
    }

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