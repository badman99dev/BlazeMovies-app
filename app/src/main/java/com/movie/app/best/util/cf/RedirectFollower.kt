package com.movie.app.best.util.cf

import android.app.Activity
import java.net.URL

class RedirectFollower {

    val tag: String = "[CHAIN]"

    suspend fun follow(
        activity: Activity,
        url: String,
        onLog: (String) -> Unit
    ): ModuleResult? {
        val originalHost = runCatching { URL(url).host }.getOrNull() ?: return null
        val scraper = RedirectWebViewScraper(tag, originalHost)
        return scraper.scrape(
            activity = activity,
            url = url,
            timeoutMs = 40_000L,
            onLog = onLog,
            isOriginalHost = { it == originalHost }
        )
    }
}
