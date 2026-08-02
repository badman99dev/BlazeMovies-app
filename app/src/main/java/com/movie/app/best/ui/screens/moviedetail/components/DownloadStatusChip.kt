package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.DownloadPhase
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.ui.theme.AppRed
import java.io.File

@Composable
fun DownloadStatusChip(
    phase: DownloadPhase,
    progress: Int,
    isZip: Boolean,
    failureReason: String?,
    posterPath: String = "",
    title: String = "",
    onPlay: () -> Unit,
    onDismiss: () -> Unit = {},
    onGoToDownloads: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = phase != DownloadPhase.NONE,
        enter = slideInVertically(tween(300), initialOffsetY = { it / 4 }) + fadeIn(tween(250)),
        exit = slideOutVertically(tween(200), targetOffsetY = { it / 4 }) + fadeOut(tween(150))
    ) {
        val accentColor = when (phase) {
            DownloadPhase.COMPLETE -> SuccessGreen
            DownloadPhase.FAILED -> AppRed
            DownloadPhase.CANCELLED -> Color(0xFFFB923C)
            else -> if (isZip) Color(0xFFB388FF) else SuccessGreen
        }

        val infiniteTransition = rememberInfiniteTransition(label = "chip")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "pulse"
        )

        val statusTitle = when (phase) {
            DownloadPhase.INITIALIZING -> "Initializing..."
            DownloadPhase.DOWNLOADING -> if (isZip) "Downloading ZIP... $progress%" else "Downloading... $progress%"
            DownloadPhase.EXTRACTING -> "Extracting episodes..."
            DownloadPhase.COMPLETE -> if (isZip) "Ready to Play!" else "Download Complete!"
            DownloadPhase.CANCELLED -> "Download Cancelled"
            DownloadPhase.FAILED -> "Download Failed"
            else -> ""
        }

        val statusSubtitle = when (phase) {
            DownloadPhase.INITIALIZING -> "Preparing download"
            DownloadPhase.DOWNLOADING -> if (isZip) "ZIP pack downloading" else "File downloading in background"
            DownloadPhase.EXTRACTING -> "Unpacking episodes from archive"
            DownloadPhase.COMPLETE -> if (isZip) "Episodes extracted & ready" else "Saved to Downloads/BlazeMovies"
            DownloadPhase.CANCELLED -> "Download was cancelled"
            DownloadPhase.FAILED -> failureReason ?: "An error occurred"
            else -> ""
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f),
                            accentColor.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = pulseAlpha * 0.5f),
                            accentColor.copy(alpha = 0.15f),
                            accentColor.copy(alpha = pulseAlpha * 0.5f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (posterPath.isNotEmpty() && File(posterPath).exists()) {
                        AsyncImage(
                            model = File(posterPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp, 60.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp, 60.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isZip) Icons.Default.FolderZip else Icons.Default.Download,
                                null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (title.isNotEmpty()) title else statusTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = statusTitle,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = statusSubtitle,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }

                    when (phase) {
                        DownloadPhase.COMPLETE -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onPlay
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Play",
                                        color = accentColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        DownloadPhase.FAILED, DownloadPhase.CANCELLED -> {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onDismiss
                                    )
                            )
                        }
                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentColor,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }

                if (phase == DownloadPhase.DOWNLOADING && progress > 0) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }

                if (phase == DownloadPhase.EXTRACTING) {
                    Spacer(Modifier.height(10.dp))
                    val shimmerOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                        label = "extractShimmer"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(shimmerOffset)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.3f),
                                            accentColor.copy(alpha = 0.8f),
                                            accentColor.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                    }
                }

                val showGoToDownloads = phase == DownloadPhase.COMPLETE ||
                    phase == DownloadPhase.DOWNLOADING ||
                    phase == DownloadPhase.EXTRACTING ||
                    phase == DownloadPhase.INITIALIZING

                if (showGoToDownloads) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onGoToDownloads
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Go to Downloads",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = accentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
