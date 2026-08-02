package com.movie.app.best.ui.screens.downloads

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.common.C
import coil.compose.AsyncImage
import com.movie.app.best.data.settings.VideoQualitySettings
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.theme.AppBlack
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.SecondaryText
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode
import java.io.File

data class ExtractedEpisode(
    val fileName: String,
    val filePath: String,
    val size: Long
)

fun scanExtractedVideos(extractPath: String): List<ExtractedEpisode> {
    val dir = File(extractPath)
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    val videoExts = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")
    return dir.walkTopDown()
        .filter { it.isFile && it.extension.lowercase() in videoExts }
        .sortedBy { it.name }
        .map { file ->
            ExtractedEpisode(
                fileName = file.name,
                filePath = file.absolutePath,
                size = file.length()
            )
        }.toList()
}

@Composable
fun ExtractedSeriesScreen(
    extractPath: String,
    slug: String,
    posterPath: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val episodes = remember(extractPath) { scanExtractedVideos(extractPath) }

    var currentEpisode by remember { mutableStateOf<ExtractedEpisode?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            val params = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            setParameters(VideoQualitySettings.applyTo(params).build())
        }
    }

    LaunchedEffect(isFullscreen) {
        FullscreenPlayerState.isActive = isFullscreen
        activity?.let {
            if (isFullscreen) ImmersiveMode.enter(it) else ImmersiveMode.exit(it)
        }
    }
    DisposableEffect(Unit) {
        onDispose { FullscreenPlayerState.isActive = false }
    }

    LaunchedEffect(currentEpisode) {
        val ep = currentEpisode ?: return@LaunchedEffect
        exoPlayer?.release()

        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(5000, 30000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                    .setBackBuffer(300000, true).build()
            ).build()

        player.setMediaItem(MediaItem.fromUri("file://${ep.filePath}"))
        player.prepare()
        player.playWhenReady = true
        exoPlayer = player
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                activity?.let {
                    val pState = player.playbackState
                    ImmersiveMode.keepScreenOn(it, isPlaying || pState == Player.STATE_BUFFERING)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            activity?.let { ImmersiveMode.keepScreenOn(it, false) }
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
                title = currentEpisode?.fileName ?: "",
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
                    title = currentEpisode?.fileName ?: "",
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (posterPath.isNotEmpty() && File(posterPath).exists()) {
                        AsyncImage(
                            model = File(posterPath),
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(enabled = episodes.isNotEmpty()) {
                            if (currentEpisode == null && episodes.isNotEmpty()) {
                                currentEpisode = episodes.first()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (episodes.isNotEmpty()) "Tap to play" else "No episodes found",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBlack)
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Extracted Episodes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (episodes.isNotEmpty()) {
                Text(
                    text = "${episodes.size} videos",
                    color = SecondaryText,
                    fontSize = 13.sp
                )
            }
        }

        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No video files found in extracted folder",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(episodes, key = { it.filePath }) { episode ->
                    ExtractedEpisodeItem(
                        episode = episode,
                        posterPath = posterPath,
                        isPlaying = currentEpisode?.filePath == episode.filePath,
                        onPlay = { currentEpisode = episode }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ExtractedEpisodeItem(
    episode: ExtractedEpisode,
    posterPath: String,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) AppRed.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(120.dp, 68.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (posterPath.isNotEmpty() && File(posterPath).exists()) {
                AsyncImage(
                    model = File(posterPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppRed.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.fileName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFileSize(episode.size),
                color = SecondaryText,
                fontSize = 12.sp
            )
        }

        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AppRed)
            )
        }
    }
}
