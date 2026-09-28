package com.movie.app.best.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.theme.AppRed

data class PlaylistItemUi(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val isSeries: Boolean,
    val contentModeration: Map<String, String>? = null
)

@Composable
fun PlaylistDetailScreen(
    playlistType: String,
    onBackClick: () -> Unit,
    onContentClick: (String, Boolean, String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val isLiked = playlistType.equals("liked", ignoreCase = true)
    val pageTitle = if (isLiked) "Liked items" else "Watch later"

    val itemsList: List<PlaylistItemUi> = remember(uiState.likedPlaylist, uiState.watchLaterPlaylist, isLiked) {
        if (isLiked) {
            uiState.likedPlaylist.map {
                PlaylistItemUi(
                    slug = it.slug,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    isSeries = it.isSeries,
                    contentModeration = it.contentModeration
                )
            }
        } else {
            uiState.watchLaterPlaylist.map {
                PlaylistItemUi(
                    slug = it.slug,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    isSeries = it.isSeries,
                    contentModeration = it.contentModeration
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pageTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${itemsList.size} videos • Private",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (uiState.isLoading && itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isLiked) Icons.Outlined.FavoriteBorder else Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No videos in $pageTitle",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(itemsList, key = { it.slug }) { item ->
                    val shouldBlur = ModerationSettings.shouldBlur(context, item.contentModeration)
                    PlaylistItemRow(
                        item = item,
                        shouldBlur = shouldBlur,
                        onClick = { onContentClick(item.slug, item.isSeries, "") },
                        onRemove = {
                            if (isLiked) {
                                viewModel.removeFromLiked(item.slug)
                            } else {
                                viewModel.removeFromWatchLater(item.slug)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistItemRow(
    item: PlaylistItemUi,
    shouldBlur: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Poster thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181818))
        ) {
            BlurredContent(
                shouldBlur = shouldBlur,
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center: Title + Metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (item.isSeries) "TV Series" else "Movie",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Trash Icon Button (User specified: "screenshot me 3 dot hai lekin ham trash wale icon rakhenge")
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Remove",
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
