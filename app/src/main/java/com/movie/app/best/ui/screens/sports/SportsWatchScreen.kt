package com.movie.app.best.ui.screens.sports

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import coil.compose.AsyncImage
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.SportStream
import com.movie.app.best.data.settings.VideoQualitySettings
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.util.ClearKeyHelper
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode

@Composable
fun SportsWatchScreen(
    onBackClick: () -> Unit,
    viewModel: SportsWatchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlayerBuffering by remember { mutableStateOf(true) }
    var playerErrorMsg by remember { mutableStateOf<String?>(null) }

    // Track selector
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(VideoQualitySettings.applyTo(buildUponParameters()).build())
        }
    }

    // Fullscreen and Orientation Handling
    LaunchedEffect(isFullscreen) {
        FullscreenPlayerState.isActive = isFullscreen
        activity?.let { act ->
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                ImmersiveMode.enter(act)
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ImmersiveMode.exit(act)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            FullscreenPlayerState.isActive = false
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ImmersiveMode.exit(act)
            }
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Stream Setup & Player Initialization
    LaunchedEffect(state.currentStream?.url) {
        val stream = state.currentStream
        if (stream == null || stream.url.isBlank()) {
            exoPlayer?.release()
            exoPlayer = null
            return@LaunchedEffect
        }

        exoPlayer?.release()
        isPlayerBuffering = true
        playerErrorMsg = null

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                3000,
                30000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setBackBuffer(30000, true)
            .build()

        val newPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        newPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlayerBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isPlayerBuffering = false
                playerErrorMsg = "Playback error: ${error.message ?: "Failed to play this server stream"}"
            }
        })

        val mediaSource = ClearKeyHelper.createMediaSource(
            url = stream.url,
            headers = stream.headers,
            drmKey = if (stream.hasClearKey) stream.drm?.key else null,
            isDash = stream.isDash,
            isHls = stream.isHls
        )

        newPlayer.setMediaSource(mediaSource)
        newPlayer.prepare()
        newPlayer.playWhenReady = true
        exoPlayer = newPlayer
    }

    val displayTitle = if (state.teamA.isNotBlank() && state.teamB.isNotBlank()) {
        "${state.teamA} vs ${state.teamB}"
    } else {
        state.title.ifBlank { "Live Sports" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isFullscreen) {
            // Fullscreen Landscape Video Player
            Box(modifier = Modifier.fillMaxSize()) {
                exoPlayer?.let { player ->
                    MediaPlayerScreen(
                        player = player,
                        title = displayTitle,
                        isInline = false,
                        isLive = true,
                        serverOptions = state.playbackOptions,
                        selectedServerOptionId = state.selectedOptionId,
                        onServerOptionSelected = { viewModel.selectServer(it) },
                        onFullscreenClick = { isFullscreen = false },
                        onBackClick = { isFullscreen = false }
                    )
                }
            }
        } else {
            // Portrait Layout: 16:9 Player on Top + Details below
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top 16:9 Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    if (exoPlayer != null && state.currentStream != null) {
                        MediaPlayerScreen(
                            player = exoPlayer,
                            title = displayTitle,
                            isInline = true,
                            isLive = true,
                            serverOptions = state.playbackOptions,
                            selectedServerOptionId = state.selectedOptionId,
                            onServerOptionSelected = { viewModel.selectServer(it) },
                            onFullscreenClick = { isFullscreen = true },
                            onBackClick = onBackClick
                        )
                    }

                    // Back button overlay when controls hidden
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Loading / Error Overlay
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AppRed, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Connecting to sports server...", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    } else if (playerErrorMsg != null || state.error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    playerErrorMsg ?: state.error ?: "Stream error",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loadStreams() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Stream", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Scrollable Match Info & Servers List Below Player
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0E111A))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Match Category & League
                    val catText = listOfNotNull(
                        state.eventCat.takeIf { it.isNotBlank() },
                        state.eventName.takeIf { it.isNotBlank() }
                    ).joinToString(" | ").uppercase()

                    if (catText.isNotBlank()) {
                        Text(
                            text = catText,
                            color = Color(0xFFFF5238),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Main Match Title
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Teams Match Card (Team A vs Team B)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Team A
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                WatchTeamFlag(flagUrl = state.teamAFlag)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.teamA.ifBlank { "Team A" },
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }

                            // VS / Live Badge Center
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                val pulse = rememberInfiniteTransition(label = "pulse")
                                val alpha by pulse.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseAlpha"
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFE50914).copy(alpha = 0.15f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(Color(0xFFE50914).copy(alpha = alpha), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "LIVE",
                                        color = Color(0xFFE50914),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VS",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Team B
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                WatchTeamFlag(flagUrl = state.teamBFlag)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.teamB.ifBlank { "Team B" },
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Streaming Servers Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = Color(0xFFFF5238),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Streaming Servers (${state.streams.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Server Selection Horizontal Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.playbackOptions) { option ->
                            val isSelected = option.id == state.selectedOptionId
                            val idx = state.playbackOptions.indexOf(option)
                            val correspondingStream = state.streams.getOrNull(idx)

                            ServerChip(
                                option = option,
                                stream = correspondingStream,
                                isSelected = isSelected,
                                onClick = { viewModel.selectServer(option.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Active Stream Security & Technical Details Card
                    state.currentStream?.let { cur ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141724)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Current Stream Information",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Format", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                    Text(
                                        text = (cur.format ?: if (cur.isDash) "DASH" else "HLS").uppercase(),
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (cur.hasClearKey) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Protection", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                        Text(
                                            "ClearKey DRM (On-Device Decryption)",
                                            color = Color(0xFF10B981),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchTeamFlag(flagUrl: String?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF222636))
            .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!flagUrl.isNullOrBlank()) {
            AsyncImage(
                model = flagUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ServerChip(
    option: PlaybackOption,
    stream: SportStream?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF2E1917) else Color(0xFF181C2B)
    val border = if (isSelected) Color(0xFFFF5238) else Color.White.copy(alpha = 0.1f)
    val textColor = if (isSelected) Color(0xFFFF5238) else Color.White

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.2.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = option.label,
                color = textColor,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            val tag = if (stream?.isDash == true) "DASH" else "HLS"
            Text(
                text = tag,
                color = if (isSelected) Color(0xFFFF5238).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
