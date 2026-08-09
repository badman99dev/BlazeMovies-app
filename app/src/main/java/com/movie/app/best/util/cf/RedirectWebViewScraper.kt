package com.movie.app.best.util.cf

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.movie.app.best.data.repository.DOWNLOAD_USER_AGENT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL

internal class RedirectWebViewScraper(
    private val tag: String,
    private val originalHost: String,
    private val onPageLoaded: () -> Unit = {}
) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun scrape(
        activity: Activity,
        url: String,
        timeoutMs: Long,
        onLog: (String) -> Unit,
        isOriginalHost: (String) -> Boolean
    ): ModuleResult? = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<ModuleResult?>()
        var webView: WebView? = null
        var loadedAnyPage = false

        try {
            CookieManager.getInstance().setAcceptCookie(true)
            val wv = WebView(activity).also { webView = it }
            wv.layoutParams = ViewGroup.LayoutParams(1, 1)
            wv.isVerticalScrollBarEnabled = false
            wv.isHorizontalScrollBarEnabled = false

            wv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val newUrl = request?.url?.toString() ?: return false
                    val newHost = runCatching { URL(newUrl).host }.getOrNull() ?: return false
                    when {
                        newHost == originalHost -> {
                            onLog("🔄 $tag: Redirect → $newUrl")
                            return false
                        }
                        isOriginalHost(newHost) -> {
                            onLog("🔄 $tag: Same-site → $newUrl")
                            return false
                        }
                        else -> {
                            onLog("💎 $tag: BINGO! $newUrl")
                            val fileName = extractFileName(newUrl)
                            if (!deferred.isCompleted) deferred.complete(ModuleResult(newUrl, fileName))
                            return true
                        }
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!loadedAnyPage) {
                        loadedAnyPage = true
                        onPageLoaded()
                    }
                    val finalHost = url?.let { runCatching { URL(it).host }.getOrNull() }
                    if (finalHost != null && finalHost != originalHost && !isOriginalHost(finalHost)) {
                        if (!deferred.isCompleted) {
                            onLog("✅ $tag: Jackpot! $url")
                            deferred.complete(ModuleResult(url, extractFileName(url)))
                        }
                    }
                }
            }

            with(wv.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = DOWNLOAD_USER_AGENT
                loadWithOverviewMode = true
                useWideViewPort = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
            }

            onLog("🔄 $tag: Loading $originalHost…")
            wv.loadUrl(url)

            val result = withTimeoutOrNull(timeoutMs) { deferred.await() }
            if (result == null) {
                onLog("❌ $tag: Timeout — no CDN redirect detected")
            }
            return@withContext result
        } catch (e: Exception) {
            onLog("❌ $tag: ${e.message ?: e::class.simpleName}")
            return@withContext null
        } finally {
            try {
                webView?.apply { stopLoading(); removeAllViews(); destroy() }
            } catch (_: Exception) {}
        }
    }

    private fun extractFileName(url: String): String? = runCatching {
        val path = URL(url).path ?: return null
        val name = path.substringAfterLast("/")
        if (name.isNotBlank() && name.contains(".")) name else null
    }.getOrNull()
}
