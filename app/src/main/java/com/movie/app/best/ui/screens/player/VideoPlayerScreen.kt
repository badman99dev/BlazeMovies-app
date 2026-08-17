package com.movie.app.best.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.debug.Zee5False404Interceptor
import com.movie.app.best.data.settings.VideoQualitySettings
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

private const val MAX_AUTO_RETRIES = 5
private const val AUTO_RETRY_DELAY_MS = 2000L

private fun PlaybackException.isTransient(): Boolean = when (errorCode) {
    PlaybackException.ERROR_CODE_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> true
    else -> false
}

@Composable
fun VideoPlayerScreen(
    onBackClick: () -> Unit,
    playerUrl: String,
    streamUrl: String,
    title: String,
    youtubeId: String,
    movieId: String,
    slug: String = "",
    isLive: Boolean = false,
    contentSource: String = "api",
    viewModel: VideoPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        FullscreenPlayerState.isActive = true
        onDispose { FullscreenPlayerState.isActive = false }
    }

    val effectiveUrl = remember(streamUrl, playerUrl) {
        val url = when {
            playerUrl.trim().isNotEmpty() -> playerUrl.trim()
            streamUrl.trim().isNotEmpty() -> streamUrl.trim()
            else -> ""
        }.let { if (it.startsWith("/")) "file://$it" else it }
            .let {
                if (it.startsWith("file://")) {
                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
                } else it
            }
        url
    }

    val isLocalFile = effectiveUrl.startsWith("file://") || effectiveUrl.startsWith("content://")
    val isHls = !isLocalFile && (
        effectiveUrl.contains(".m3u8", ignoreCase = true) ||
        effectiveUrl.contains("sparkling-breeze", ignoreCase = true) ||
        effectiveUrl.contains("/?id=", ignoreCase = true)
    )

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            val params = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            setParameters(VideoQualitySettings.applyTo(params).build())
        }
    }

    val exoPlayer = remember(effectiveUrl) {
        if (effectiveUrl.isEmpty()) return@remember null

        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(Zee5False404Interceptor())
            .addInterceptor(DebugInterceptor())
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://wasmer-jhns970ko-badals-projects-03fab3df.vercel.app/",
                "Accept" to "*/*"
            ))

        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpFactory)
        val mediaSourceFactory = if (isHls)
            HlsMediaSource.Factory(dataSourceFactory)
        else
            DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        5000,
                        30000,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                    )
                    .setBackBuffer(300000, true)
                    .build()
            )
            .build()
            .apply {
                playWhenReady = true
            }
    }

    LaunchedEffect(effectiveUrl) {
        if (effectiveUrl.isNotEmpty() && exoPlayer != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(effectiveUrl))
            exoPlayer.prepare()
        }
        activity?.let { ImmersiveMode.enter(it) }
    }

    val firebaseRepository = remember { com.movie.app.best.data.repository.FirebaseRepository(context) }

    var resumePos by remember { mutableStateOf(0L) }
    var hasResumed by remember { mutableStateOf(false) }

    LaunchedEffect(slug, effectiveUrl) {
        hasResumed = false
        resumePos = 0L
        if (isLocalFile && effectiveUrl.isNotEmpty()) {
            val fileProgress = firebaseRepository.getLocalFileProgress(effectiveUrl, title)
            resumePos = fileProgress?.progressMs ?: 0L
        } else if (slug.isNotEmpty()) {
            val local = firebaseRepository.getLocalProgress(slug)
            resumePos = local?.progressMs ?: 0L
        }
    }

    LaunchedEffect(resumePos) {
        if (resumePos > 0 && !hasResumed && exoPlayer != null) {
            if (exoPlayer.playbackState == Player.STATE_READY) {
                hasResumed = true
                val seekTo = resumePos
                resumePos = 0L
                exoPlayer.seekTo(seekTo)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && resumePos > 0 && !hasResumed) {
                    hasResumed = true
                    val seekTo = resumePos
                    resumePos = 0L
                    exoPlayer?.seekTo(seekTo)
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }

    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        if (isLocalFile && effectiveUrl.isEmpty()) return@LaunchedEffect
        if (!isLocalFile && slug.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(10_000)
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 0 && dur > 0) {
                if (isLocalFile) {
                    firebaseRepository.saveLocalFileProgress(effectiveUrl, title, pos, dur)
                } else {
                    firebaseRepository.updateProgressLocal(slug, pos, dur)
                }
            }
        }
    }

    var playerError by remember { mutableStateOf<String?>(null) }
    var autoRetryCount by remember { mutableStateOf(0) }
    var isAutoRetrying by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (error.isTransient() && autoRetryCount < MAX_AUTO_RETRIES) {
                    autoRetryCount++
                    isAutoRetrying = true
                    coroutineScope.launch {
                        delay(AUTO_RETRY_DELAY_MS)
                        try {
                            exoPlayer?.let { player ->
                                player.setMediaItem(MediaItem.fromUri(effectiveUrl))
                                player.prepare()
                                player.playWhenReady = true
                            }
                        } catch (_: Exception) {}
                    }
                    return
                }

                isAutoRetrying = false
                playerError = error.message ?: "Playback error"
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playerError = null
                    autoRetryCount = 0
                    isAutoRetrying = false
                }
                activity?.let {
                    val state = exoPlayer?.playbackState ?: Player.STATE_IDLE
                    ImmersiveMode.keepScreenOn(it, isPlaying || state == Player.STATE_BUFFERING)
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                activity?.let {
                    val playing = exoPlayer?.isPlaying == true
                    ImmersiveMode.keepScreenOn(it, playing || state == Player.STATE_BUFFERING)
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            activity?.let { ImmersiveMode.keepScreenOn(it, false) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer?.playbackState == Player.STATE_READY) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val pos = exoPlayer?.currentPosition ?: 0
            val dur = exoPlayer?.duration ?: 0
            if (pos > 0 && dur > 0) {
                if (isLocalFile) {
                    firebaseRepository.saveLocalFileProgress(effectiveUrl, title, pos, dur)
                } else if (slug.isNotEmpty()) {
                    firebaseRepository.updateProgressLocal(slug, pos, dur)
                    val repo = firebaseRepository
                    val s = slug
                    Thread {
                        try { runBlocking { repo.updateProgress(s, pos, dur) } } catch (_: Exception) {}
                    }.start()
                }
            }
            exoPlayer?.release()
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    BackHandler {
        activity?.let { ImmersiveMode.exit(it) }
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onBackClick()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (exoPlayer != null && playerError == null) {
            MediaPlayerScreen(
                player = exoPlayer,
                onBackClick = onBackClick,
                onPlayInBackgroundClick = { },
                title = title,
                isLive = isLive,
            )
            if (isAutoRetrying) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(72.dp),
                    color = Color.White,
                )
            }
        } else if (playerError != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = playerError ?: "Unknown error",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    playerError = null
                    autoRetryCount = 0
                    isAutoRetrying = false
                    exoPlayer?.let { player ->
                        player.setMediaItem(MediaItem.fromUri(effectiveUrl))
                        player.prepare()
                        player.playWhenReady = true
                    }
                }) {
                    Text("Retry", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onBackClick) {
                    Text("Exit", color = Color.White)
                }
            }
        }
    }
}
