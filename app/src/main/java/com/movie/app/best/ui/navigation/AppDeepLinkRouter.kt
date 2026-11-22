package com.movie.app.best.ui.navigation

import android.net.Uri
import androidx.navigation.NavController

/**
 * Routes `app://internal/<link>` deep links into the in-app browser.
 *
 * Supported forms (heading is optional):
 *   app://internal/https://example.com/page
 *   app://internal/https%3A%2F%2Fexample.com%2Fpage
 *   app://internal/https://example.com/page?heading=Help%20Center
 *   app://internal?url=https%3A%2F%2Fexample.com&heading=Help
 */
object AppDeepLinkRouter {

    private const val PREFIX = "app://internal"

    data class InternalLink(val url: String, val heading: String)

    fun parseInternal(uri: String): InternalLink? {
        val raw = uri.trim()
        if (!raw.startsWith(PREFIX, ignoreCase = true)) return null

        var rest = raw.substring(PREFIX.length)

        // Query form: app://internal?url=...&heading=...
        if (rest.startsWith("?")) {
            val q = runCatching { Uri.parse(PREFIX + rest) }.getOrNull() ?: return null
            val url = q.getQueryParameter("url")
                ?: q.getQueryParameter("link")
                ?: return null
            val heading = q.getQueryParameter("heading")
                ?: q.getQueryParameter("h")
                ?: q.getQueryParameter("title")
                ?: ""
            val decoded = decodeMaybe(url)
            return if (isWebUrl(decoded)) InternalLink(decoded, heading) else null
        }

        if (rest.startsWith("/")) rest = rest.substring(1)

        // Path form: app://internal/<url>[?heading=...]
        var heading = ""
        val marker = HEADING_MARKERS.firstOrNull { rest.contains(it, ignoreCase = true) }
        if (marker != null) {
            val idx = rest.indexOf(marker, ignoreCase = true)
            heading = Uri.decode(rest.substring(idx + marker.length).substringBefore("&"))
            rest = rest.substring(0, idx)
        }

        val url = decodeMaybe(rest)
        if (!isWebUrl(url)) return null
        return InternalLink(url, heading)
    }

    /**
     * Navigates to the in-app browser when [uri] is an `app://internal/...` link.
     * Returns true when handled.
     */
    fun navigateInternal(navController: NavController, uri: String): Boolean {
        val link = parseInternal(uri) ?: return false
        return runCatching {
            navController.navigate(Screen.InAppBrowser.createRoute(link.url, link.heading))
            true
        }.getOrDefault(false)
    }

    private val HEADING_MARKERS = listOf(
        "?heading=", "?h=", "?title=",
        "&heading=", "&h=", "&title="
    )

    private fun decodeMaybe(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.contains('%')) {
            runCatching { Uri.decode(trimmed) }.getOrDefault(trimmed)
        } else {
            trimmed
        }
    }

    private fun isWebUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
}