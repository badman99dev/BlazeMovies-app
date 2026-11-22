package com.movie.app.best.ui.screens.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.movie.app.best.ui.theme.AppDarkRed
import com.movie.app.best.ui.theme.AppRed

/**
 * Minimal in-app browser: back button on the left, and on the right either the
 * supplied heading (when present) or the BlazeMovies wordmark. No URL bar.
 */
@Composable
fun InAppBrowserScreen(
    url: String,
    heading: String = "",
    onBackClick: () -> Unit = {},
    onOpenAppLink: (String) -> Unit = {}
) {
    var progress by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val openAppLink = rememberUpdatedState(onOpenAppLink)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        BrowserHeader(heading = heading, onBackClick = onBackClick)

        if (loading && url.isNotBlank()) {
            LinearProgressIndicator(
                progress = { (progress.coerceIn(0, 100)) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = AppRed,
                trackColor = Color.Transparent
            )
        }

        if (url.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Invalid link",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        BrowserWebView(
            url = url,
            onOpenAppLink = { openAppLink.value(it) },
            onProgress = { p -> progress = p },
            onLoading = { loading = it },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BrowserHeader(heading: String, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(54.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            if (heading.isNotBlank()) {
                Text(
                    text = heading,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                BlazeWordmark()
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 1.dp)
    }
}

@Composable
private fun BlazeWordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(17.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(AppRed, AppDarkRed)))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Blaze",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp
        )
        Text(
            text = "Movies",
            color = AppRed,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserWebView(
    url: String,
    onOpenAppLink: (String) -> Unit,
    onProgress: (Int) -> Unit,
    onLoading: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val openAppLink = rememberUpdatedState(onOpenAppLink)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                overScrollMode = View.OVER_SCROLL_NEVER

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress)
                        if (newProgress >= 100) onLoading(false)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: android.graphics.Bitmap?
                    ) {
                        onLoading(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onProgress(100)
                        onLoading(false)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val link = request?.url?.toString() ?: return false
                        return when {
                            link.startsWith("app://", ignoreCase = true) -> {
                                openAppLink.value(link)
                                true
                            }
                            link.startsWith("http", ignoreCase = true) -> false
                            link.startsWith("mailto:", ignoreCase = true) ||
                                link.startsWith("tel:", ignoreCase = true) ||
                                link.startsWith("intent:", ignoreCase = true) -> {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                }
                                true
                            }
                            else -> false
                        }
                    }
                }

                loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        }
    )
}