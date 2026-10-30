package com.movie.app.best.ui.screens.livetv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.compose.AsyncImage
import com.movie.app.best.data.config.RemoteConfig
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.settings.PlayerRotationSettings
import com.movie.app.best.ui.components.LoginRequiredDialog
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.screens.player.PlayerFactory
import com.movie.app.best.ui.screens.player.extensions.resetScreenBrightness
import com.movie.app.best.ui.screens.sports.components.LiveChatSheet
import com.movie.app.best.ui.screens.sports.components.LiveChatStrip
import com.movie.app.best.ui.theme.AppBlack
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.SecondaryText
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode
import com.movie.app.best.util.NetworkUtils
import okhttp3.OkHttpClient

private const val MAX_AUTO_RETRIES = 4
private const val AUTO_RETRY_DELAY_MS = 1800L

@Composable
fun LiveTvWatchScreen(
    onBackClick: () -> Unit,
    onLoginRequired: () -> Unit = {},
    onSimilarChannelClick: (UnifiedChannel) -> Unit = {},
    viewModel: LiveTvWatchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val liveChatState by viewModel.liveChatState.collectAsState()
    val chatHandle by viewModel.chatHandle.collectAsState()
    val similarChannels by viewModel.similarChannels.collectAsState()

    var isFullscreen by remember { mutableStateOf(false) }
    var isChatSheetOpen by remember { mutableStateOf(false) }
    var isFullscreenChatOpen by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlayerBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var consecutiveErrors by remember { mutableStateOf(0) }
    var resumeOnReturn by remember { mutableStateOf(false) }

    val animatedAspect by animateFloatAsState(
        targetValue = videoAspect,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "livetv_player_aspect",
    )

    val trackSelector = remember { PlayerFactory.trackSelector(context, disableSubtitles = false) }

    val exitFullscreen = {
        isFullscreen = false
        isFullscreenChatOpen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.let { ImmersiveMode.exit(it) }
    }

    val enterFullscreen = {
        isFullscreen = true
        activity?.requestedOrientation = if (PlayerRotationSettings.isAutoRotate(context)) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        activity?.let { ImmersiveMode.enter(it) }
    }

    LaunchedEffect(isFullscreen) {
        FullscreenPlayerState.isActive = isFullscreen
        activity?.let { act ->
            if (isFullscreen) ImmersiveMode.enter(act) else ImmersiveMode.exit(act)
        }
    }

    BackHandler(enabled = isChatSheetOpen) {
        isChatSheetOpen = false
        viewModel.disconnectChat()
    }

    BackHandler(enabled = isFullscreenChatOpen) {
        isFullscreenChatOpen = false
        viewModel.disconnectChat()
    }

    BackHandler(enabled = !isChatSheetOpen && !isFullscreenChatOpen) {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onBackClick()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            FullscreenPlayerState.isActive = false
            viewModel.disconnectChat()
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                act.resetScreenBrightness()
                ImmersiveMode.keepScreenOn(act, false)
                ImmersiveMode.exit(act)
            }
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Live stream must go silent when the app leaves the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeOnReturn = exoPlayer?.isPlaying == true
                    exoPlayer?.pause()
                }
                Lifecycle.Event.ON_START -> {
                    if (resumeOnReturn) {
                        exoPlayer?.play()
                        resumeOnReturn = false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                            } catch (_: Exception) {
                            }
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
        } catch (_: Exception) {
        }
        onDispose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(viewModel.streamUrl) {
        val url = viewModel.streamUrl
        exoPlayer?.release()
        exoPlayer = null
        if (url.isBlank()) {
            playerError = "Channel stream is unavailable"
            isPlayerBuffering = false
            return@LaunchedEffect
        }

        isPlayerBuffering = true
        playerError = null
        consecutiveErrors = 0
        videoAspect = 16f / 9f
        activity?.let { ImmersiveMode.keepScreenOn(it, false) }

        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to RemoteConfig.playerReferer(),
                    "Accept" to "*/*",
                )
            )
        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpFactory)
        val isHls = url.contains(".m3u8", ignoreCase = true) ||
            url.contains("sparkling-breeze", ignoreCase = true) ||
            url.contains("/?id=", ignoreCase = true)
        val mediaSourceFactory = if (isHls) {
            HlsMediaSource.Factory(dataSourceFactory)
        } else {
            DefaultMediaSourceFactory(dataSourceFactory)
        }

        val player = PlayerFactory.build(
            context = context,
            trackSelector = trackSelector,
            mediaSourceFactory = mediaSourceFactory,
            backBufferMs = PlayerFactory.SPORTS_BACK_BUFFER_MS,
        )

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlayerBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) consecutiveErrors = 0
                activity?.let {
                    val playing = player.isPlaying
                    ImmersiveMode.keepScreenOn(it, playing || playbackState == Player.STATE_BUFFERING)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) consecutiveErrors = 0
                activity?.let {
                    val pState = player.playbackState
                    ImmersiveMode.keepScreenOn(it, isPlaying || pState == Player.STATE_BUFFERING)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (NetworkUtils.isDeviceOffline(context, error)) {
                    isPlayerBuffering = true
                    return
                }
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    consecutiveErrors = 0
                    val dur = player.duration
                    if (dur > 6000L) player.seekTo(dur - 6000L) else player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                    return
                }
                consecutiveErrors++
                if (consecutiveErrors <= MAX_AUTO_RETRIES) {
                    isPlayerBuffering = true
                    mainHandler.postDelayed({
                        try {
                            val dur = player.duration
                            if (dur > 6000L) player.seekTo(dur - 6000L) else player.seekToDefaultPosition()
                            player.prepare()
                            player.play()
                        } catch (_: Exception) {
                        }
                    }, AUTO_RETRY_DELAY_MS)
                } else {
                    consecutiveErrors = 0
                    isPlayerBuffering = false
                    playerError = error.message ?: "Stream connection error"
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    videoAspect = ratio.coerceIn(1.33f, 2.45f)
                }
            }
        })

        player.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(8000L)
                        .build()
                )
                .build()
        )
        player.prepare()
        player.playWhenReady = true
        exoPlayer = player
    }

    val openChat = {
        if (!viewModel.isUserLoggedIn()) {
            showLoginDialog = true
        } else if (isFullscreen) {
            isFullscreenChatOpen = true
            viewModel.connectChat()
        } else {
            isChatSheetOpen = true
            viewModel.connectChat()
        }
        Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
    ) {
        if (isFullscreen) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (exoPlayer != null) {
                    MediaPlayerScreen(
                        player = exoPlayer,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { exitFullscreen() },
                        onPlayInBackgroundClick = {},
                        onFullscreenClick = { exitFullscreen() },
                        isInline = false,
                        isLive = true,
                        title = viewModel.channelName,
                    )
                }
                ChatFab(
                    onClick = openChat,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = isFullscreenChatOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(250)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LiveChatSheet(
                        state = liveChatState,
                        floatingReactions = viewModel.floatingReactions,
                        currentUserName = liveChatState.currentUserName ?: chatHandle,
                        onCloseClick = {
                            isFullscreenChatOpen = false
                            viewModel.disconnectChat()
                        },
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        onSendReaction = { id, emoji -> viewModel.sendChatReaction(id, emoji) },
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Color.Black)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(animatedAspect)
                        .background(Color.Black)
                ) {
                    if (exoPlayer != null) {
                        MediaPlayerScreen(
                            player = exoPlayer,
                            modifier = Modifier.fillMaxSize(),
                            onBackClick = {
                                activity?.let { ImmersiveMode.exit(it) }
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                onBackClick()
                            },
                            onPlayInBackgroundClick = {},
                            onFullscreenClick = { enterFullscreen() },
                            isInline = true,
                            isLive = true,
                            title = viewModel.channelName,
                        )
                    }

                    if (isPlayerBuffering && exoPlayer != null) {
                        CircularProgressIndicator(
                            color = AppRed,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(42.dp)
                        )
                    }

                    if (playerError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.86f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = playerError ?: "Stream connection error",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        playerError = null
                                        consecutiveErrors = 0
                                        isPlayerBuffering = true
                                        exoPlayer?.let { player ->
                                            try {
                                                val dur = player.duration
                                                if (dur > 6000L) player.seekTo(dur - 6000L) else player.seekToDefaultPosition()
                                                player.prepare()
                                                player.play()
                                            } catch (_: Exception) {
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    ChatFab(
                        onClick = openChat,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(AppBlack)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                    ) {
                        LiveChatStrip(
                            userName = chatHandle ?: viewModel.getUserFirstName(),
                            onlineCount = liveChatState.onlineCount,
                            isConnected = liveChatState.isConnected,
                            onClick = openChat,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        ChannelInfoCard(
                            name = viewModel.channelName,
                            logoUrl = viewModel.logoUrl,
                            category = viewModel.category,
                            onFullscreen = { enterFullscreen() },
                            onShare = {
                                val text = "Watch ${viewModel.channelName} live on BlazeMovies"
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(send, "Share channel"))
                                } catch (_: Exception) {
                                }
                            }
                        )

                        if (similarChannels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Similar Channels",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(similarChannels, key = { it.uniqueKey.ifBlank { it.id } }) { channel ->
                                    SimilarChannelCard(
                                        channel = channel,
                                        onClick = { onSimilarChannelClick(channel) }
                                    )
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isChatSheetOpen,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(320, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(250)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200)),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LiveChatSheet(
                            state = liveChatState,
                            floatingReactions = viewModel.floatingReactions,
                            currentUserName = liveChatState.currentUserName ?: chatHandle,
                            onCloseClick = {
                                isChatSheetOpen = false
                                viewModel.disconnectChat()
                            },
                            onSendMessage = { viewModel.sendChatMessage(it) },
                            onSendReaction = { id, emoji -> viewModel.sendChatReaction(id, emoji) },
                        )
                    }
                }
            }
        }
    }

    if (showLoginDialog) {
        LoginRequiredDialog(
            onLoginClick = {
                showLoginDialog = false
                onLoginRequired()
            },
            onDismiss = { showLoginDialog = false }
        )
    }
}

@Composable
private fun ChatFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = "Live chat",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ChannelInfoCard(
    name: String,
    logoUrl: String,
    category: String,
    onFullscreen: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                } else {
                    Text(
                        text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "TV",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .background(AppRed.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AppRed)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "LIVE",
                            color = AppRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (category.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category,
                            color = SecondaryText,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionChip(text = "Fullscreen", icon = Icons.Default.Fullscreen, onClick = onFullscreen)
            ActionChip(text = "Share", icon = Icons.Default.Share, onClick = onShare)
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SimilarChannelCard(
    channel: UnifiedChannel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            } else {
                Text(
                    text = channel.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "TV",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(26.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = channel.name,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
