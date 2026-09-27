package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.remote.StreamRequestApiResponse
import com.movie.app.best.util.AppSoundHelper
import kotlinx.coroutines.launch

@Composable
fun DetailActionButtons(
    hasStream: Boolean,
    streamRequested: Boolean,
    isInMyList: Boolean,
    isLiked: Boolean,
    hasDownloadLinks: Boolean = true,
    isSeries: Boolean = false,
    alwaysShowWatch: Boolean = false,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onMyListClick: () -> Unit,
    onLikeClick: () -> Unit,
    onRequestStream: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasStream || isSeries || alwaysShowWatch) {
            val playBg = Brush.linearGradient(colors = listOf(Color(0xFFE50914), Color(0xFFB71C1C)))
            val playBorder = Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252)))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(playBg)
                    .border(width = 1.dp, brush = playBorder, shape = RoundedCornerShape(24.dp))
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        } else {
            val capsuleBg = if (streamRequested) {
                Brush.linearGradient(colors = listOf(Color(0xFF1B5E20).copy(alpha = 0.5f), Color(0xFF2E7D32).copy(alpha = 0.3f)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFFE50914), Color(0xFFB71C1C)))
            }
            val borderBrush = if (streamRequested) {
                Brush.linearGradient(colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.6f), Color(0xFF81C784).copy(alpha = 0.3f)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252)))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(capsuleBg)
                    .border(
                        width = if (!streamRequested) 1.dp else 0.5.dp,
                        brush = borderBrush,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(enabled = !streamRequested) { onRequestStream() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (streamRequested) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp), tint = Color(0xFF81C784))
                    } else {
                        Icon(Icons.Default.SmartDisplay, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (streamRequested) "Requested ✓" else "Request Stream",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (streamRequested) Color(0xFFA5D6A7) else Color.White
                    )
                }
            }
        }

        ActionIconButton(
            icon = Icons.Default.Download,
            label = "Download",
            tint = if (hasDownloadLinks) Color.White else Color.Gray.copy(alpha = 0.5f),
            onClick = if (hasDownloadLinks) onDownloadClick else ({})
        )

        AnimatedActionIconButton(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (isLiked) "Liked" else "Like",
            isActive = isLiked,
            activeTint = Color(0xFFFF1744),
            onClick = onLikeClick
        )

        AnimatedActionIconButton(
            icon = if (isInMyList) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
            label = if (isInMyList) "Saved" else "My List",
            isActive = isInMyList,
            activeTint = Color(0xFFE50914),
            onClick = onMyListClick
        )
    }
}

@Composable
private fun AnimatedActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeTint: Color,
    inactiveTint: Color = Color.White.copy(alpha = 0.85f),
    hasBurstEffect: Boolean = true,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val burstScale = remember { Animatable(0.4f) }
    val burstAlpha = remember { Animatable(0f) }

    val tint by animateColorAsState(
        targetValue = if (isActive) activeTint else inactiveTint,
        animationSpec = tween(durationMillis = 200),
        label = "tintColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            // Radial halo burst ring on active transition
            if (burstAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = burstScale.value
                            scaleY = burstScale.value
                            alpha = burstAlpha.value
                        }
                        .border(
                            width = 2.dp,
                            color = activeTint,
                            shape = CircleShape
                        )
                )
            }

            IconButton(
                onClick = {
                    val willBeActive = !isActive
                    // 1. Instantaneous low-latency Facebook pop sound
                    AppSoundHelper.playPopSound(context, willBeActive)
                    // 2. Tactile haptic feedback
                    AppSoundHelper.triggerHapticFeedback(context, willBeActive)
                    // 3. Crisp Spring Pop animation
                    scope.launch {
                        scale.animateTo(0.72f, tween(70, easing = FastOutSlowInEasing))
                        launch {
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                        if (hasBurstEffect && willBeActive) {
                            burstScale.snapTo(0.4f)
                            burstAlpha.snapTo(0.85f)
                            launch {
                                burstScale.animateTo(1.75f, tween(320, easing = FastOutSlowInEasing))
                            }
                            launch {
                                burstAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                            }
                        }
                    }
                    onClick()
                },
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Text(
            text = label,
            color = tint.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            text = label,
            color = tint.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StreamRequestWaitingPopup() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.95f), RoundedCornerShape(20.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SmartDisplay,
                contentDescription = null,
                tint = Color(0xFFE50914),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Submitting request...",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Please wait a moment...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(
                color = Color(0xFFE50914),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun StreamRequestResultModal(
    result: StreamRequestApiResponse,
    isModerator: Boolean = false,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.97f), RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (result.already_requested) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(40.dp)
                )
            } else if (result.has_stream) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    result.already_requested -> "Already Requested"
                    result.has_stream -> "Stream Available!"
                    else -> "Request Submitted!"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when {
                    result.already_requested -> "You have already submitted a stream request for this content."
                    result.has_stream -> "This content already has a stream available. Hit Play Now!"
                    result.tier == "vip_user" && result.skynet?.get("triggered") == true -> "VIP request submitted! Processing has been triggered. 🚀"
                    result.tier == "vip_user" -> "VIP request submitted! Processing trigger failed — vote registered."
                    result.tier == "moderator" && result.skynet?.get("triggered") == true -> "Moderator request submitted! Force processing triggered. 🛡️"
                    result.tier == "moderator" -> "Moderator request submitted! Force trigger encountered an issue."
                    else -> "Stream request submitted! We will notify you when ready."
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 19.sp
            )

            if (!result.already_requested && !result.has_stream) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF9800))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${result.request_count} requests so far",
                        color = Color(0xFFFF9800),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isModerator && result.skynet != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Skynet Response",
                    color = Color(0xFF7C4DFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val httpCode = result.skynet["http_code"]
                        val triggered = result.skynet["triggered"]
                        val error = result.skynet["error"]

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Triggered:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            Text(
                                if (triggered == true) "✅ Yes" else "❌ No",
                                color = if (triggered == true) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        if (httpCode != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("HTTP:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text("$httpCode", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }
                        if (error != null) {
                            Spacer(Modifier.height(4.dp))
                            Text("Error: $error", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        }

                        val response = result.skynet["response"]
                        if (response is Map<*, *>) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = com.google.gson.Gson().toJson(response),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                maxLines = 10
                            )
                        }
                    }
                }

                if (result.skynet_debug.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    result.skynet_debug.forEach { dbg ->
                        Text(
                            text = "→ $dbg",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

