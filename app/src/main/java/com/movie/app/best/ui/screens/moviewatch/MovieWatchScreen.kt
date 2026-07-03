package com.movie.app.best.ui.screens.moviewatch

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
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
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import coil.compose.AsyncImage
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.debug.Zee5False404Interceptor
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.settings.VideoQualitySettings
import com.movie.app.best.ui.components.GlassBadge
import com.movie.app.best.ui.screens.moviedetail.components.FindingSimilarSection
import com.movie.app.best.ui.screens.moviedetail.components.MoreLikeThisSection
import com.movie.app.best.ui.screens.moviedetail.components.StreamRequestResultModal
import com.movie.app.best.ui.screens.moviedetail.components.StreamRequestWaitingPopup
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.theme.WasmerBlack
import com.movie.app.best.ui.theme.WasmerRed
import com.movie.app.best.ui.theme.WasmerSubText
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieWatchScreen(
    onBackClick: () -> Unit,
    onMovieClick: (String) -> Unit = {},
    viewModel: MovieWatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val firebaseRepository = remember { FirebaseRepository(context) }
    val slug = viewModel.contentSlug
    val title = viewModel.title

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            val params = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            setParameters(VideoQualitySettings.applyTo(params).build())
        }
    }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var langSwitchSeek by remember { mutableStateOf(0L) }

    LaunchedEffect(isFullscreen) {
        FullscreenPlayerState.isActive = isFullscreen
        activity?.let {
            if (isFullscreen) ImmersiveMode.enter(it) else ImmersiveMode.exit(it)
        }
    }
    DisposableEffect(Unit) {
        onDispose { FullscreenPlayerState.isActive = false }
    }

    // Resume position
    var resumePos by remember { mutableStateOf(0L) }
    var hasResumed by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentM3u8) {
        val m3u8 = state.currentM3u8
        hasResumed = false
        resumePos = 0L
        if (m3u8 == null) {
            exoPlayer?.release()
            exoPlayer = null
            return@LaunchedEffect
        }

        exoPlayer?.release()

        // On language switch: resume from captured position; else slug-based saved progress
        if (langSwitchSeek > 0) {
            resumePos = langSwitchSeek
            langSwitchSeek = 0L
        } else if (slug.isNotEmpty()) {
            val saved = firebaseRepository.getLocalProgress(slug)
            resumePos = saved?.progressMs ?: 0L
        }

        val referer = when (state.activeSource) {
            "gemma" -> "https://gemma416okl.com/"
            else -> "https://wasmer-jhns970ko-badals-projects-03fab3df.vercel.app/"
        }

        val okClient = OkHttpClient.Builder()
            .followRedirects(true).followSslRedirects(true)
            .addInterceptor(Zee5False404Interceptor())
            .addInterceptor(DebugInterceptor()).build()

        val okFactory = OkHttpDataSource.Factory(okClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to referer,
                "Accept" to "*/*"
            ))

        val dsFactory = DefaultDataSource.Factory(context, okFactory)
        val hlsFactory = HlsMediaSource.Factory(dsFactory)

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(hlsFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(5000, 30000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                    .setBackBuffer(300000, true).build()
            ).build()

        player.setMediaItem(MediaItem.fromUri(m3u8))
        player.prepare()
        player.playWhenReady = true
        exoPlayer = player
    }

    // Resume seek once ready
    LaunchedEffect(resumePos) {
        if (resumePos > 0 && !hasResumed && exoPlayer != null) {
            if (exoPlayer?.playbackState == Player.STATE_READY) {
                hasResumed = true
                val seekTo = resumePos
                resumePos = 0L
                exoPlayer?.seekTo(seekTo)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && resumePos > 0 && !hasResumed) {
                    hasResumed = true
                    val seekTo = resumePos
                    resumePos = 0L
                    exoPlayer?.seekTo(seekTo)
                }
                activity?.let {
                    val playing = player.isPlaying
                    ImmersiveMode.keepScreenOn(it, playing || playbackState == Player.STATE_BUFFERING)
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                activity?.let {
                    val pState = player.playbackState
                    ImmersiveMode.keepScreenOn(it, isPlaying || pState == Player.STATE_BUFFERING)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                // Playback failed -> trigger Gemma fallback (or next candidate)
                viewModel.onPlaybackError()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            activity?.let { ImmersiveMode.keepScreenOn(it, false) }
        }
    }

    // Progress save loop
    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        if (slug.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(10_000)
            val pos = exoPlayer?.currentPosition ?: 0
            val dur = exoPlayer?.duration ?: 0
            if (pos > 0 && dur > 0) {
                firebaseRepository.updateProgressLocal(slug, pos, dur)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> { if (exoPlayer?.playbackState == Player.STATE_READY) exoPlayer?.play() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val pos = exoPlayer?.currentPosition ?: 0
            val dur = exoPlayer?.duration ?: 0
            if (pos > 0 && dur > 0 && slug.isNotEmpty()) {
                firebaseRepository.updateProgressLocal(slug, pos, dur)
                val repo = firebaseRepository
                val s = slug
                Thread {
                    try { runBlocking { repo.updateProgress(s, pos, dur) } } catch (_: Exception) {}
                }.start()
            }
            exoPlayer?.release()
            exoPlayer = null
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val exitFullscreen = {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.let { ImmersiveMode.exit(it) }
    }

    val enterFullscreen = {
        isFullscreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.let { ImmersiveMode.enter(it) }
    }

    val onLangSelect: (String) -> Unit = { lang ->
        exoPlayer?.let { p ->
            if (p.duration > 0) langSwitchSeek = p.currentPosition.coerceIn(0L, p.duration)
        }
        viewModel.selectLanguage(lang)
    }

    val customAudioTracks = state.availableLanguages
        .takeIf { it.size > 1 && state.activeSource == "gemma" }

    BackHandler {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onBackClick()
        }
    }

    if (isFullscreen && exoPlayer != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MediaPlayerScreen(
                player = exoPlayer,
                modifier = Modifier.fillMaxSize(),
                onBackClick = { exitFullscreen() },
                onPlayInBackgroundClick = {},
                onFullscreenClick = { exitFullscreen() },
                isInline = false,
                title = title,
                customAudioTracks = customAudioTracks,
                selectedAudioTrack = state.selectedLanguage,
                onAudioTrackSelected = onLangSelect,
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WasmerBlack)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .zIndex(1f)
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
                        title = title,
                        customAudioTracks = customAudioTracks,
                        selectedAudioTrack = state.selectedLanguage,
                        onAudioTrackSelected = onLangSelect,
                    )
                } else if (state.error != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val thumbUrl = viewModel.posterUrl.takeIf { it.isNotEmpty() }
                        if (thumbUrl != null) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartDisplay,
                                contentDescription = "Source not found",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Source not found",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stream isn't available yet",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            val capsuleBg = if (state.streamRequested) {
                                Brush.linearGradient(colors = listOf(Color(0xFF1B5E20).copy(alpha = 0.5f), Color(0xFF2E7D32).copy(alpha = 0.3f)))
                            } else {
                                Brush.linearGradient(colors = listOf(Color(0xFFE50914), Color(0xFFB71C1C)))
                            }
                            val borderBrush = if (state.streamRequested) {
                                Brush.linearGradient(colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.6f), Color(0xFF81C784).copy(alpha = 0.3f)))
                            } else {
                                Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252)))
                            }
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(capsuleBg)
                                    .border(
                                        width = if (!state.streamRequested) 1.dp else 0.5.dp,
                                        brush = borderBrush,
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .clickable(enabled = !state.streamRequested) { viewModel.requestStream() }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.streamRequested) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF81C784)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.SmartDisplay,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.streamRequested) "Requested ✓" else "Request Stream",
                                        color = if (state.streamRequested) Color(0xFFA5D6A7) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val thumbUrl = viewModel.posterUrl.takeIf { it.isNotEmpty() }
                        if (thumbUrl != null) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        }
                        CircularProgressIndicator(color = WasmerRed, modifier = Modifier.size(36.dp))
                    }
                }
            }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WasmerBlack)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val headerTitle = state.titleDetails?.primaryTitle?.takeIf { it.isNotBlank() } ?: title
            Text(
                text = headerTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val year = state.titleDetails?.startYear?.takeIf { it > 0 }?.toString()
                year?.let {
                    Text(
                        text = it,
                        color = WasmerSubText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                state.ageRating.takeIf { it.isNotBlank() }?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, WasmerSubText, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                GlassBadge(text = "HD", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val description = state.titleDetails?.plot?.takeIf { it.isNotBlank() }
            description?.let { desc ->
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
            }

            val stars = state.titleDetails?.stars ?: emptyList()
            val backendCast = state.backendCast
            val castList = if (stars.isNotEmpty()) {
                stars
            } else if (backendCast.isNotBlank()) {
                backendCast.split(",").mapNotNull { name ->
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) com.movie.app.best.data.model.ImdbName(displayName = trimmed) else null
                }
            } else {
                emptyList()
            }
            if (castList.isNotEmpty()) {
                CollapsibleCastRow(castList)
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.07f))
        )

        // More Like This (replaces episodes/seasons)
        if (state.isSimilarLoading) {
            FindingSimilarSection()
        } else if (state.similarMovies.isNotEmpty()) {
            MoreLikeThisSection(
                movies = state.similarMovies,
                onMovieClick = { movieSlug, isSeries ->
                    if (!isSeries) onMovieClick(movieSlug)
                }
            )
            Spacer(modifier = Modifier.height(80.dp))
        } else {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Full-screen buffering overlay: shown until IMDb responds (max 4s)
    if (state.showBuffering) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = WasmerRed, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Loading...",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Stream request overlays
    if (state.isStreamRequesting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            StreamRequestWaitingPopup()
        }
    }

    if (state.showStreamRequestResult && state.streamRequestResult != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            StreamRequestResultModal(
                result = state.streamRequestResult!!,
                onDismiss = { viewModel.dismissStreamRequestResult() }
            )
        }
    }
    }
}

@Composable
private fun CollapsibleCastRow(stars: List<com.movie.app.best.data.model.ImdbName>) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300),
        label = "arrow_rot"
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(22.dp)
                .rotate(arrowRotation)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Cast",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "  (${stars.size})",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(300)),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200))
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(stars) { star ->
                Column(
                    modifier = Modifier.width(72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (star.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = star.photoUrl,
                                contentDescription = star.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = star.displayName,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
