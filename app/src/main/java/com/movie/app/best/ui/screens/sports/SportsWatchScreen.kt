package com.movie.app.best.ui.screens.sports

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.SportStream
import com.movie.app.best.data.settings.VideoQualitySettings
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.components.TeamFlagBadge
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.InfoBlue
import com.movie.app.best.ui.theme.SuccessGreen
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

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(VideoQualitySettings.applyTo(buildUponParameters()).build())
        }
    }

    val exitFullscreen = {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.let { ImmersiveMode.exit(it) }
    }

    val enterFullscreen = {
        isFullscreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.let { ImmersiveMode.enter(it) }
    }

    LaunchedEffect(isFullscreen) {
        FullscreenPlayerState.isActive = isFullscreen
        activity?.let { act ->
            if (isFullscreen) {
                ImmersiveMode.enter(act)
            } else {
                ImmersiveMode.exit(act)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            FullscreenPlayerState.isActive = false
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ImmersiveMode.exit(act)
            }
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    BackHandler {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onBackClick()
        }
    }

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
                5000,
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

    // ── 404 / NOT FOUND STATE ───────────────────────────
    if (state.isNotFound) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(CardDark, CircleShape)
                        .border(1.5.dp, AppRed.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = AppRed,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Event Unavailable",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.error ?: "This event is not available or has already ended.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Browse Live Sports",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        return
    }

    val displayTitle = state.displayTitle
    val event = state.event
    val info = event?.eventInfo
    val isUpcoming = event?.isUpcoming == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isFullscreen) {
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
                        onFullscreenClick = { exitFullscreen() },
                        onPlayInBackgroundClick = {},
                        onBackClick = { exitFullscreen() }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    if (isUpcoming && exoPlayer == null) {
                        UpcomingCountdownPlayerBanner(
                            event = event,
                            timeTick = state.timeTick
                        )
                    } else if (exoPlayer != null && state.currentStream != null) {
                        MediaPlayerScreen(
                            player = exoPlayer,
                            title = displayTitle,
                            isInline = true,
                            isLive = true,
                            serverOptions = state.playbackOptions,
                            selectedServerOptionId = state.selectedOptionId,
                            onServerOptionSelected = { viewModel.selectServer(it) },
                            onFullscreenClick = { enterFullscreen() },
                            onPlayInBackgroundClick = {},
                            onBackClick = onBackClick
                        )
                    }

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
                    } else if (playerErrorMsg != null && !isUpcoming) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    playerErrorMsg ?: "Stream error",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loadMatchAndStreams() },
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val catText = listOfNotNull(
                        info?.eventCat?.takeIf { it.isNotBlank() },
                        info?.eventName?.takeIf { it.isNotBlank() }
                    ).joinToString(" | ").uppercase()

                    if (catText.isNotBlank()) {
                        Text(
                            text = catText,
                            color = AppRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TeamFlagBadge(flagUrl = info?.teamAFlag, size = 48.dp, borderWidth = 1.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = info?.teamA ?: "Team A",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                if (isUpcoming) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(InfoBlue.copy(alpha = 0.2f), RoundedCornerShape(50))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = InfoBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "UPCOMING",
                                            color = InfoBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                } else {
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
                                            .background(AppRed.copy(alpha = 0.15f), RoundedCornerShape(50))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(AppRed.copy(alpha = alpha), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "LIVE",
                                            color = AppRed,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VS",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TeamFlagBadge(flagUrl = info?.teamBFlag, size = 48.dp, borderWidth = 1.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = info?.teamB ?: "Team B",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    if (isUpcoming && event != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = InfoBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = event.formattedStartTime,
                                        color = InfoBlue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = event.countdownText,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Live streaming will commence automatically when the match starts.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (state.streams.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = AppRed,
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

                        state.currentStream?.let { cur ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardDark),
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
                                            color = InfoBlue,
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
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Protection", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                            }
                                            Text(
                                                "ClearKey DRM (On-Device Decryption)",
                                                color = SuccessGreen,
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
}

@Composable
private fun UpcomingCountdownPlayerBanner(
    event: com.movie.app.best.data.model.SportEvent?,
    timeTick: Long
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(InfoBlue.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = InfoBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MATCH NOT STARTED YET",
                    color = InfoBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = event?.countdownText ?: "Match Starting Soon",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = event?.formattedStartTime ?: "",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
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
    val bg = if (isSelected) CardDark else Color(0xFF111111)
    val border = if (isSelected) AppRed else Color.White.copy(alpha = 0.1f)
    val textColor = if (isSelected) AppRed else Color.White

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
                color = if (isSelected) AppRed.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}