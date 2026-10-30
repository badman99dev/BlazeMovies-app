package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun stripAvatarColors(name: String): Pair<Color, Color> {
    val hash = kotlin.math.abs(name.hashCode())
    val start = when (hash % 3) {
        0 -> Color(0xFF3F3F46)
        1 -> Color(0xFF4A4A52)
        else -> Color(0xFF35353B)
    }
    return start to Color(0xFF1A1A1D)
}

@Composable
fun LiveChatStrip(
    userName: String?,
    onlineCount: Int,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = userName?.ifBlank { null }
    val (avatarStart, avatarEnd) = remember(displayName) { stripAvatarColors(displayName ?: "fan") }
    val initial = displayName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "F"
    val online = isConnected && onlineCount > 0

    val transition = rememberInfiniteTransition(label = "chatStrip")
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0B0B0D))
            .border(1.dp, Color(0xFF1F1F23), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .graphicsLayer {
                                alpha = (1f - halo) * 0.4f
                                scaleX = 0.45f + halo
                                scaleY = 0.45f + halo
                            }
                            .background(Color(0xFFFF3B30), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isConnected) Color(0xFFFF3B30) else Color(0xFF6B7280),
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = "Live chat",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = when {
                    online -> "$onlineCount watching"
                    isConnected -> "Connected"
                    else -> "Tap to join"
                },
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(2.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF121214))
                .border(1.dp, Color(0xFF1F1F23), RoundedCornerShape(22.dp))
                .padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Brush.linearGradient(listOf(avatarStart, avatarEnd)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Text(
                text = displayName?.let { "Chat publicly as @$it" } ?: "Chat publicly with other fans",
                color = Color(0xFF9A9AA0),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xFF1C1C1F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}