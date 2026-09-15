package com.movie.app.best.ui.screens.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.ServerScanRow

private val ScanYellow = Color(0xFFFFD54A)
private val ScanGreen = Color(0xFF17C964)
private val ScanRed = Color(0xFFE03131)
private val ScanGrey = Color(0xFF7C8899)
private val CardBgTop = Color(0xFF061A0E)
private val CardBgBottom = Color(0xFF020A05)

private fun statusColor(status: String): Color = when (status) {
    "found" -> ScanGreen
    "fail" -> ScanRed
    "na" -> ScanGrey
    else -> ScanYellow
}

/**
 * Live "scanning servers" animation shown over the (blurred) poster while SourceHub + Gemma resolve.
 * resolving → neon-yellow rotating ring; found → green ✓; fail → red ✕; na → grey –.
 * Subtitle flips to "Ready to play" once every row has settled but the default playlist is still loading.
 */
@Composable
fun ServerScanOverlay(
    modifier: Modifier = Modifier,
    title: String,
    rows: List<ServerScanRow>,
    posterUrl: String? = null
) {
    val total = rows.size
    val settled = rows.count { it.status != "resolving" }
    val foundCount = rows.count { it.status == "found" }
    val allSettled = total > 0 && settled == total
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else settled.toFloat() / total.toFloat(),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "scanProgress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Background: blurred poster (fallback to dark radial) + scrim
        if (!posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(26.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC000000), Color(0xE6000000), Color(0xCC000000))
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xFF06210F), Color.Black), radius = 1400f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.ifBlank { "Preparing stream" },
                color = Color(0xFFE8F5EC),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Crossfade(targetState = allSettled, label = "scanSubtitle") { ready ->
                Text(
                    text = if (ready) "Ready to play" else "Scanning high-speed servers...",
                    color = if (ready) ScanGreen else Color(0xFF7F98C9),
                    fontSize = 13.sp,
                    fontWeight = if (ready) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF141A14))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFB8860B), ScanYellow, Color(0xFFFFF3A0))))
                )
                if (settled < total) ShimmerStripe()
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$settled ANALYZED", color = Color(0xFF8FA39A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    text = if (settled < total) "${total - settled} REMAINING" else "DONE · $foundCount FOUND",
                    color = if (settled < total) ScanYellow else ScanGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    itemsIndexed(rows, key = { _, r -> r.name }) { index, row ->
                        ServerScanCard(row = row, index = index)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Black,
                                0.12f to Color.Transparent,
                                0.88f to Color.Transparent,
                                1f to Color.Black
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ShimmerStripe() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                    startX = x * 800f,
                    endX = x * 800f + 120f
                )
            )
    )
}

@Composable
private fun ServerScanCard(row: ServerScanRow, index: Int) {
    val color = statusColor(row.status)
    val isResolving = row.status == "resolving"

    val pop = remember { Animatable(1f) }
    LaunchedEffect(row.status) {
        if (!isResolving) {
            pop.snapTo(0.2f)
            pop.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse$index")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1100 + index * 90, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow$index"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(CardBgTop, CardBgBottom)))
                .border(1.dp, color.copy(alpha = if (isResolving) 0.55f else 0.30f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp)) {
                if (isResolving) RotatingNeonRing(color = ScanYellow, glow = glow)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(if (isResolving) 1f else pop.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(color.copy(alpha = 0.95f * glow), color.copy(alpha = 0.18f * glow))
                            )
                        )
                        .border(1.2.dp, color.copy(alpha = 0.5f + 0.4f * glow), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (row.status) {
                            "found" -> "\u2713"
                            "fail" -> "\u2715"
                            "na" -> "\u2013"
                            else -> "\u2022"
                        },
                        color = if (row.status == "found") Color(0xFF03210F) else Color.White,
                        fontSize = if (isResolving) 9.sp else 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = row.name.replaceFirstChar { it.uppercase() },
            color = when (row.status) {
                "fail" -> Color(0xFFFFB3B3)
                "na" -> Color(0xFFAEB8C4)
                else -> Color(0xFFBFF0D0)
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = when (row.status) {
                "found" -> if (row.elapsedMs > 0) "${row.elapsedMs / 1000.0}s" else "found"
                "fail" -> row.error?.take(12) ?: "failed"
                "na" -> "n/a"
                else -> "resolving"
            },
            color = color.copy(alpha = 0.75f),
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RotatingNeonRing(color: Color, glow: Float) {
    val t = rememberInfiniteTransition(label = "scanRing")
    val angle by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Restart),
        label = "scanRingAngle"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        rotate(degrees = angle) {
            drawArc(
                color = color.copy(alpha = glow),
                startAngle = 0f,
                sweepAngle = 250f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        rotate(degrees = angle * 1.6f) {
            drawArc(
                color = color.copy(alpha = 0.45f * glow),
                startAngle = 200f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
    }
}
