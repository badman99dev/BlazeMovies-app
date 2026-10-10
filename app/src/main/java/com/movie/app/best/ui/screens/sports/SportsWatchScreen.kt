package com.movie.app.best.ui.screens.sports

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.screens.player.PlayerFactory
import com.movie.app.best.util.NetworkUtils
import com.movie.app.best.ui.components.TeamFlagBadge
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.InfoBlue
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.util.ClearKeyHelper
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode
import com.movie.app.best.ui.screens.sports.components.CricketScoreCard

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
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var consecutiveSegmentErrors by remember { mutableStateOf(0) }
    var isSyncingLive by remember { mutableStateOf(false) }
    var hasInitialLiveSynced by remember { mutableStateOf(false) }

    val animatedAspect by animateFloatAsState(
        targetValue = videoAspect,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "sports_player_aspect"
    )

    val trackSelector = remember {
        PlayerFactory.trackSelector(context, disableSubtitles = false)
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
                ImmersiveMode.keepScreenOn(act, false)
                ImmersiveMode.exit(act)
            }
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }
    DisposableEffect(connectivityManager) {
        val cm = connectivityManager ?: return@DisposableEffect onDispose {}
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post {
                    exoPlayer?.let { player ->
                        if (player.playbackState == Player.STATE_IDLE) {
                            try {
                                val dur = player.duration
                                if (dur > 6000L) player.seekTo(dur - 6000L) else player.seekToDefaultPosition()
                                player.prepare()
                                player.play()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}
        onDispose {
            try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
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

    LaunchedEffect(state.playToken, state.currentStream?.url) {
        val stream = state.currentStream
        if (stream == null || stream.url.isBlank()) {
            exoPlayer?.release()
            exoPlayer = null
            activity?.let { ImmersiveMode.keepScreenOn(it, false) }
            return@LaunchedEffect
        }

        exoPlayer?.release()
        isPlayerBuffering = true
        videoAspect = 16f / 9f
        consecutiveSegmentErrors = 0
        isSyncingLive = false
        hasInitialLiveSynced = false
        activity?.let { ImmersiveMode.keepScreenOn(it, false) }

        val newPlayer = PlayerFactory.build(
            context = context,
            trackSelector = trackSelector,
            backBufferMs = PlayerFactory.SPORTS_BACK_BUFFER_MS
        )

        newPlayer.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                // 🚀 Pre-download intercept: Direct -8s start before ExoPlayer requests -30s segment!
                if (!timeline.isEmpty && !hasInitialLiveSynced) {
                    val window = androidx.media3.common.Timeline.Window()
                    timeline.getWindow(0, window)
                    if (window.isLive()) {
                        hasInitialLiveSynced = true
                        val dur = window.durationMs
                        if (dur > 8000L) {
                            newPlayer.seekTo(dur - 8000L)
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlayerBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    viewModel.onPlaybackReady()
                    // 🟢 Any successful playback state resets consecutive failure streak to 0!
                    consecutiveSegmentErrors = 0
                    isSyncingLive = false
                }
                activity?.let {
                    val playing = newPlayer.isPlaying
                    ImmersiveMode.keepScreenOn(it, playing || playbackState == Player.STATE_BUFFERING)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // 🟢 Playing successfully resets consecutive failure streak to 0!
                    consecutiveSegmentErrors = 0
                    isSyncingLive = false
                }
                activity?.let {
                    val pState = newPlayer.playbackState
                    ImmersiveMode.keepScreenOn(it, isPlaying || pState == Player.STATE_BUFFERING)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // 0. Check if user's device lost internet connection (Wi-Fi off / no data):
                if (NetworkUtils.isDeviceOffline(context, error)) {
                    isSyncingLive = false
                    isPlayerBuffering = true
                    // 🚫 Device is offline -> DO NOT count strikes, DO NOT switch servers!
                    return
                }

                // 1. Check if user fell behind the sliding buffer window (e.g. while paused):
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    consecutiveSegmentErrors = 0 // 🟢 Explicit streak wipe on live sync!
                    isSyncingLive = true
                    val dur = newPlayer.duration
                    if (dur > 6000L) newPlayer.seekTo(dur - 6000L) else newPlayer.seekToDefaultPosition()
                    newPlayer.prepare()
                    newPlayer.play()
                    return
                }

                // 2. Initial dead manifest (manifest could not be loaded at all):
                val isInitialDeadManifest = newPlayer.currentPosition == 0L && newPlayer.playbackState == Player.STATE_IDLE
                if (isInitialDeadManifest) {
                    isPlayerBuffering = false
                    consecutiveSegmentErrors = 0
                    isSyncingLive = false
                    viewModel.onPlaybackError()
                    return
                }

                // 3. Segment / chunk failure on ongoing stream:
                consecutiveSegmentErrors++
                if (consecutiveSegmentErrors < 3) {
                    // Strike 1 or 2: Attempt auto-resync to live edge
                    isSyncingLive = true
                    isPlayerBuffering = true
                    val dur = newPlayer.duration
                    if (dur > 6000L) newPlayer.seekTo(dur - 6000L) else newPlayer.seekToDefaultPosition()
                    newPlayer.prepare()
                    newPlayer.play()
                } else {
                    // 🔴 Strike 3: 3 CONSECUTIVE FAILURES -> Zombie Server declared!
                    consecutiveSegmentErrors = 0
                    isSyncingLive = false
                    isPlayerBuffering = false
                    viewModel.onPlaybackError()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    videoAspect = ratio.coerceIn(1.33f, 2.45f)
                }
            }
        })

        if (newPlayer.videoSize.width > 0 && newPlayer.videoSize.height > 0) {
            val ratio = newPlayer.videoSize.width.toFloat() / newPlayer.videoSize.height.toFloat()
            videoAspect = ratio.coerceIn(1.33f, 2.45f)
        }

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
    val isLive = event?.isLive == true
    val isUpcoming = event?.isUpcoming == true
    val isEnded = event != null && !isLive && !isUpcoming

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isFullscreen) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLive && exoPlayer != null) {
                    MediaPlayerScreen(
                        player = exoPlayer,
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

                    ServerSwitchingBanner(
                        visible = state.isSwitchingServer && isLive && !state.isLoading,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    LiveSyncingBanner(
                        visible = isSyncingLive && !state.isSwitchingServer && isLive && !state.isLoading,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                    )
                } else if (isUpcoming) {
                    UpcomingMatchPlayerOverlay(
                        event = event,
                        timeTick = state.timeTick,
                        onBackClick = { exitFullscreen() }
                    )
                } else if (state.error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Stream connection error",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
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
                } else {
                    EndedMatchPlayerOverlay(
                        event = event,
                        onBackClick = { exitFullscreen() }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Color.Black)
                )
                // ── 16:9 Player / Custom Overlay Container ─────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(animatedAspect)
                        .background(Color.Black)
                ) {
                    when {
                        // 1. Loading State: Native player buffering animation
                        state.isLoading || event == null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(64.dp),
                                    strokeWidth = 3.5.dp
                                )
                            }
                        }

                        // 2. Upcoming Match: In-player countdown overlay (no dead streams)
                        isUpcoming -> {
                            UpcomingMatchPlayerOverlay(
                                event = event,
                                timeTick = state.timeTick,
                                onBackClick = onBackClick
                            )
                        }

                        // 3. Ended Match: Concluded notice overlay (ignore dead streams)
                        isEnded -> {
                            EndedMatchPlayerOverlay(
                                event = event,
                                onBackClick = onBackClick
                            )
                        }

                        // 4. Live Match: Active ExoPlayer stream
                        isLive && exoPlayer != null && state.currentStream != null -> {
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

                        // 5. All servers failed / stream error
                        state.error != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.92f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.error ?: "Stream connection error",
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

                        // 6. Connecting to Live Stream
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(64.dp),
                                    strokeWidth = 3.5.dp
                                )
                            }
                        }
                    }

                    // Auto-failover banner: server failed, trying next one
                    ServerSwitchingBanner(
                        visible = state.isSwitchingServer && isLive && !state.isLoading,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    LiveSyncingBanner(
                        visible = isSyncingLive && !state.isSwitchingServer && isLive && !state.isLoading,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                ) {
                    AnimatedVisibility(
                        visible = !state.isLoading && event != null,
                        enter = fadeIn(animationSpec = tween(350)),
                        exit = fadeOut(animationSpec = tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
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

                            if (displayTitle.isNotBlank()) {
                                Text(
                                    text = displayTitle,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            if (state.isCricket) {
                                CricketScoreCard(
                                    scoreData = state.cricketScore,
                                    isLoading = state.isCricketLoading,
                                    syncWithStream = state.syncWithStream,
                                    onToggleStreamSync = { viewModel.toggleStreamSync(it) }
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

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
                                        when {
                                            isLive -> {
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
                                            isUpcoming -> {
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
                                            }
                                            else -> {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "ENDED",
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
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
                                            text = event.getNotchText(state.timeTick),
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
                            } else if (isEnded) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardDark),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Broadcast Concluded",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "This match has ended. Live streaming is no longer active.",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.5.sp
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
private fun UpcomingMatchPlayerOverlay(
    event: com.movie.app.best.data.model.SportEvent?,
    timeTick: Long,
    onBackClick: () -> Unit
) {
    val info = event?.eventInfo
    val countdownStr = event?.getNotchText(timeTick) ?: "Match Starting Soon"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF070B14),
                        Color.Black
                    )
                )
            )
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        val league = listOfNotNull(
            info?.eventCat?.takeIf { it.isNotBlank() },
            info?.eventName?.takeIf { it.isNotBlank() }
        ).joinToString(" • ").uppercase()

        if (league.isNotBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = league,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = info?.teamA ?: (event?.title ?: "Team A"),
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TeamFlagBadge(flagUrl = info?.teamAFlag, size = 36.dp, borderWidth = 1.2.dp)
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TeamFlagBadge(flagUrl = info?.teamBFlag, size = 36.dp, borderWidth = 1.2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = info?.teamB ?: "Team B",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(InfoBlue.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .border(1.dp, InfoBlue.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = InfoBlue,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = countdownStr,
                    color = InfoBlue,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                )
            }

            if (!event?.formattedStartTime.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Scheduled: ${event?.formattedStartTime}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Live streaming will commence automatically when the match starts",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EndedMatchPlayerOverlay(
    event: com.movie.app.best.data.model.SportEvent?,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1414),
                        Color(0xFF0F0B0B),
                        Color.Black
                    )
                )
            )
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .border(1.2.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "BROADCAST CONCLUDED",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This Match Has Ended",
                color = Color.White,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "The live broadcast for this event has finished and streaming is no longer active.",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            if (!event?.formattedStartTime.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Event Time: ${event?.formattedStartTime}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

@Composable
private fun ServerSwitchingBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Switching server...",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LiveSyncingBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(50))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Syncing with live stream...",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}