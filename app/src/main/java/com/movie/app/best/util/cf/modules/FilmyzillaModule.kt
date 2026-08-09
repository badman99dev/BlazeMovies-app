package com.movie.app.best.util.cf.modules

import android.app.Activity
import com.movie.app.best.util.cf.HostBypassModule
import com.movie.app.best.util.cf.ModuleResult
import com.movie.app.best.util.cf.RedirectWebViewScraper
import java.net.URL

class FilmyzillaModule : HostBypassModule {

    override val tag: String = "[FILMYZILLA]"

    private companion object {
        val HOST_REGEX =
            Regex("^(www\\.)?filmyzill(a|ay)\\d+\\.com$", RegexOption.IGNORE_CASE)
    }

    override fun matches(host: String): Boolean = HOST_REGEX.matches(host)

    override suspend fun scrape(
        activity: Activity,
        url: String,
        onLog: (String) -> Unit
    ): ModuleResult? {
        val originalHost = runCatching { URL(url).host }.getOrNull() ?: return null
        if (!matches(originalHost)) {
            onLog("⚠️ ${tag}: host mismatch — skipping")
            return null
        }
        val scraper = RedirectWebViewScraper(tag, originalHost)
        return scraper.scrape(
            activity = activity,
            url = url,
            timeoutMs = 40_000L,
            onLog = onLog,
            isOriginalHost = { matches(it) }
        )
    }
}
