package com.movie.app.best.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginRequiredDialog(
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit,
    anchorCenter: Offset? = null
) {
    val scope = rememberCoroutineScope()
    val dialogScale = remember { Animatable(0.88f) }
    val dialogAlpha = remember { Animatable(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val view = LocalView.current
    DisposableEffect(Unit) {
        runCatching {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        while (boxSize == IntSize.Zero) {
            withFrameNanos { }
        }
        coroutineScope {
            launch { dialogScale.animateTo(1f, tween(320)) }
            launch { dialogAlpha.animateTo(1f, tween(220)) }
        }
    }

    fun dismiss(callback: () -> Unit) {
        scope.launch {
            coroutineScope {
                launch { dialogScale.animateTo(0.88f, tween(180)) }
                launch { dialogAlpha.animateTo(0f, tween(180)) }
            }
            delay(40)
            callback()
        }
    }

    val computedOrigin = remember(anchorCenter, boxSize) {
        if (anchorCenter == null || boxSize == IntSize.Zero) {
            TransformOrigin.Center
        } else {
            val screenW = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenH = with(density) { configuration.screenHeightDp.dp.toPx() }
            val dialogLeft = (screenW - boxSize.width) / 2f
            val dialogTop = (screenH - boxSize.height) / 2f
            val originX = ((anchorCenter.x - dialogLeft) / boxSize.width).coerceIn(0f, 1f)
            val originY = ((anchorCenter.y - dialogTop) / boxSize.height).coerceIn(0f, 1f)
            TransformOrigin(originX, originY)
        }
    }

    Dialog(
        onDismissRequest = { dismiss(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Deep pure black backdrop with high contrast blur feeling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .graphicsLayer { alpha = dialogAlpha.value },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .onSizeChanged { boxSize = it }
                    .graphicsLayer {
                        scaleX = dialogScale.value
                        scaleY = dialogScale.value
                        alpha = dialogAlpha.value
                        transformOrigin = computedOrigin
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF050507))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFFFF2E93).copy(alpha = 0.5f),
                                Color(0xFFE50914).copy(alpha = 0.3f),
                                Color(0xFF1E1E24)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    
                    // Top Concept Header Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF2E93))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "MEMBER PRIVILEGE",
                                color = Color(0xFFFF4D8D),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF121217))
                                .border(1.dp, Color(0xFF262630), RoundedCornerShape(20.dp))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "100% FREE",
                                color = Color(0xFF00E676),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 3D HOLOGRAPHIC VIP ACCESS CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF26050C),
                                        Color(0xFF10070B),
                                        Color(0xFF0B0612),
                                        Color(0xFF170007)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF2E93),
                                        Color(0xFFE50914).copy(alpha = 0.5f),
                                        Color(0xFF3B1528)
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFE50914),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "BLAZE ACCESS",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.8.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE50914).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "All-Access Pass",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Sign in once to unlock cloud sync, direct stream requests & community features.",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TIER: VIP MEMBER",
                                    color = Color(0xFFFF4D8D),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NO ADS • NO SUB",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // MODERN FEATURE PILLS (Cluster tags)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VipFeatureChip(icon = Icons.Default.SmartDisplay, label = "Request Streams")
                        VipFeatureChip(icon = Icons.Default.Favorite, label = "Sync Likes")
                        VipFeatureChip(icon = Icons.Default.BookmarkAdd, label = "Watchlist")
                        VipFeatureChip(icon = Icons.Default.ChatBubble, label = "Comments")
                        VipFeatureChip(icon = Icons.Default.CloudSync, label = "Multi-Device")
                        VipFeatureChip(icon = Icons.Default.Flag, label = "Report Issues")
                    }

                    Spacer(Modifier.height(20.dp))

                    // ACTION BUTTON: High-End Gradient VIP CTA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color(0xFFE50914),
                                spotColor = Color(0xFFFF2E93)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFE50914),
                                        Color(0xFFFF2E93)
                                    )
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { dismiss(onLoginClick) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Claim Free Pass →",
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    TextButton(onClick = { dismiss(onDismiss) }) {
                        Text(
                            text = "Continue as Guest",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VipFeatureChip(icon: ImageVector, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0F14))
            .border(1.dp, Color(0xFF1F1F2A), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF4D8D),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = Color(0xFFEDEDED),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
