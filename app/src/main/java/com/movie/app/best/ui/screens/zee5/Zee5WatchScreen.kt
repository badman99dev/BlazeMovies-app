package com.movie.app.best.ui.screens.zee5

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.debug.Zee5False404Interceptor
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.settings.VideoQualitySettings
import com.movie.app.best.ui.components.GlassBadge
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.theme.AppBlack
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.SecondaryText
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode
import okhttp3.OkHttpClient

@Composable
fun Zee5WatchScreen(
    onBackClick: () -> Unit,
    onRelatedItemClick: (String) -> Unit = {},
    viewModel: Zee5WatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullscreen by remember { mutableStateOf(false) }
    val episodeListState = rememberLazyListState()
    var playerError by remember { mutableStateOf<String?>(null) }
    var zee5PlayerErrorRetryCount by remember { mutableStateOf(0) }

    LaunchedEffect(isFullscreen) {
        FullscreenPlayerState.isActive = isFullscreen
        activity?.let {
            if (isFullscreen) ImmersiveMode.enter(it) else ImmersiveMode.exit(it)
        }
    }
    DisposableEffect(Unit) {
        onDispose { FullscreenPlayerState.isActive = false }
    }

    val shouldLoadMoreEpisodes = remember {
        derivedStateOf {
            val layoutInfo = episodeListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMoreEpisodes.value) {
        if (shouldLoadMoreEpisodes.value) {
            viewModel.loadMoreEpisodes()
        }
    }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            val params = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            setParameters(VideoQualitySettings.applyTo(params).build())
        }
    }

    val exoPlayer = remember {
        val okClient = OkHttpClient.Builder()
            .followRedirects(true).followSslRedirects(true)
            .addInterceptor(Zee5False404Interceptor())
            .addInterceptor(DebugInterceptor()).build()

        val okFactory = OkHttpDataSource.Factory(okClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://zee5-no-ads.onrender.com/",
                "Accept" to "*/*"
            ))

        val dsFactory = DefaultDataSource.Factory(context, okFactory)
        val hlsFactory = HlsMediaSource.Factory(dsFactory)

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(hlsFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(5000, 30000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                    .setBackBuffer(300000, true).build()
            ).build()
    }

    LaunchedEffect(state.currentM3u8) {
        val m3u8 = state.currentM3u8
        playerError = null
        zee5PlayerErrorRetryCount = 0
        if (m3u8 != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(m3u8))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (zee5PlayerErrorRetryCount < 2) {
                    zee5PlayerErrorRetryCount++
                    val url = state.currentM3u8
                    if (url != null) {
                        exoPlayer.setMediaItem(MediaItem.fromUri(url))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                    return
                }
                playerError = error.message ?: "Playback error"
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) playerError = null
                activity?.let {
                    val pState = exoPlayer.playbackState
                    ImmersiveMode.keepScreenOn(it, isPlaying || pState == Player.STATE_BUFFERING)
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                activity?.let {
                    val playing = exoPlayer.isPlaying
                    ImmersiveMode.keepScreenOn(it, playing || playbackState == Player.STATE_BUFFERING)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.let { ImmersiveMode.keepScreenOn(it, false) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> { if (exoPlayer.playbackState == Player.STATE_READY) exoPlayer.play() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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

    BackHandler {
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
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    if (isFullscreen) {
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
                title = state.currentEpisode?.title ?: state.detail?.title ?: ""
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .zIndex(1f)
        ) {
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
                title = state.detail?.title ?: state.currentEpisode?.title ?: ""
            )

            if (state.isFetchingStream || state.error != null || playerError != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbUrl = state.currentEpisode?.effectiveLandscapeUrl
                        ?: state.currentEpisode?.effectiveImageUrl
                        ?: state.detail?.effectiveLandscapeUrl
                        ?: state.detail?.effectiveImageUrl
                    thumbUrl?.let { url ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(url)
                                .crossfade(true)
                                .build(),
                            contentDescription = state.currentEpisode?.title ?: state.detail?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )

                    if (state.isFetchingStream) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppRed, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: playerError ?: "Playback error",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                playerError = null
                                zee5PlayerErrorRetryCount = 0
                                state.currentEpisode?.let { viewModel.onEpisodeClick(it) }
                            }) {
                                Text("Retry", color = AppRed)
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBlack)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val title = state.currentEpisode?.title ?: state.detail?.title
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.detail?.releaseDate?.let {
                        Text(
                            text = it.take(4),
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    state.detail?.ageRating?.let {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(0.5.dp, SecondaryText, RoundedCornerShape(4.dp))
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
                    state.detail?.languages?.firstOrNull()?.let { lang ->
                        Text(
                            text = lang,
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    state.currentEpisode?.episodeNumber?.let {
                        Text(
                            text = "Episode $it",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    GlassBadge(text = "HD", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val description = state.currentEpisode?.description ?: state.detail?.description
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
        }

        if (state.seasons.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBlack)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.seasons.forEach { season ->
                    val isSelected = season.id == state.selectedSeasonId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AppRed else CardDark)
                            .clickable { season.id?.let { viewModel.selectSeason(it) } }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = season.title ?: "Season ${season.seasonNumber}",
                            color = if (isSelected) Color.White else SecondaryText,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        when {
            state.detail?.isTvShow == true -> {
                Text(
                    text = "Episodes",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (state.episodes.isEmpty() && !state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No episodes available", color = SecondaryText, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        state = episodeListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(state.episodes, key = { it.id ?: it.episodeNumber?.toString() ?: it.title ?: "" }) { episode ->
                            Zee5WatchEpisodeCard(
                                episode = episode,
                                isPlaying = state.currentEpisode?.id == episode.id,
                                onPlayClick = { viewModel.onEpisodeClick(episode) }
                            )
                        }

                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AppRed, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        if (!state.hasMoreEpisodes && state.episodes.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GlassBadge(text = "Reached to bottom")
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
            state.detail?.isMovie == true -> {
                val related = state.detail?.related
                if (!related.isNullOrEmpty()) {
                    Zee5RelatedSection(
                        related = related,
                        onItemClick = { item ->
                            item.id?.let { onRelatedItemClick(it) }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recommendations available", color = SecondaryText, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Zee5WatchEpisodeCard(
    episode: Zee5Item,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "watch_ep_press"
    )

    val titleColor = if (isPlaying) AppRed else Color.White

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onPlayClick
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardDark)
            ) {
                val thumbUrl = episode.effectiveLandscapeUrl ?: episode.effectiveImageUrl
                if (thumbUrl != null) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E${episode.episodeNumber ?: "?"}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("NOW PLAYING", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }

                episode.duration?.let { duration ->
                    if (duration > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${duration / 60} min",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                val titleText = episode.episodeNumber?.let { "$it. ${episode.title ?: "Untitled"}" } ?: (episode.title ?: "Untitled")
                Text(
                    text = titleText,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val meta = buildList {
                    episode.episodeNumber?.let { add("Episode $it") }
                    episode.duration?.let { if (it > 0) add("${it / 60} min") }
                    episode.releaseDate?.let { try { add(java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it) ?: return@let)) } catch (_: Exception) {} }
                }.joinToString(" · ")

                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(meta, color = SecondaryText, fontSize = 12.sp)
                }

                if (!episode.description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = episode.description,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Download",
                tint = SecondaryText,
                modifier = Modifier.size(22.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color.White.copy(alpha = 0.06f),
            thickness = 0.5.dp
        )
    }
}


