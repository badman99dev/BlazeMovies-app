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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
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
    onHistoryClick: () -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
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

            // Section 2: Content Loading / Offline Buffering / Loaded lists
            when {
                uiState.isLoading -> {
                    BufferingContentState(message = "Loading your library...")
                }
                !uiState.isOnline -> {
                    OfflineBufferingState()
                }
                else -> {
                    // History Section (History > + horizontal row of last 20)
                    HistorySection(
                        history = uiState.history,
                        onHistoryClick = onHistoryClick,
                        onContentClick = onContentClick
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // Playlists Section (Single horizontal row with Liked and Watch later)
                    PlaylistsSection(
                        likedCount = uiState.likedPlaylist.size,
                        likedLastPoster = uiState.likedPlaylist.firstOrNull()?.posterUrl,
                        watchLaterCount = uiState.watchLaterPlaylist.size,
                        watchLaterLastPoster = uiState.watchLaterPlaylist.firstOrNull()?.posterUrl,
                        onPlaylistClick = onPlaylistClick
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Utility: Persistent Downloads Action Row
                    DownloadsRow(onClick = onDownloadsClick)
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
    onHistoryClick: () -> Unit,
    onContentClick: (String, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val displayList = remember(history) { history.take(20) }

    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
        // YouTube-style Header with clickable '>'
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHistoryClick
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 17.5.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View all history",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
            )
        }

        if (displayList.isEmpty()) {
            Text(
                text = "No history yet",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 12.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(displayList, key = { it.slug + it.watchedAt }) { item ->
                    val shouldBlur = ModerationSettings.shouldBlur(context, item.contentModeration)
                    HistoryItemCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        isSeries = item.isSeries,
                        progressPercent = item.progressPercent,
                        shouldBlur = shouldBlur,
                        onClick = { onContentClick(item.slug, item.isSeries, item.imdbId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    title: String,
    posterUrl: String,
    isSeries: Boolean,
    progressPercent: Float,
    shouldBlur: Boolean,
    onClick: () -> Unit
) {
    val displayProgress = when {
        progressPercent >= 0.98f -> 1.0f
        progressPercent <= 0f -> 0f
        else -> maxOf(0.05f, progressPercent)
    }

    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Thumbnail with 16:9 aspect ratio and progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF181818))
        ) {
            BlurredContent(
                shouldBlur = shouldBlur,
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
            )

            // Bottom Red Progress bar
            if (displayProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.2f))
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

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        // Subtitle
        Text(
            text = if (isSeries) "TV Series" else "Movie",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.5.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun PlaylistsSection(
    likedCount: Int,
    likedLastPoster: String?,
    watchLaterCount: Int,
    watchLaterLastPoster: String?,
    onPlaylistClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 17.5.sp,
                color = Color.White
            )
        }

        // Single Horizontal Row with Playlists
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            // 1. Liked items Card
            item {
                PlaylistCard(
                    title = "Liked items",
                    subtitle = if (likedCount > 0) "$likedCount items • Private" else "Private",
                    badgeCount = likedCount,
                    lastPosterUrl = likedLastPoster,
                    badgeIcon = Icons.Outlined.FavoriteBorder,
                    onClick = { onPlaylistClick("liked") }
                )
            }

            // 2. Watch later Card
            item {
                PlaylistCard(
                    title = "Watch later",
                    subtitle = if (watchLaterCount > 0) "$watchLaterCount items • Private" else "Private",
                    badgeCount = watchLaterCount,
                    lastPosterUrl = watchLaterLastPoster,
                    badgeIcon = Icons.Outlined.Schedule,
                    onClick = { onPlaylistClick("watchlist") }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    title: String,
    subtitle: String,
    badgeCount: Int,
    lastPosterUrl: String?,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Playlist Thumbnail Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!lastPosterUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = lastPosterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Scrim over image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }

            // Prominent icon outline in center
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(32.dp)
            )

            // Bottom-right YouTube-style badge with item count and icon
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$badgeCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Playlist Title (below image)
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle (below title)
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
