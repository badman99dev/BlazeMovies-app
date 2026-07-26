package com.movie.app.best.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.components.SkeletonLibraryPage

@Composable
fun LibraryScreen(
    onContentClick: (String, Boolean) -> Unit = { _, _ -> },
    onDownloadsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        SkeletonLibraryPage()
        return
    }

    PullToRefreshLayout(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
        ) {
            com.movie.app.best.ui.components.CompactPageHeader(
                title = "Library",
                actions = {
                    com.movie.app.best.ui.components.PageHeaderIconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            if (!uiState.isOnline) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "You're offline — only Downloads available",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            if (uiState.isOnline) {
                HistorySection(
                    history = uiState.history,
                    onContentClick = onContentClick,
                    onRemove = viewModel::removeFromHistory,
                    onClearAll = viewModel::clearHistory
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                PlaylistSection(
                    title = "Liked",
                    icon = Icons.Default.Favorite,
                    items = uiState.likedPlaylist,
                    onContentClick = onContentClick,
                    onRemove = viewModel::removeFromLiked
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                PlaylistSection(
                    title = "Watch Later",
                    icon = Icons.Default.Schedule,
                    items = uiState.watchLaterPlaylist,
                    onContentClick = onContentClick,
                    onRemove = viewModel::removeFromWatchLater
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            DownloadsButton(onClick = onDownloadsClick)

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HistorySection(
    history: List<FirebaseHistoryItem>,
    onContentClick: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (history.isNotEmpty()) {
                IconButton(onClick = onClearAll, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (history.isEmpty()) {
            Text(
                text = "No history yet",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.slug + it.watchedAt }) { item ->
                    val shouldBlur = ModerationSettings.shouldBlur(context, item.contentModeration)
                    SmallMovieCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        progressPercent = item.progressPercent,
                        shouldBlur = shouldBlur,
                        onClick = { onContentClick(item.slug, item.isSeries) },
                        onRemove = { onRemove(item.slug) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Any>,
    onContentClick: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (title == "Liked") Color(0xFFE50914) else Color(0xFF4FC3F7),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${items.size}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "No movies in $title",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = items.size, key = { it }) { idx ->
                    val item = items[idx]
                    val slug = when (item) { is BookmarkItem -> item.slug; is LikeItem -> item.slug; else -> "" }
                    val itemTitle = when (item) { is BookmarkItem -> item.title; is LikeItem -> item.title; else -> "" }
                    val posterUrl = when (item) { is BookmarkItem -> item.posterUrl; is LikeItem -> item.posterUrl; else -> "" }
                    val isSeries = when (item) { is BookmarkItem -> item.isSeries; is LikeItem -> item.isSeries; else -> false }
                    val cm = when (item) { is BookmarkItem -> item.contentModeration; is LikeItem -> item.contentModeration; else -> null }
                    val shouldBlur = ModerationSettings.shouldBlur(context, cm)
                    SmallMovieCard(
                        title = itemTitle,
                        posterUrl = posterUrl,
                        progressPercent = 0f,
                        shouldBlur = shouldBlur,
                        onClick = { onContentClick(slug, isSeries) },
                        onRemove = { onRemove(slug) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallMovieCard(
    title: String,
    posterUrl: String,
    progressPercent: Float = 0f,
    shouldBlur: Boolean = false,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val displayProgress = when {
        progressPercent >= 0.98f -> 1.0f
        progressPercent <= 0f -> 0f
        else -> maxOf(0.05f, progressPercent)
    }
    Card(
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Box(modifier = Modifier.clickable { onClick() }) {
            BlurredContent(
                shouldBlur = shouldBlur,
                modifier = Modifier.fillMaxWidth().height(155.dp)
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 80f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (displayProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(displayProgress)
                        .background(Color(0xFFE50914))
                )
            }
        }
    }
}

@Composable
private fun DownloadsButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
