package com.movie.app.best.ui.screens.notification

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.ui.components.CompactPageHeader
import com.movie.app.best.ui.theme.AppRed

@Composable
fun MessageScreen(
    onBackClick: () -> Unit,
    onNavigateLink: (String) -> Unit = {},
    viewModel: MessageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompactPageHeader(
            title = uiState.title.ifBlank { "Message" },
            onBackClick = onBackClick
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppRed)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error.orEmpty(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
            !uiState.markdown.isNullOrBlank() -> {
                MarkdownWebView(
                    markdown = uiState.markdown.orEmpty(),
                    onNavigateLink = onNavigateLink,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Empty message",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Renders real markdown with marked.js inside a locally bundled WebView
 * (assets/markdown). app:// links are routed back into the NavHost, http(s)
 * links open in the external browser.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MarkdownWebView(
    markdown: String,
    onNavigateLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestNavigate = rememberUpdatedState(onNavigateLink)

    val encoded = remember(markdown) {
        Base64.encodeToString(markdown.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }
    val latestEncoded = rememberUpdatedState(encoded)
    val pageReady = remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.loadsImagesAutomatically = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        return when {
                            url.startsWith("app://", ignoreCase = true) -> {
                                latestNavigate.value(url)
                                true
                            }
                            url.startsWith("http", ignoreCase = true) -> {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                                true
                            }
                            else -> false
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageReady.value = true
                        view?.evaluateJavascript(
                            "window.renderMarkdown && window.renderMarkdown('${latestEncoded.value}')",
                            null
                        )
                    }
                }

                loadUrl("file:///android_asset/markdown/index.html")
            }
        },
        update = { webView ->
            if (pageReady.value) {
                webView.evaluateJavascript(
                    "window.renderMarkdown && window.renderMarkdown('${latestEncoded.value}')",
                    null
                )
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        }
    )
}