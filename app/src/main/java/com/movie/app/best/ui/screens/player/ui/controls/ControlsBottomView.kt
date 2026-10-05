package com.movie.app.best.ui.screens.player.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.buttons.PlayerButton
import com.movie.app.best.ui.screens.player.extensions.nameRes
import com.movie.app.best.ui.screens.player.extensions.noRippleClickable
import com.movie.app.best.ui.screens.player.model.VideoContentScale
import com.movie.app.best.ui.screens.player.state.MediaPresentationState
import com.movie.app.best.ui.screens.player.state.durationFormatted
import com.movie.app.best.ui.screens.player.state.positionFormatted
import com.movie.app.best.ui.screens.player.state.pendingPositionFormatted

@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    player: Player,
    mediaPresentationState: MediaPresentationState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    isPipSupported: Boolean,
    isInline: Boolean = false,
    isLive: Boolean = false,
    isRotationLocked: Boolean,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isEffectivelyFullscreen = !isInline || isLandscape || com.movie.app.best.util.FullscreenPlayerState.isActive

    if (isInline) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                var showPendingPosition by rememberSaveable { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .noRippleClickable { if (!isLive) showPendingPosition = !showPendingPosition },
                ) {
                    if (isLive) {
                        LiveBadge(
                            liveOffsetMs = mediaPresentationState.liveOffsetMs,
                            onCatchUpToLive = {
                                val dur = player.duration.coerceAtLeast(0L)
                                val target = if (dur > 6000L) dur - 6000L else dur
                                if (target > 0L) player.seekTo(target)
                            },
                            isSmall = true,
                        )
                    } else {
                        Text(
                            text = when (showPendingPosition) {
                                true -> "-${mediaPresentationState.pendingPositionFormatted}"
                                false -> "${mediaPresentationState.positionFormatted} / ${mediaPresentationState.durationFormatted}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onFullscreenClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isEffectivelyFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isEffectivelyFullscreen) "Exit Fullscreen" else "Fullscreen",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            CustomSeekbar(
                position = if (isLive) (mediaPresentationState.duration - mediaPresentationState.liveOffsetMs).toFloat().coerceAtLeast(0f) else mediaPresentationState.position.toFloat(),
                duration = mediaPresentationState.duration.toFloat(),
                onSeek = { onSeek(it.toLong()) },
                onSeekEnd = { onSeekEnd() },
                isLive = isLive,
                barHeight = if (isLive) 12.dp else 16.dp,
            )
        }
    } else {
        val bottomPad = if (systemBarsPadding.calculateBottomPadding() == 0.dp) 16.dp else systemBarsPadding.calculateBottomPadding()
        Column(
            modifier = modifier
                .padding(start = systemBarsPadding.calculateLeftPadding(layoutDirection), end = systemBarsPadding.calculateRightPadding(layoutDirection), top = 0.dp, bottom = bottomPad)
                .padding(horizontal = 8.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var showPendingPosition by rememberSaveable { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.noRippleClickable { if (!isLive) showPendingPosition = !showPendingPosition },
                ) {
                    if (isLive) {
                        LiveBadge(
                            liveOffsetMs = mediaPresentationState.liveOffsetMs,
                            onCatchUpToLive = {
                                val dur = player.duration.coerceAtLeast(0L)
                                val target = if (dur > 6000L) dur - 6000L else dur
                                if (target > 0L) player.seekTo(target)
                            },
                            isSmall = false,
                        )
                    } else {
                        Text(
                            text = when (showPendingPosition) {
                                true -> "-${mediaPresentationState.pendingPositionFormatted}"
                                false -> mediaPresentationState.positionFormatted
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                        Text(text = " / ", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text(
                            text = mediaPresentationState.durationFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                }
            }

            CustomSeekbar(
                position = if (isLive) (mediaPresentationState.duration - mediaPresentationState.liveOffsetMs).toFloat().coerceAtLeast(0f) else mediaPresentationState.position.toFloat(),
                duration = mediaPresentationState.duration.toFloat(),
                onSeek = { onSeek(it.toLong()) },
                onSeekEnd = { onSeekEnd() },
                isLive = isLive,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerButton(onClick = onLockControlsClick) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                }
                PlayerButton(onClick = onRotateClick) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = null,
                        tint = if (isRotationLocked) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                ) {
                    Icon(imageVector = Icons.Default.FitScreen, contentDescription = null)
                }
                Spacer(modifier = Modifier.weight(1f))

                PlayerButton(onClick = onFullscreenClick) {
                    Icon(
                        imageVector = if (isEffectivelyFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isEffectivelyFullscreen) "Exit Fullscreen" else "Fullscreen"
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSeekbar(
    modifier: Modifier = Modifier,
    position: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    isLive: Boolean = false,
    barHeight: Dp = if (isLive) 12.dp else 20.dp,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackHeight = 2.dp
    val hasDvr = isLive && duration >= 90_000f
    val isPureLive = isLive && !hasDvr
    val canSeek = !isLive || hasDvr
    val thumbRadius = if (canSeek && !isPureLive) 6.dp else 0.dp
    val minSafePosition = if (isLive && duration >= 90_000f) 6000f else 0f
    val maxSafePosition = if (isLive && duration > 6000f) (duration - 6000f) else duration
    
    var sliderWidth by rememberSaveable { mutableStateOf(0f) }
    var isDragging by rememberSaveable { mutableStateOf(false) }
    var dragPosition by rememberSaveable { mutableStateOf(0f) }
    
    val currentPosition = if (isDragging) dragPosition else position.coerceIn(minSafePosition, maxSafePosition)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .onGloballyPositioned { coordinates ->
                sliderWidth = coordinates.size.width.toFloat()
            }
            .then(
                if (!canSeek) Modifier else Modifier.pointerInput(canSeek, duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragPosition = ((offset.x / sliderWidth) * duration).coerceIn(minSafePosition, maxSafePosition)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekEnd()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragPosition = ((dragPosition + (dragAmount / sliderWidth) * duration).coerceIn(minSafePosition, maxSafePosition))
                            onSeek(dragPosition)
                        }
                    )
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (sliderWidth <= 0f) return@Canvas
            
            val centerY = size.height / 2
            val trackStrokeWidth = trackHeight.toPx()
            val radius = thumbRadius.toPx()
            
            // Calculate the position of the circle center
            val fraction = when {
                isPureLive -> 1f // Always Full Solid Red Bar for pure live streams!
                duration > 0f -> (currentPosition / duration).coerceIn(0f, 1f)
                else -> 1f
            }
            val circleCenterX = radius + (size.width - 2 * radius) * fraction
            
            if (!isPureLive) {
                // Draw grey track from circle center to end
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(circleCenterX, centerY),
                    end = Offset(size.width - radius, centerY),
                    strokeWidth = trackStrokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            
            // Draw red track from start to circle center (or full width if pure live)
            drawLine(
                color = primaryColor,
                start = Offset(radius, centerY),
                end = Offset(circleCenterX, centerY),
                strokeWidth = trackStrokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            if (canSeek && !isPureLive) {
                // Draw filled circle thumb at the boundary
                drawCircle(
                    color = primaryColor,
                    radius = radius,
                    center = Offset(circleCenterX, centerY)
                )
            }
        }
    }
}

@Composable
fun LiveBadge(
    liveOffsetMs: Long,
    onCatchUpToLive: () -> Unit,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
) {
    val isAtLiveEdge = liveOffsetMs <= 14_000L
    val dotSize = if (isSmall) 6.dp else 8.dp
    val textStyle = if (isSmall) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
    val dotColor = if (isAtLiveEdge) Color(0xFFFF0000) else Color.White.copy(alpha = 0.55f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = !isAtLiveEdge) {
                onCatchUpToLive()
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        if (!isAtLiveEdge) {
            Text(
                text = formatLiveOffset(liveOffsetMs),
                style = textStyle,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .size(dotSize)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(if (isSmall) 4.dp else 6.dp))
        Text(
            text = "LIVE",
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = if (isAtLiveEdge) Color.White else Color.White.copy(alpha = 0.75f),
        )
    }
}

fun formatLiveOffset(offsetMs: Long): String {
    val totalSec = (offsetMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    val hours = minutes / 60L
    return if (hours > 0L) {
        String.format("-%d:%02d:%02d", hours, minutes % 60L, seconds)
    } else {
        String.format("-%02d:%02d", minutes, seconds)
    }
}

private fun DrawScope.drawRoundedRect(offset: Offset, size: Size, color: Color, startCornerRadius: Float, endCornerRadius: Float) {
    val startCorner = CornerRadius(startCornerRadius, startCornerRadius)
    val endCorner = CornerRadius(endCornerRadius, endCornerRadius)
    drawPath(
        path = Path().apply {
            addRoundRect(RoundRect(rect = Rect(Offset(offset.x, 0f), size = Size(size.width, size.height)), topLeft = startCorner, topRight = endCorner, bottomRight = endCorner, bottomLeft = startCorner))
        },
        color = color,
    )
}
