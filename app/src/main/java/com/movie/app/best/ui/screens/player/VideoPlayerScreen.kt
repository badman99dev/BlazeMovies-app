package com.movie.app.best.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.movie.app.best.data.settings.LocalPlaybackSettings
import com.movie.app.best.ui.screens.player.engines.EngineRouter
import com.movie.app.best.ui.screens.player.engines.media3.Media3EngineFactory
import com.movie.app.best.ui.screens.player.engines.media3.Media3PlayerEvents
import com.movie.app.best.ui.screens.player.engines.vlc.VlcEngineFactory
import com.movie.app.best.ui.screens.player.extensions.resetScreenBrightness
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        PlayerFactory.trackSelector(context)
    }

    // Local playback ka decoder mode (quality button ki jagah decoder button).
    // Per-file preference: jo last time us file pe chala wahi mode dobara khulta hai.
    // HW = Media3 (battery / pure hardware), HW+ aur SW = libVLC.
    var decoderMode by remember(effectiveUrl) {
        mutableStateOf(
            if (EngineRouter.isLocal(effectiveUrl)) {
                LocalPlaybackSettings.modeFor(context, effectiveUrl)
            } else {
                LocalPlaybackSettings.Mode.HW_PLUS
            }
        )
    }

    // Router: local/downloaded → libVLC "Advance Decoder" (HW+ / HW / SW — demuxer VLC ka,
    // isliye seek har mode me chalta hai), streams → Media3. Both are plain media3 Players,
    // so the rest of this screen and the shared player UI are engine-agnostic.
    val engine: Player? = remember(effectiveUrl, decoderMode) {
        if (effectiveUrl.isEmpty()) return@remember null
        if (EngineRouter.isLocal(effectiveUrl)) {
            VlcEngineFactory.create(context, decoderMode)
        } else {
            Media3EngineFactory.create(
                context = context,
                isHls = isHls,
                trackSelector = trackSelector,
            )
        }
    }

    LaunchedEffect(effectiveUrl, engine) {
        if (effectiveUrl.isNotEmpty() && engine != null) {
            val mediaItem = if (isLive) {
                val liveConfig = MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(8000L)
                    .build()
                MediaItem.Builder()
                    .setUri(effectiveUrl)
                    .setLiveConfiguration(liveConfig)
                    .build()
            } else {
                MediaItem.fromUri(effectiveUrl)
            }
            engine.setMediaItem(mediaItem)
            engine.prepare()
        }
        activity?.let { ImmersiveMode.enter(it) }
    }

    val firebaseRepository = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.movie.app.best.data.repository.FirebaseRepositoryEntryPoint::class.java
        ).firebaseRepository()
    }

    var resumePos by remember { mutableStateOf(0L) }
    var hasResumed by remember { mutableStateOf(false) }
    var hasInitialLiveSynced by remember { mutableStateOf(false) }

    LaunchedEffect(slug, effectiveUrl) {
        hasResumed = false
        hasInitialLiveSynced = false
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
        if (resumePos > 0 && !hasResumed && engine != null) {
            if (engine.playbackState == Player.STATE_READY) {
                hasResumed = true
                val seekTo = resumePos
                resumePos = 0L
                engine.seekTo(seekTo)
            }
        }
    }

    // Resume the saved position once the engine is ready.
    DisposableEffect(engine) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && resumePos > 0 && !hasResumed) {
                    hasResumed = true
                    val seekTo = resumePos
                    resumePos = 0L
                    engine?.seekTo(seekTo)
                }
            }
        }
        engine?.addListener(listener)
        onDispose {
            engine?.removeListener(listener)
        }
    }

    // Media3-only: jump to the live edge on the first live window (see Media3PlayerEvents).
    DisposableEffect(engine, isLive) {
        if (!isLive) return@DisposableEffect onDispose { }
        val liveListener = Media3PlayerEvents.liveEdgeSyncListener(
            isLive = isLive,
            hasSynced = { hasInitialLiveSynced },
            markSynced = { hasInitialLiveSynced = true },
            onSeekTo = { target -> engine?.seekTo(target) },
        )
        engine?.addListener(liveListener)
        onDispose {
            engine?.removeListener(liveListener)
        }
    }

    LaunchedEffect(engine) {
        if (engine == null) return@LaunchedEffect
        if (isLocalFile && effectiveUrl.isEmpty()) return@LaunchedEffect
        if (!isLocalFile && slug.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(10_000)
            val pos = engine.currentPosition
            val dur = engine.duration
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

    /** Decoder switch — engine recreate + same position, aur us file ke liye pin. */
    fun switchDecoder(mode: LocalPlaybackSettings.Mode, reason: String) {
        val resumeAt = runCatching { engine?.currentPosition ?: 0L }.getOrDefault(0L)
        runCatching { engine?.release() }
        LocalPlaybackSettings.setMode(context, effectiveUrl, mode)
        decoderMode = mode
        resumePos = resumeAt
        hasResumed = resumeAt <= 0L
        playerError = null
        autoRetryCount = 0
        isAutoRetrying = false
        android.util.Log.w("VideoPlayerScreen", "switching decoder to ${mode.key}: $reason")
    }

    DisposableEffect(engine) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("VideoPlayerScreen", "player error: ${error.errorCodeName}", error)
                // Local file me decoder fail (jaise HW force + unsupported codec) →
                // friendly error screen, jahan HW+/SW switch ke buttons hain.
                if (isLocalFile && !error.isTransient()) {
                    playerError = "decode"
                    return
                }
                if (error.isTransient() && autoRetryCount < MAX_AUTO_RETRIES) {
                    autoRetryCount++
                    isAutoRetrying = true
                    coroutineScope.launch {
                        delay(AUTO_RETRY_DELAY_MS)
                        try {
                            engine?.let { player ->
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
                    val state = engine?.playbackState ?: Player.STATE_IDLE
                    ImmersiveMode.keepScreenOn(it, isPlaying || state == Player.STATE_BUFFERING)
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                activity?.let {
                    val playing = engine?.isPlaying == true
                    ImmersiveMode.keepScreenOn(it, playing || state == Player.STATE_BUFFERING)
                }
            }
        }
        engine?.addListener(listener)
        onDispose {
            engine?.removeListener(listener)
            activity?.let { ImmersiveMode.keepScreenOn(it, false) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> engine?.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (engine?.playbackState == Player.STATE_READY) engine.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val pos = engine?.currentPosition ?: 0
            val dur = engine?.duration ?: 0
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
            engine?.release()
            activity?.resetScreenBrightness()
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
        if (engine != null && playerError == null) {
            MediaPlayerScreen(
                player = engine,
                onBackClick = onBackClick,
                onPlayInBackgroundClick = { },
                title = title,
                isLive = isLive,
                decoderMode = if (isLocalFile) decoderMode else null,
                onDecoderModeSelected = if (isLocalFile) {
                    { mode ->
                        if (mode != decoderMode) {
                            val resumeAt = runCatching { engine?.currentPosition ?: 0L }.getOrDefault(0L)
                            // Purana engine release karo (libVLC instance leak na ho).
                            runCatching { engine?.release() }
                            LocalPlaybackSettings.setMode(context, effectiveUrl, mode)
                            decoderMode = mode
                            // Naya engine isi jagah se shuru ho.
                            resumePos = resumeAt
                            hasResumed = resumeAt <= 0L
                        }
                    }
                } else null,
            )
            if (isAutoRetrying) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(72.dp),
                    color = Color.White,
                )
            }
        } else if (playerError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (isLocalFile) "Can't play this file" else "Playback error",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isLocalFile) {
                        "The player couldn't start this video. Try another decoder, or retry."
                    } else {
                        "Something went wrong while starting the stream. Please retry."
                    },
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Local file → doosra decoder try karne ke options (VLC hi rehta hai).
                if (isLocalFile && decoderMode != LocalPlaybackSettings.Mode.HW_PLUS) {
                    Button(
                        onClick = { switchDecoder(LocalPlaybackSettings.Mode.HW_PLUS, "error screen") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Switch to HW+ (hardware + software)", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (isLocalFile && decoderMode != LocalPlaybackSettings.Mode.SW) {
                    Button(
                        onClick = { switchDecoder(LocalPlaybackSettings.Mode.SW, "error screen") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Switch to SW (software)", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        playerError = null
                        autoRetryCount = 0
                        isAutoRetrying = false
                        engine?.let { player ->
                            runCatching {
                                player.setMediaItem(MediaItem.fromUri(effectiveUrl))
                                player.prepare()
                                player.playWhenReady = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Retry", color = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = onBackClick) {
                    Text("Exit", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

    }
}
