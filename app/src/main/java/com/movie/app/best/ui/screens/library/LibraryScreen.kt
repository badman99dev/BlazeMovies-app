package com.movie.app.best.ui.screens.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.R
import com.movie.app.best.data.model.AppUser
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.components.CompactPageHeader
import com.movie.app.best.ui.components.PageHeaderIconButton
import com.movie.app.best.ui.theme.AppRed

@Composable
fun LibraryScreen(
    onContentClick: (String, Boolean, String) -> Unit = { _, _, _ -> },
    onDownloadsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            // Top App Bar
            CompactPageHeader(
                title = "Library",
                actions = {
                    PageHeaderIconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    PageHeaderIconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )

            // Section 1: Profile Card (Online or Offline cached)
            if (uiState.isLoggedIn && uiState.user != null) {
                LoggedInProfileCard(
                    user = uiState.user!!,
                    userTier = uiState.userTier,
                    isLoggingOut = uiState.isLoggingOut,
                    onLogoutClick = { viewModel.logout() }
                )
            } else {
                LoggedOutProfileCard(onLoginClick = onLoginClick)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 2: Persistent Downloads Action Row
            DownloadsRow(onClick = onDownloadsClick)

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Content Loading / Offline Buffering / Loaded lists
            when {
                uiState.isLoading -> {
                    BufferingContentState(message = "Loading your library...")
                }
                !uiState.isOnline -> {
                    OfflineBufferingState()
                }
                else -> {
                    // Online and ready
                    HistorySection(
                        history = uiState.history,
                        onContentClick = onContentClick,
                        onRemove = viewModel::removeFromHistory,
                        onClearAll = viewModel::clearHistory
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    PlaylistSection(
                        title = "Liked Videos",
                        icon = Icons.Default.Favorite,
                        items = uiState.likedPlaylist,
                        onContentClick = onContentClick,
                        onRemove = viewModel::removeFromLiked
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    PlaylistSection(
                        title = "Watch Later",
                        icon = Icons.Default.Schedule,
                        items = uiState.watchLaterPlaylist,
                        onContentClick = onContentClick,
                        onRemove = viewModel::removeFromWatchLater
                    )
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun LoggedInProfileCard(
    user: AppUser,
    userTier: String,
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit
) {
    val displayName = user.firstName?.let {
        "$it ${user.lastName ?: ""}".trim()
    }?.ifEmpty { null } ?: user.username.ifEmpty { "User" }

    val initialLetter = (displayName.firstOrNull() ?: 'U').uppercaseChar().toString()

    val (tierLabel, tierTextColor, tierBgColor, tierBorderColor) = when (userTier.lowercase()) {
        "vip" -> Quadruple("VIP MEMBER", Color(0xFFFF5252), Color(0xFFE50914).copy(alpha = 0.15f), Color(0xFFE50914).copy(alpha = 0.45f))
        "moderator" -> Quadruple("MODERATOR", Color(0xFF4FC3F7), Color(0xFF0288D1).copy(alpha = 0.15f), Color(0xFF0288D1).copy(alpha = 0.45f))
        else -> Quadruple("STANDARD USER", Color(0xFFCCCCCC), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.12f))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with verified badge
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!user.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFF2E2E2E), CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE50914), Color(0xFF6B070D))
                                )
                            )
                            .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialLetter,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (user.isVerified == 1) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_verified_badge),
                        contentDescription = "Verified",
                        modifier = Modifier
                            .size(17.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info & Tier Pill
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (user.email.isNotEmpty()) {
                    Text(
                        text = user.email,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(tierBgColor)
                        .border(1.dp, tierBorderColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tierLabel,
                        color = tierTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sleek Logout Button on Right
            IconButton(
                onClick = onLogoutClick,
                enabled = !isLoggingOut,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF4D4D),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoggedOutProfileCard(onLoginClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sign in to BlazeMovies",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sync watchlist, history and likes",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Sign In",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DownloadsRow(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F1F1F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Downloads",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Offline storage available",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun BufferingContentState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AppRed,
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OfflineBufferingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = AppRed,
            strokeWidth = 3.dp,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Connecting to Blaze server...",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Pull down to refresh when connected",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 10.5.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(54.dp))

        // Bottom No Connection badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF242424), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4D4D))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No connection",
                    color = Color(0xFFCCCCCC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<FirebaseHistoryItem>,
    onContentClick: (String, Boolean, String) -> Unit,
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
                        onClick = { onContentClick(item.slug, item.isSeries, item.imdbId) },
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
    onContentClick: (String, Boolean, String) -> Unit,
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
                tint = if (title.contains("Liked", ignoreCase = true)) Color(0xFFE50914) else Color(0xFF4FC3F7),
                modifier = Modifier.size(19.dp)
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
                        onClick = { onContentClick(slug, isSeries, "") },
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
    ) {
        Box(modifier = Modifier.clickable { onClick() }) {
            BlurredContent(
                shouldBlur = shouldBlur,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 75f
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

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
