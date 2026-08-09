package com.movie.app.best.ui.util

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.ui.theme.AccentPurple
import com.movie.app.best.util.cf.CfBypassController
import com.movie.app.best.util.cf.ModuleResult
import com.movie.app.best.util.findActivity

@Composable
fun CfBypassHost(
    bypassUrl: String?,
    onLog: (String) -> Unit,
    onSolved: (ModuleResult) -> Unit,
    onFailed: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(bypassUrl) {
        val url = bypassUrl ?: return@LaunchedEffect
        if (activity == null) {
            onLog("❌ No Activity host — cannot run Cloudflare solver")
            onFailed()
            return@LaunchedEffect
        }
        if (url.isBlank()) {
            onFailed()
            return@LaunchedEffect
        }
        onLog("🛡 Cloudflare challenge detected — bypassing…")
        val controller = CfBypassController(activity as Activity)
        val result = controller.solveAndExtractDirectUrl(url) { line -> onLog(line) }
        if (result != null) onSolved(result) else onFailed()
    }
}

@Composable
fun BypassingCloudflarePopup(logs: List<String>) {
    val infiniteTransition = rememberInfiniteTransition(label = "cf")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "cfPulse"
    )
    val accent = AccentPurple
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    val latest = logs.lastOrNull() ?: "Bypassing Cloudflare…"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = pulseAlpha * 0.6f),
                            accent.copy(alpha = 0.2f),
                            accent.copy(alpha = pulseAlpha * 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = accent.copy(alpha = pulseAlpha),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bypassing Cloudflare…",
                        color = accent.copy(alpha = pulseAlpha),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = latest,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = accent,
                    strokeWidth = 2.5.dp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (logs.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF050507))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { line ->
                    Text(
                        text = line,
                        color = cfLogColor(line),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun cfLogColor(line: String): Color = when {
    line.contains("❌") || line.contains("fail", true) -> Color(0xFFEF4444)
    line.contains("✅") || line.contains("acquired", true) -> Color(0xFF22C55E)
    line.contains("⚠️") -> Color(0xFFEAB308)
    else -> Color(0xFF71717A)
}
